#!/usr/bin/env python3
"""
etl_product.py — 商品数据 ETL
数据源:
  A) goods.sql   — 781 条，完整字段（含 HS code, 税率, 工厂名）
  B) DB.xlsx     — 1877 条，仅含英文名、日文名、价格、重量

目标表: product, product_factory

用法:
  python etl_product.py                  # 干跑（输出到 stdout）
  python etl_product.py --output=V5__product_seed_data.sql
  python etl_product.py --dry-run        # 仅统计，不输出 SQL
"""

import re, sys, argparse
from pathlib import Path

sys.stdout.reconfigure(encoding='utf-8')

BASE_DIR = Path(__file__).parent
GOODS_SQL = BASE_DIR / "goods.sql"
DB_XLSX   = BASE_DIR / "DB.xlsx"

# ── 1. 解析 goods.sql ───────────────────────────────────────────────────────

def parse_goods_sql(path: Path) -> list[dict]:
    """解析 goods.sql INSERT 语句，返回记录列表"""
    content = path.read_text(encoding="utf-8")
    inserts = re.findall(r"INSERT INTO.*?;\s*$", content, re.MULTILINE)

    FIELD_NAMES = [
        "id", "sku", "hs_code", "name_en", "name_zh", "unit_price",
        "tax_rate", "unit", "weight_gross", "weight_net", "declaration_elements",
        "box_qty", "box_desc", "origin", "factory_name", "buyer", "remark", "last_used",
    ]

    records = []
    for ins in inserts:
        m = re.search(r"VALUES\s*\((.*)\);", ins, re.DOTALL)
        if not m:
            continue
        vals = m.group(1)
        parts = _split_csv_row(vals)
        if len(parts) < len(FIELD_NAMES):
            continue
        rec = dict(zip(FIELD_NAMES, parts[: len(FIELD_NAMES)]))
        records.append(rec)
    return records


def _split_csv_row(val_str: str) -> list[str]:
    """解析 VALUES (...) 中的字段，处理单引号包围的字符串"""
    parts, current, in_quote = [], "", False
    for ch in val_str:
        if ch == "'" and (not current or current[-1] != "\\"):
            in_quote = not in_quote
            current += ch
        elif ch == "," and not in_quote:
            parts.append(current.strip().strip("'"))
            current = ""
        else:
            current += ch
    parts.append(current.strip().strip("'"))
    return parts


# ── 2. 解析 DB.xlsx ──────────────────────────────────────────────────────────

def parse_db_xlsx(path: Path) -> list[dict]:
    """解析 DB.xlsx，返回 {sku -> {name_en, name_zh, unit_price, ...}}"""
    import openpyxl, zipfile, re

    # 直接读 sharedStrings（UTF-8）
    with zipfile.ZipFile(path, "r") as z:
        ss_xml = z.read("xl/sharedStrings.xml").decode("utf-8")
        si = re.findall(r"<si><t[^>]*>(.*?)</t></si>", ss_xml)

        ws_xml = z.read("xl/worksheets/sheet1.xml").decode("utf-8")

    # 解析单元格: <c r="C2" t="s"><v>13</v></c> 或 <c r="D2"><v>592</v></c>
    cells = re.findall(r'<c r="([A-Z]+)(\d+)"[^>]*><v>([^<]+)</v></c>', ws_xml)
    # 解析 inlineStr: <c r="C2" t="inlineStr"><is><t>...</t></is></c>
    inline = re.findall(r'<c r="([A-Z]+)(\d+)"[^>]*><is><t[^>]*>(.*?)</t></is></c>', ws_xml)

    # Build row dict: row_num -> {col: str_value}
    row_data: dict[int, dict[str, str]] = {}
    for col, row, vidx in cells:
        r = int(row)
        # t="s" cells: vidx is shared string index (digits only) → lookup in si[]
        # no t attr cells: vidx is numeric (may be float) → keep as-is
        if vidx.isdigit():
            idx = int(vidx)
            row_data.setdefault(r, {})[col] = si[idx] if idx < len(si) else f"[MISSING:{idx}]"
        else:
            row_data.setdefault(r, {})[col] = vidx  # numeric value
    for col, row, val in inline:
        r = int(row)
        row_data.setdefault(r, {})[col] = val

    records = []
    for rnum in sorted(row_data.keys()):
        if rnum == 1:
            continue  # skip header
        row = row_data[rnum]
        # Col C = ITEM NUMBER (SKU)
        sku = (row.get("C") or "").strip()
        if not sku:
            continue
        # Col D = English name (DESCRIPTION OF GOODS, English)
        # Col B = Japanese name
        # Col G = quantity (numeric)
        # Col E = unit (PCS/...)
        # Col F = related SKU (ITEM NO for pairing)
        # Col H = unit price (RMB, numeric)
        # Col J = material
        # Col O/Q = weight (KGS, numeric)
        records.append(
            {
                "sku":          sku,
                "name_en":      (row.get("S") or "").strip(),   # English name (Col S, always valid)
                "name_zh":      None,                            # no Chinese name in xlsx
                "name_ja":      (row.get("B") or "").strip(),   # Japanese name
                "unit_price":   _to_float(row.get("H")),         # unit price (RMB)
                "box_qty":      _to_int(row.get("G")),           # quantity
                "weight_net":   _to_float(row.get("O")),
                "weight_gross": _to_float(row.get("Q")),
                "unit":         (row.get("E") or "").strip() or "PCS",
                "source":       "xlsx",
            }
        )
    return records


def _to_float(v: str | None) -> float | None:
    if v is None:
        return None
    try:
        return float(v)
    except (ValueError, TypeError):
        return None


def _to_int(v: str | None) -> int | None:
    if v is None:
        return None
    try:
        return int(float(v))
    except (ValueError, TypeError):
        return None


# ── 3. 清洗与转换 ────────────────────────────────────────────────────────────

def clean_product(rec: dict, source: str = "goods") -> dict | None:
    """
    清洗一条记录，返回 product INSERT 字段 dict。
    返回 None 表示跳过（HS code 无效等）。
    """
    sku = (rec.get("sku") or "").strip()

    # SKU 必须非空
    if not sku or sku.lower() == "null":
        return None

    # SKU 拆分: 'in083-a' → master + sub
    if "-" in sku:
        master = sku[: sku.index("-")]
        sub    = sku[sku.index("-") + 1 :]
    else:
        master = sku
        sub    = None

    # HS code: 必须是数字且非 '0'
    hs = (rec.get("hs_code") or "").strip()
    if hs and hs != "0" and re.fullmatch(r"[0-9]+", hs):
        hs_code = hs
    else:
        hs_code = None  # goods.sql 3 条无效；xlsx 本身无 HS code

    # 含税单价
    price_raw = rec.get("unit_price")
    if isinstance(price_raw, str):
        price_raw = price_raw.strip()
        unit_price = float(price_raw) if price_raw and price_raw.lower() != "null" else None
    elif isinstance(price_raw, (int, float)):
        unit_price = float(price_raw)
    else:
        unit_price = None

    # 税率
    if source == "goods":
        tax_raw = (rec.get("tax_rate") or "").strip()
        if tax_raw and tax_raw.lower() != "null":
            try:
                tax = float(tax_raw) / 100.0
            except ValueError:
                tax = 0.1000
        else:
            tax = 0.1000
    else:
        tax = 0.1000  # xlsx 无税率，默认 10%

    # 毛重: g → kg（goods.sql 是 g，xlsx 已是 kg）
    if source == "goods":
        wg_raw = rec.get("weight_gross")
        if isinstance(wg_raw, str):
            wg_raw = wg_raw.strip()
            gross = float(wg_raw) / 1000.0 if wg_raw and wg_raw.lower() != "null" else None
        elif isinstance(wg_raw, (int, float)):
            gross = float(wg_raw) / 1000.0
        else:
            gross = None
    else:
        gross = rec.get("weight_gross")  # xlsx 已是 kg

    # 净重: g → kg
    if source == "goods":
        wn_raw = rec.get("weight_net")
        if isinstance(wn_raw, str):
            wn_raw = wn_raw.strip()
            net = float(wn_raw) / 1000.0 if wn_raw and wn_raw.lower() != "null" else None
        elif isinstance(wn_raw, (int, float)):
            net = float(wn_raw) / 1000.0
        else:
            net = None
    else:
        net = rec.get("weight_net")  # xlsx 已是 kg

    # 每箱数量（xlsx 有 goods.sql 无）
    if source == "xlsx":
        bq = rec.get("box_qty")
        units_per_pkg = bq if bq and bq > 0 else None
    else:
        bq_raw = rec.get("box_qty")
        if isinstance(bq_raw, str):
            bq_raw = bq_raw.strip()
            if bq_raw and bq_raw.lower() != "null" and re.fullmatch(r"[0-9]+", bq_raw):
                units_per_pkg = int(bq_raw)
            else:
                units_per_pkg = None
        elif isinstance(bq_raw, (int, float)):
            units_per_pkg = int(bq_raw)
        else:
            units_per_pkg = None

    # 原产国
    if source == "goods":
        origin = (rec.get("origin") or "").strip()
        if not origin or origin.lower() == "null":
            origin = "中国"
    else:
        origin = "中国"  # xlsx 无 origin

    # 名称
    name_en = (rec.get("name_en") or "").strip()
    name_zh = (rec.get("name_zh") or "").strip()
    name_ja = (rec.get("name_ja") or "").strip()
    if name_en.lower() == "null":
        name_en = ""
    if name_zh.lower() == "null":
        name_zh = ""
    if name_ja.lower() == "null":
        name_ja = ""

    # 申报要素
    decl = (rec.get("declaration_elements") or "").strip()
    if decl.lower() == "null":
        decl = ""

    # 备注
    remark = (rec.get("remark") or "").strip()
    if remark.lower() == "null":
        remark = ""

    # 最近使用日期
    last_used = (rec.get("last_used") or "").strip()
    if last_used.lower() == "null":
        last_used = ""

    # 单位
    unit = (rec.get("unit") or "").strip() or "PCS"
    if unit.lower() == "null":
        unit = "PCS"

    return {
        "master_code":          master,
        "sub_code":             sub or None,
        "name_en":              name_en or None,
        "name_zh":              name_zh or None,
        "name_ja":              name_ja or None,
        "unit_price_rmb":       unit_price,
        "tax_rate":             tax,
        "gross_weight_kg":      round(gross, 6) if gross is not None else None,
        "net_weight_kg":         round(net, 6) if net is not None else None,
        "hs_code":              hs_code,
        "declaration_elements": decl or None,
        "units_per_package":    units_per_pkg,
        "origin":               origin,
        "remarks":              remark or None,
        "last_used_date":       last_used or None,
        "unit":                 unit,
        "source":               source,
        "original_sku":         sku,
        "factory_name":         (rec.get("factory_name") or "").strip() or None,
    }


# ── 4. SQL 生成 ─────────────────────────────────────────────────────────────

COLUMNS_PRODUCT = [
    "master_code", "sub_code", "name_en", "name_zh", "name_ja",
    "unit_price_rmb", "tax_rate", "gross_weight_kg", "net_weight_kg",
    "hs_code", "declaration_elements", "units_per_package", "origin",
    "remarks", "last_used_date", "unit", "is_deleted", "create_by",
    "update_by", "create_time", "update_time",
]


def sql_escape(v) -> str:
    if v is None:
        return "NULL"
    s = str(v).replace("\\", "\\\\").replace("'", "\\'")
    return f"'{s}'"


def format_product_insert(rec: dict) -> str:
    vals = []
    for col in COLUMNS_PRODUCT[:-5]:  # skip is_deleted, create_by, update_by, create_time, update_time
        vals.append(sql_escape(rec.get(col)))
    vals.extend(["FALSE", "'etl'", "'etl'", "NOW()", "NOW()"])
    return f"INSERT INTO product ({', '.join(COLUMNS_PRODUCT)})\nVALUES ({', '.join(vals)});"


def sql_val(v) -> str:
    """返回不带外层引号的原始值（用于子查询 SELECT 列表）"""
    if v is None:
        return "NULL"
    return str(v).replace("\\", "\\\\").replace("'", "\\'")


# ── 5. 主流程 ────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default=None, help="输出 SQL 文件路径")
    parser.add_argument("--dry-run", action="store_true", help="仅统计，不输出 SQL")
    args = parser.parse_args()

    print("=== ETL 商品数据 ===", file=sys.stderr)
    print(f"源文件: {GOODS_SQL}, {DB_XLSX}", file=sys.stderr)

    # 解析
    goods_records = parse_goods_sql(GOODS_SQL)
    xlsx_records  = parse_db_xlsx(DB_XLSX)
    print(f"goods.sql 原始记录: {len(goods_records)}", file=sys.stderr)
    print(f"DB.xlsx   原始记录: {len(xlsx_records)}", file=sys.stderr)

    # 清洗 goods.sql
    goods_products = []
    skipped_hs = 0
    for rec in goods_records:
        p = clean_product(rec, source="goods")
        if p is None:
            skipped_hs += 1
            continue
        goods_products.append(p)
    print(f"goods.sql 有效记录: {len(goods_products)} (跳过 HS 无效: {skipped_hs})", file=sys.stderr)

    # xlsx: 过滤掉已在 goods_products 中的 SKU（按 master_code 匹配）
    goods_master_skus = {p["master_code"] for p in goods_products}
    xlsx_new = []
    xlsx_dupe = 0
    for rec in xlsx_records:
        p = clean_product(rec, source="xlsx")
        if p is None:
            continue
        # xlsx SKU 格式同 goods.sql: 可能有 master-sub
        if p["master_code"] in goods_master_skus:
            xlsx_dupe += 1
            continue
        xlsx_new.append(p)
    print(f"DB.xlsx  新增记录: {len(xlsx_new)} (重复 SKU 跳过: {xlsx_dupe})", file=sys.stderr)
    print(f"  → 总计待导入 product: {len(goods_products) + len(xlsx_new)}", file=sys.stderr)

    all_products = goods_products + xlsx_new

    # 统计
    print("\n=== 统计 ===", file=sys.stderr)
    print(f"HS code 有值: {sum(1 for p in all_products if p['hs_code'])}", file=sys.stderr)
    print(f"含工厂关联 (goods.sql): {sum(1 for p in goods_products if p['factory_name'])}", file=sys.stderr)
    print(f"goods.sql 税率分布: {sorted(set(round(p['tax_rate'],4) for p in goods_products))}", file=sys.stderr)
    print(f"xlsx 税率: 全部默认 0.1000", file=sys.stderr)

    if args.dry_run:
        print("\n=== 干跑完成（--dry-run，不输出 SQL）===", file=sys.stderr)
        return

    # 生成 SQL
    lines = []
    lines.append("-- ============================================================")
    lines.append(f"-- product_seed_data.sql  (ETL 脚本: etl_product.py)")
    lines.append(f"-- 生成时间: {__import__('datetime').datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    lines.append("-- 数据来源:")
    lines.append(f"--   A) goods.sql  → {len(goods_products)} 条（完整字段）")
    lines.append(f"--   B) DB.xlsx    → {len(xlsx_new)} 条（无 HS code / 工厂关联）")
    lines.append("-- ============================================================")
    lines.append("")

    # product INSERT（分批，每 100 条一个 INSERT）
    BATCH = 100
    lines.append("-- -------------------------------------------------------")
    lines.append("-- product 表数据")
    lines.append("-- -------------------------------------------------------")

    batches = [all_products[i : i + BATCH] for i in range(0, len(all_products), BATCH)]
    for batch_idx, batch in enumerate(batches):
        vals_list = []
        for p in batch:
            vals = []
            for col in COLUMNS_PRODUCT[:-5]:
                vals.append(sql_escape(p.get(col)))
            vals.extend(["FALSE", "'etl'", "'etl'", "NOW()", "NOW()"])
            vals_list.append(f"  ({', '.join(vals)})")
        lines.append(
            f"INSERT INTO product ({', '.join(COLUMNS_PRODUCT)}) VALUES"
        )
        lines.append(",\n".join(vals_list) + ";")
        lines.append("")

    # product_factory 关联（仅 goods.sql 来源，有 factory_name）
    factory_products = [p for p in goods_products if p.get("factory_name")]
    if factory_products:
        lines.append("-- -------------------------------------------------------")
        lines.append("-- product_factory 多对多关联")
        lines.append(f"-- 共 {len(factory_products)} 条（仅 goods.sql 来源）")
        lines.append("-- -------------------------------------------------------")
        for p in factory_products:
            sku = p["original_sku"]
            factory = p["factory_name"]
            sub_cond = f" AND p.sub_code = '{sql_val(p['sub_code'])}'" if p.get("sub_code") else " AND p.sub_code IS NULL"
            lines.append(
                f"-- SKU: {sku}, 工厂: {factory}\n"
                f"INSERT INTO product_factory (product_id, factory_id, supplier_sku, moq, is_preferred)\n"
                f"SELECT p.id, f.id, '{sql_val(sku)}', 1, TRUE\n"
                f"FROM product p\n"
                f"JOIN factory f ON f.factory_name = '{sql_val(factory)}'\n"
                f"WHERE p.master_code = '{p['master_code']}'"
                + sub_cond
                + " ON DUPLICATE KEY UPDATE supplier_sku = VALUES(supplier_sku);"
            )

    # 迁移报告
    lines.append("")
    lines.append("-- -------------------------------------------------------")
    lines.append("-- 迁移报告")
    lines.append("-- -------------------------------------------------------")
    lines.append("SELECT CONCAT('product 总条数: ', COUNT(*)) AS msg FROM product;")
    lines.append("SELECT CONCAT('product_factory 关联条数: ', COUNT(*)) AS msg FROM product_factory;")

    sql_text = "\n".join(lines)

    if args.output:
        Path(args.output).write_text(sql_text, encoding="utf-8")
        print(f"SQL 已写入: {args.output}", file=sys.stderr)
        print(f"共 {len(sql_text.splitlines())} 行", file=sys.stderr)
    else:
        print(sql_text)


if __name__ == "__main__":
    main()
