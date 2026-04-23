import pymysql
import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

conn = pymysql.connect(
    host='192.168.13.202', port=23306,
    user='root', password='manpou23306',
    database='manpou', charset='utf8mb4'
)
conn.autocommit(False)
c = conn.cursor()

# Target column order
new_order = [
    'id','master_code','sub_code','jan_code',
    'name_zh','name_en','name_ja',
    'category','status','color_name',
    'material','material_ja',
    'origin','warehouse',
    'quantities','carton_qty','unit','unit_price_rmb','amount_rmb',
    'tax_rate','tax_point',
    'length_cm','width_cm','height_cm','volume_cbm',
    'gross_weight_kg','net_weight_kg',
    'hs_code','hs_code_jp','declaration_elements',
    'units_per_package','package_length_cm','package_width_cm','package_height_cm','package_volume_cbm','package_weight_kg',
    'requires_qc',
    'image_url','remarks','last_used_date',
    'create_by','create_time','is_deleted','update_by','update_time',
]

# Get current column types from information_schema
c.execute("""
    SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, EXTRA, COLUMN_KEY
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA='manpou' AND TABLE_NAME='product'
    ORDER BY ORDINAL_POSITION
""")
col_info = {}
for row in c.fetchall():
    col_info[row[0]] = {
        'type': row[1],
        'nullable': row[2],
        'default': row[3],
        'extra': row[4],
        'key': row[5],
    }

print("Step 1: Build CREATE TABLE statement...")
col_defs = []
for col in new_order:
    info = col_info.get(col)
    if not info:
        print("  WARNING: column '{}' not found".format(col))
        continue
    d = "`{}` {}".format(col, info['type'])
    if info['nullable'] == 'NO':
        d += ' NOT NULL'
    if info['default'] is not None and info['default'] != '':
        if info['default'].startswith("b'") and info['default'].endswith("'"):
            d += ' DEFAULT {}'.format(info['default'])
        elif 'CURRENT_TIMESTAMP' in str(info['default']).upper():
            d += ' DEFAULT {}'.format(info['default'])
        elif info['default'] == '':
            d += " DEFAULT ''"
        else:
            d += " DEFAULT '{}'".format(str(info['default']).replace("'", "''"))
    if info['extra']:
        d += ' {}'.format(info['extra'])
    if info['key'] == 'PRI':
        d += ' PRIMARY KEY'
    col_defs.append(d)

# Add any extra columns not in new_order
for col, info in col_info.items():
    if col not in new_order:
        d = "`{}` {}".format(col, info['type'])
        if info['nullable'] == 'NO':
            d += ' NOT NULL'
        if info['default'] is not None:
            d += ' DEFAULT {}'.format(info['default'])
        if info['extra']:
            d += ' {}'.format(info['extra'])
        col_defs.append(d)
        print("  Extra column appended: {}".format(col))

create_sql = "CREATE TABLE product_new (\n  " + ',\n  '.join(col_defs) + "\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
print("  CREATE TABLE with {} columns".format(len(col_defs)))
# Debug: print the relevant part
for cd in col_defs:
    if 'is_deleted' in cd or 'create_time' in cd or 'create_by' in cd:
        print("  DEBUG: {}".format(cd))

# Get current indexes (excluding PRIMARY)
c.execute("SHOW INDEX FROM product")
indexes = {}
for row in c.fetchall():
    idx_name = row[2]
    if idx_name == 'PRIMARY':
        continue
    if idx_name not in indexes:
        indexes[idx_name] = []
    indexes[idx_name].append(row[4])  # column name

print("\nStep 2: Create new table...")
c.execute(create_sql)

print("Step 3: Recreate indexes (except PRIMARY)...")
for idx_name, cols in indexes.items():
    col_list = ','.join('`{}`'.format(cn) for cn in cols)
    uniq = 'UNIQUE' if idx_name.startswith('uk') else ''
    try:
        c.execute("CREATE {} INDEX {} ON product_new ({})".format(uniq, idx_name, col_list))
        print("  Created index {}".format(idx_name))
    except Exception as e:
        print("  Index {}: {}".format(idx_name, e))

print("\nStep 4: Fetch all data...")
select_cols = ', '.join('`{}`'.format(col) for col in new_order if col in col_info)
c.execute("SELECT {} FROM product".format(select_cols))
rows = c.fetchall()
print("  Fetched {} rows".format(len(rows)))

print("\nStep 5: Insert data...")
placeholders = ', '.join(['%s'] * len(new_order))
insert_sql = "INSERT INTO product_new ({}) VALUES ({})".format(
    ', '.join('`{}`'.format(col) for col in new_order if col in col_info),
    placeholders
)

batch_size = 500
for i in range(0, len(rows), batch_size):
    batch = rows[i:i+batch_size]
    c.executemany(insert_sql, batch)
    conn.commit()
    print("  {}-{}".format(i+1, min(i+batch_size, len(rows))))

print("\nStep 6: Swap tables...")
c.execute("DROP TABLE product")
print("  Dropped product")
c.execute("RENAME TABLE product_new TO product")
print("  Renamed product_new -> product")

conn.commit()
conn.close()

# Verify in new connection
print("\nStep 7: Verify...")
conn2 = pymysql.connect(host='192.168.13.202', port=23306, user='root', password='manpou23306', database='manpou', charset='utf8mb4')
conn2.autocommit(True)
c2 = conn2.cursor()
c2.execute("SHOW COLUMNS FROM product")
for r in c2.fetchall():
    print("  {}".format(r[0]))
c2.execute("SELECT COUNT(*) FROM product")
print("  Total rows: {}".format(c2.fetchone()[0]))
conn2.close()
print("\nDone!")
