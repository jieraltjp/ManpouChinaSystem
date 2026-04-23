import pymysql
import sys
import io

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

conn = pymysql.connect(
    host='192.168.13.202',
    port=23306,
    user='root',
    password='manpou23306',
    database='manpou',
    charset='utf8mb4'
)
conn.autocommit(True)
cursor = conn.cursor()

# Get column names
cursor.execute("""
    SELECT COLUMN_NAME
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA='manpou' AND TABLE_NAME='cn_hs_code'
    ORDER BY ORDINAL_POSITION
""")
cn_cols = [row[0] for row in cursor.fetchall()]

cursor.execute("""
    SELECT COLUMN_NAME
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA='manpou' AND TABLE_NAME='jp_hs_code'
    ORDER BY ORDINAL_POSITION
""")
jp_cols = [row[0] for row in cursor.fetchall()]

cn_code_col = cn_cols[1]
jp_code_col = jp_cols[1]

print("Step 1: CN col={} JP col={}".format(cn_code_col.encode('utf-8'), jp_code_col.encode('utf-8')))

# Add hs_code_jp column
try:
    cursor.execute("ALTER TABLE product ADD COLUMN hs_code_jp VARCHAR(20) DEFAULT NULL COMMENT 'Japanese HS code' AFTER hs_code")
    print("Added hs_code_jp column OK")
except Exception as e:
    print("Add col: {}".format(e))

# Update HS codes
update_sql = (
    "UPDATE product p "
    "INNER JOIN goods_master gm ON p.id = gm.id "
    "INNER JOIN goods_hs_mapping m ON gm.id = m.goods_id "
    "LEFT JOIN cn_hs_code cn ON m.cn_hs_id = cn.id "
    "LEFT JOIN jp_hs_code jp ON m.jp_hs_id = jp.id "
    "SET p.hs_code = cn.`{0}`, p.hs_code_jp = jp.`{1}`, p.update_by = 'system', p.update_time = NOW(6)"
).format(cn_code_col, jp_code_col)

cursor.execute(update_sql)
print("Updated {} rows".format(cursor.rowcount))

# Stats
cursor.execute("""
    SELECT COUNT(*), SUM(hs_code IS NOT NULL), SUM(hs_code_jp IS NOT NULL), SUM(hs_code IS NOT NULL AND hs_code_jp IS NOT NULL)
    FROM product
""")
r = cursor.fetchone()
print("Total={} HasCN={} HasJP={} Both={}".format(r[0], r[1], r[2], r[3]))

# Sample
cursor.execute("SELECT id, sub_code, hs_code, hs_code_jp FROM product WHERE hs_code IS NOT NULL OR hs_code_jp IS NOT NULL LIMIT 5")
for r in cursor.fetchall():
    print("  {} {} CN={} JP={}".format(r[0], r[1], r[2], r[3]))

cursor.close()
conn.close()
print("Done.")
