import json
import re

with open(r'd:/Programme/java/ManpouChinaSystem/docs/database/sql/goods.sql', 'r', encoding='utf-8') as f:
    goods_sql = f.read()

lines = goods_sql.split('\n')
inserts, current = [], ''
for line in lines:
    current += line + '\n'
    if current.strip().endswith(');'):
        inserts.append(current.strip())
        current = ''

def parse_insert(sql):
    inner = sql[sql.index('VALUES') + 7:].rstrip().lstrip('(').rstrip().rstrip(')')
    parts, current, in_str, i = [], '', False, 0
    while i < len(inner):
        c = inner[i]
        if c == "'":
            if in_str and i + 1 < len(inner) and inner[i+1] == "'":
                current += "'"
                i += 2
                continue
            in_str = not in_str
            i += 1
            continue
        if not in_str and c == ',':
            parts.append(current.strip())
            current = ''
            i += 1
            continue
        current += c
        i += 1
    if current.strip():
        parts.append(current.strip())
    return parts

def val(v):
    v = v.strip()
    if v == 'NULL':
        return None
    return v.strip("'")

goods_by_sku = {}
for ins in inserts:
    try:
        parts = parse_insert(ins)
        if len(parts) >= 18:
            sku = val(parts[1])
            if not sku:
                continue
            m = re.match(r'^([a-zA-Z0-9]+)-([a-zA-Z0-9]+)$', sku)
            master_code = m.group(1) if m else sku
            sub_code = m.group(2) if m else None
            goods_by_sku[sku] = {
                'master_code': master_code,
                'sub_code': sub_code,
                'sku': sku,
                'hs_code': val(parts[2]),
                'name_en': val(parts[3]),
                'name_zh': val(parts[4]),
                'unit_price': val(parts[5]),
                'unit': val(parts[7]),
                'weight_gross': val(parts[8]),
                'weight_net': val(parts[9]),
                'declaration_elements': val(parts[10]),
                'box_qty': val(parts[11]),
                'origin': val(parts[13]),
                'factory_name': val(parts[14]),
                'buyer': val(parts[15]),
            }
    except Exception:
        pass

with open(r'd:/Programme/java/ManpouChinaSystem/docs/database/sql/DB_parsed.json', 'r', encoding='utf-8') as f:
    db_rows = json.load(f)

def get_val(row, col):
    return row.get(str(col))

db_by_sku = {}
for row in db_rows[1:]:
    sku = str(get_val(row, 2) or '').strip()
    if not sku:
        continue
    name_zh = get_val(row, 1)
    material_ja = get_val(row, 9)
    decl = ''
    if name_zh and material_ja:
        decl = name_zh + '|' + material_ja
    elif name_zh:
        decl = name_zh
    m = re.match(r'^([a-zA-Z0-9]+)-([a-zA-Z0-9]+)$', sku)
    master_code = m.group(1) if m else sku
    sub_code = m.group(2) if m else None
    db_by_sku[sku] = {
        'master_code': master_code,
        'sub_code': sub_code,
        'sku': sku,
        'name_en': get_val(row, 18),
        'name_zh': name_zh,
        'unit_price': get_val(row, 6),
        'unit': get_val(row, 4) or 'PCS',
        'weight_gross': get_val(row, 16),
        'weight_net': get_val(row, 14),
        'box_qty': get_val(row, 5),
        'declaration_elements': decl,
        'quantities': get_val(row, 3),
        'amount_rmb': get_val(row, 7),
        'material_ja': material_ja,
    }

# Build merged records keyed by (master_code, sub_code)
merged = {}
for sku, g in goods_by_sku.items():
    key = (g['master_code'], g['sub_code'])
    if key not in merged:
        merged[key] = {'master_code': g['master_code'], 'sub_code': g['sub_code']}
    merged[key]['goods'] = g

for sku, d in db_by_sku.items():
    key = (d['master_code'], d['sub_code'])
    if key not in merged:
        merged[key] = {'master_code': d['master_code'], 'sub_code': d['sub_code']}
    merged[key]['db'] = d

def sql_str(v):
    if v is None:
        return 'NULL'
    return "'" + str(v).replace('\\', '\\\\').replace("'", "''") + "'"

def sql_num(v):
    if v is None or str(v).strip() == '':
        return 'NULL'
    try:
        return str(float(v))
    except Exception:
        return 'NULL'

header = [
    "-- =============================================================",
    "-- Migration: goods.sql + DB.json -> product",
    "-- Generated: 2026-04-23",
    "-- Merge strategy:",
    "--   hs_code, origin -> goods.sql priority (structured product data)",
    "--   unit_price, weight, name_en, declaration_elements -> DB.json priority (manifest data)",
    "-- =============================================================",
    "",
    "SET FOREIGN_KEY_CHECKS=0;",
    "",
    "-- Step 1: Add temp unique key for upsert by (master_code, sub_code)",
    "ALTER TABLE `product` ADD UNIQUE INDEX `uk_master_sub` (`master_code`(32), `sub_code`(32));",
    "",
]

output_lines = []
output_lines.extend(header)
count = 0

for key, rec in sorted(merged.items(), key=lambda x: (x[0][0] or '', x[0][1] or '')):
    master_code = rec['master_code']
    sub_code = rec['sub_code']
    if not master_code:
        continue

    g = rec.get('goods')
    d = rec.get('db')

    hs_code = (g['hs_code'] if g else None)
    name_en = (d['name_en'] if d else None) or (g['name_en'] if g else None)
    name_zh = (g['name_zh'] if g else None) or (d['name_zh'] if d else None)
    name_ja = (d['name_zh'] if d else None)
    unit_price = (d['unit_price'] if d else None) or (g['unit_price'] if g else None)
    unit = (d['unit'] if d else None) or (g['unit'] if g else None)
    weight_gross = (d['weight_gross'] if d else None) or (g['weight_gross'] if g else None)
    weight_net = (d['weight_net'] if d else None) or (g['weight_net'] if g else None)
    origin = (g['origin'] if g else None) or '中国'
    decl = (d['declaration_elements'] if d else None) or (g['declaration_elements'] if g else None)
    box_qty = (d['box_qty'] if d else None)
    quantities = (d['quantities'] if d else None)
    amount_rmb = (d['amount_rmb'] if d else None)
    material_ja = (d['material_ja'] if d else None)

    updates = []
    if hs_code:
        updates.append('hs_code = ' + sql_str(hs_code))
    if name_en:
        updates.append('name_en = ' + sql_str(name_en))
    if name_zh:
        updates.append('name_zh = ' + sql_str(name_zh))
    if name_ja:
        updates.append('name_ja = ' + sql_str(name_ja))
    if unit_price:
        updates.append('unit_price_rmb = ' + sql_num(unit_price))
    if unit:
        updates.append('unit = ' + sql_str(unit))
    if weight_gross:
        updates.append('gross_weight_kg = ' + sql_num(weight_gross))
    if weight_net:
        updates.append('net_weight_kg = ' + sql_num(weight_net))
    if origin:
        updates.append('origin = ' + sql_str(origin))
    if decl:
        updates.append('declaration_elements = ' + sql_str(decl))
    if box_qty:
        updates.append('carton_qty = ' + sql_num(box_qty))
    if quantities:
        updates.append('quantities = ' + sql_num(quantities))
    if amount_rmb:
        updates.append('amount_rmb = ' + sql_num(amount_rmb))
    if material_ja:
        updates.append('material_ja = ' + sql_str(material_ja))

    update_clause = ', '.join(updates) if updates else 'master_code = master_code'
    sql = (
        "INSERT INTO `product` "
        "(master_code, sub_code, hs_code, name_en, name_zh, name_ja, unit_price_rmb, unit, "
        "gross_weight_kg, net_weight_kg, carton_qty, quantities, amount_rmb, material_ja, "
        "origin, declaration_elements, create_by, create_time, update_by, update_time) "
        "VALUES ("
        + sql_str(master_code) + ", " + sql_str(sub_code) + ", " + sql_str(hs_code) + ", "
        + sql_str(name_en) + ", " + sql_str(name_zh) + ", " + sql_str(name_ja) + ", "
        + sql_num(unit_price) + ", " + sql_str(unit) + ", "
        + sql_num(weight_gross) + ", " + sql_num(weight_net) + ", "
        + sql_num(box_qty) + ", " + sql_num(quantities) + ", " + sql_num(amount_rmb) + ", " + sql_str(material_ja) + ", "
        + sql_str(origin) + ", " + sql_str(decl) + ", "
        + "'system', NOW(6), 'system', NOW(6)) "
        "ON DUPLICATE KEY UPDATE "
        + update_clause + ", update_by = 'system', update_time = NOW(6);"
    )
    output_lines.append(sql)
    count += 1

output_lines.append("")
output_lines.append("SET FOREIGN_KEY_CHECKS=1;")
output_lines.append("")
output_lines.append("-- Step 2: Remove temp unique key")
output_lines.append("ALTER TABLE `product` DROP INDEX `uk_master_sub`;")
output_lines.append("")
output_lines.append("-- Total upsert records: " + str(count))

sql_content = '\n'.join(output_lines)

with open(r'd:/Programme/java/ManpouChinaSystem/docs/database/sql/migration_product_merge.sql', 'w', encoding='utf-8') as f:
    f.write(sql_content)

overlap = sum(1 for r in merged.values() if 'goods' in r and 'db' in r)
only_g = sum(1 for r in merged.values() if 'goods' in r and 'db' not in r)
only_d = sum(1 for r in merged.values() if 'db' in r and 'goods' not in r)

stats = (
    f"goods.sql SKUs: {len(goods_by_sku)}\n"
    f"DB.json SKUs: {len(db_by_sku)}\n"
    f"Total merged: {count}\n"
    f"Overlap (both): {overlap}\n"
    f"Only goods.sql: {only_g}\n"
    f"Only DB.json: {only_d}\n"
)
with open(r'd:/Programme/java/ManpouChinaSystem/docs/database/sql/migration_stats.txt', 'w', encoding='utf-8') as f:
    f.write(stats)

print("Generated: migration_product_merge.sql")
print(stats)
