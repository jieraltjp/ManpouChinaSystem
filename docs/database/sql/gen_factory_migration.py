#!/usr/bin/env python3
"""
Generate factory migration SQL from companies.sql data.
Reads INSERT statements and generates ALTER TABLE + INSERT INTO factory.
"""
import re
import sys
from datetime import datetime

def clean_field(val: str) -> str:
    val = val.strip()
    if val.lower() == "null":
        return ""
    if val.startswith("'") and val.endswith("'"):
        val = val[1:-1]
    val = val.replace("''", "'")
    return val

COMPANIES_SQL = "companies.sql"
OUTPUT_SQL = "V4__factory_migration.sql"

# Keyword -> category mapping (order matters: check in order)
CATEGORY_KEYWORDS = [
    (["五金", "工具", "工具有限", "金工", "机电", "机械"], "TOOLS"),
    (["纺织", "服饰", "服装", "箱包", "帽业", "布业", "织造", "针纺"], "TEXTILE"),
    (["塑料", "塑胶", "橡塑", "塑胶"], "PLASTIC"),
    (["电子", "电器", "光电", "电气", "LED", "科技", "电器厂"], "ELECTRONICS"),
    (["家具", "家居", "木业", "竹木", "家居厂"], "FURNITURE"),
    (["汽车", "汽配", "车业", "汽保", "汽配厂"], "AUTO_PARTS"),
    (["户外", "体育", "运动", "健身", "玩具", "户外厂", "运动厂"], "SPORTS"),
    (["宠物", "猫狗", "笼具", "宠物厂"], "PET"),
    (["医疗", "器械", "护理", "康养", "医疗厂"], "MEDICAL"),
    (["工艺", "礼品", "编织", "工艺厂"], "CRAFTS"),
    (["化工", "涂料", "颜料", "涂层"], "CHEMICAL"),
]

# Entries that are test data, locations, or malformed — skip migration
SKIP_IDS = {9, 21, 22, 30, 42}

def infer_category(name: str) -> str:
    for keywords, cat in CATEGORY_KEYWORDS:
        for kw in keywords:
            if kw in name:
                return cat
    return "OTHER"

def unescape(s: str) -> str:
    """Strip single quotes from SQL string literals."""
    if s.startswith("'") and s.endswith("'"):
        return s[1:-1].replace("''", "'")
    return s

def parse_tuple(s: str) -> list:
    """Parse a VALUES (...) tuple into individual field strings."""
    s = s.strip()
    if s.startswith("(") and s.endswith(")"):
        s = s[1:-1]
    fields = []
    current = ""
    in_str = False
    escape_next = False
    for ch in s:
        if escape_next:
            current += ch
            escape_next = False
            continue
        if ch == '\\':
            escape_next = True
            continue
        if ch == "'" and not escape_next:
            in_str = not in_str
            current += ch
        elif ch == ',' and not in_str:
            fields.append(current)
            current = ""
        else:
            current += ch
    if current:
        fields.append(current)
    return fields

def parse_insert_values(sql_text: str):
    """Extract fields from INSERT statements, line by line."""
    for line in sql_text.split("\n"):
        ul = line.upper()
        if "INSERT INTO" not in ul or "`COMPANIES`" not in ul or "VALUES" not in ul:
            continue
        # Extract the tuple part between VALUES ( and );
        try:
            start = line.index("VALUES") + len("VALUES")
            end = line.rindex(";")
            inner = line[start:end].strip()
        except ValueError:
            continue

        fields = parse_tuple(inner)
        if len(fields) < 12:
            continue

        try:
            record_id = int(clean_field(fields[0]))
        except ValueError:
            record_id = 0

        lon_raw = clean_field(fields[6])
        lat_raw = clean_field(fields[7])
        is_del = clean_field(fields[10])
        yield {
            "id": record_id,
            "name": clean_field(fields[1]),
            "province": clean_field(fields[2]),
            "city": clean_field(fields[3]),
            "district": clean_field(fields[4]),
            "address": clean_field(fields[5]),
            "longitude": lon_raw,
            "latitude": lat_raw,
            "created_at": clean_field(fields[8]),
            "is_deleted": is_del == "1",
        }

def make_factory_code(created_at_str: str, seq: int) -> str:
    try:
        dt = datetime.strptime(created_at_str, "%Y-%m-%d %H:%M:%S")
        date_str = dt.strftime("%Y%m%d")
    except Exception:
        date_str = "20260402"
    return f"F-{date_str}-{seq:03d}"

def sql_escape(s: str) -> str:
    if s is None:
        return "NULL"
    s = str(s).replace("\\", "\\\\").replace("'", "''").replace("\r", "").replace("\n", " ")
    return f"'{s}'"

def generate_alters():
    return """
-- ============================================================
-- V4__factory_migration.sql
-- 从 companies 迁移到 factory
-- 对应: DB-10-factory.md §3 数据迁移策略
-- ============================================================

-- Step 1: 扩展 factory 表（逐列添加，避免 MySQL 批量 ADD COLUMN 限制）
-- 如果 factory 表已由 Hibernate ddl-auto=update 创建，需要补充以下列
ALTER TABLE factory
    ADD COLUMN category           VARCHAR(32)  NOT NULL  DEFAULT 'OTHER'  COMMENT '分类: TOOLS/TEXTILE/PLASTIC/ELECTRONICS/FURNITURE/AUTO_PARTS/SPORTS/PET/MEDICAL/CRAFTS/CHEMICAL/OTHER';

ALTER TABLE factory
    ADD COLUMN province           VARCHAR(64)  NOT NULL  DEFAULT ''  COMMENT '省';

ALTER TABLE factory
    ADD COLUMN city              VARCHAR(64)  NOT NULL  DEFAULT ''  COMMENT '市';

ALTER TABLE factory
    ADD COLUMN county            VARCHAR(64)  NOT NULL  DEFAULT ''  COMMENT '县/区';

ALTER TABLE factory
    ADD COLUMN longitude         DECIMAL(11,8)  DEFAULT NULL  COMMENT '经度';

ALTER TABLE factory
    ADD COLUMN latitude          DECIMAL(11,8)  DEFAULT NULL  COMMENT '纬度';

ALTER TABLE factory
    ADD COLUMN contact_wechat   VARCHAR(64)  DEFAULT NULL  COMMENT '微信号';

ALTER TABLE factory
    ADD COLUMN contact_qq       VARCHAR(32)  DEFAULT NULL  COMMENT 'QQ号';

ALTER TABLE factory
    ADD COLUMN cooperation_status VARCHAR(32)  NOT NULL  DEFAULT 'POTENTIAL'  COMMENT '合作状态: ACTIVE/SUSPENDED/ELIMINATED/POTENTIAL';

ALTER TABLE factory
    ADD COLUMN payment_terms     VARCHAR(64)  NOT NULL  DEFAULT 'NET_30'  COMMENT '账期: CASH/NET_30/NET_60/NET_90/CREDIT';

ALTER TABLE factory
    ADD COLUMN notes            VARCHAR(500)  DEFAULT NULL  COMMENT '备注';

-- Step 2: 添加缺失索引
ALTER TABLE factory
    ADD INDEX idx_factory_category (category);

ALTER TABLE factory
    ADD INDEX idx_factory_cooperation_status (cooperation_status);

ALTER TABLE factory
    ADD INDEX idx_factory_province (province);

ALTER TABLE factory
    ADD INDEX idx_factory_city (city);

-- Step 3: 删除旧列（确认新列数据正确后执行）
-- ALTER TABLE factory DROP COLUMN location;
"""

def main():
    with open(COMPANIES_SQL, "r", encoding="utf-8") as f:
        sql_text = f.read()

    records = list(parse_insert_values(sql_text))
    print(f"Parsed {len(records)} company records", file=sys.stderr)

    # Group by create_date to assign sequential numbers
    by_date = {}
    for r in records:
        try:
            dt = datetime.strptime(r["created_at"], "%Y-%m-%d %H:%M:%S")
            date_str = dt.strftime("%Y%m%d")
        except Exception:
            date_str = "20260402"
        by_date.setdefault(date_str, []).append(r)

    # Assign sequential numbers per date
    for date_str, recs in by_date.items():
        for i, r in enumerate(recs, 1):
            r["factory_code"] = f"F-{date_str}-{i:03d}"

    out = [generate_alters()]
    out.append("-- Step 4: 从 companies 迁移数据到 factory")
    out.append("-- 注意: is_deleted=1 或属于测试/无效数据的记录不迁移")
    out.append("")
    out.append("INSERT INTO factory (")
    out.append("    id, factory_code, factory_name, category,")
    out.append("    province, city, county, rough_location,")
    out.append("    longitude, latitude,")
    out.append("    cooperation_status, payment_terms,")
    out.append("    create_time, create_by, update_by, is_deleted")
    out.append(") VALUES")

    lines = []
    for i, r in enumerate(records):
        if r["is_deleted"]:
            continue
        if r["id"] in SKIP_IDS:
            print(f"  Skipping test/junk entry id={r['id']} name={r['name']}", file=sys.stderr)
            continue
        cat = infer_category(r["name"])

        # Handle longitude/latitude: 0.00000000 -> NULL
        lon = "NULL" if r["longitude"] == "0.00000000" else r["longitude"]
        lat = "NULL" if r["latitude"] == "0.00000000" else r["latitude"]

        # Handle empty address
        addr = r["address"] if r["address"] else ""

        created = r["created_at"] if r["created_at"] else "2026-04-02 00:00:00"

        line = (
            f"    ({r['id']}, {sql_escape(r['factory_code'])}, {sql_escape(r['name'])}, {sql_escape(cat)},"
            f" {sql_escape(r['province'])}, {sql_escape(r['city'])}, {sql_escape(r['district'])}, {sql_escape(addr)},"
            f" {lon}, {lat},"
            f" 'POTENTIAL', 'NET_30',"
            f" {sql_escape(created)}, 'system', '', 0)"
        )
        lines.append(line)

    out.append(",\n".join(lines) + ";")
    out.append("")
    out.append(f"-- 迁移完成: {len(lines)} 条记录写入 factory 表")

    result = "\n".join(out)
    with open(OUTPUT_SQL, "w", encoding="utf-8") as f:
        f.write(result)

    # Summary
    cat_counts = {}
    for r in records:
        if not r["is_deleted"] and r["id"] not in SKIP_IDS:
            cat = infer_category(r["name"])
            cat_counts[cat] = cat_counts.get(cat, 0) + 1
    print("Category distribution:", file=sys.stderr)
    for cat, cnt in sorted(cat_counts.items(), key=lambda x: -x[1]):
        print(f"  {cat}: {cnt}", file=sys.stderr)
    print(f"\nGenerated: {OUTPUT_SQL}")
    print(f"Total records migrated: {len(lines)}")

if __name__ == "__main__":
    main()
