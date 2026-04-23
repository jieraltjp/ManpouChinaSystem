import pymysql
import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

conn = pymysql.connect(
    host='192.168.13.202', port=23306,
    user='root', password='manpou23306',
    database='manpou', charset='utf8mb4'
)
conn.autocommit(True)
c = conn.cursor()

# Step 1: Add jan_code column
try:
    c.execute("ALTER TABLE product ADD COLUMN jan_code VARCHAR(64) DEFAULT NULL COMMENT 'JANコード from goods_master' AFTER sub_code")
    print("Added jan_code column")
except Exception as e:
    print("Add jan_code: {}".format(e))

# Step 2: Add status column
try:
    c.execute("ALTER TABLE product ADD COLUMN status VARCHAR(32) DEFAULT NULL COMMENT '商品区分: 通常/予約' AFTER category")
    print("Added status column")
except Exception as e:
    print("Add status: {}".format(e))

# Step 3: Populate jan_code from goods_master.JANコード
c.execute("""
    UPDATE product p
    INNER JOIN goods_master gm ON p.id = gm.id
    SET p.jan_code = gm.JANコード
    WHERE gm.JANコード IS NOT NULL AND gm.JANコード != ''
""")
print("Updated jan_code: {} rows".format(c.rowcount))

# Step 4: Populate status from goods_master.商品区分
# 予約 -> FACTORY_DIRECT / 通常 -> ORDINARY
c.execute("""
    UPDATE product p
    INNER JOIN goods_master gm ON p.id = gm.id
    SET p.status = gm.商品区分
    WHERE gm.商品区分 IN ('予約', '通常')
""")
print("Updated status: {} rows".format(c.rowcount))

# Step 5: Clear warehouse (wrong data)
c.execute("UPDATE product SET warehouse = NULL")
print("Cleared warehouse: {} rows".format(c.rowcount))

# Step 6: Clean remarks - delete 通常/予約 only, keep real remarks
c.execute("UPDATE product SET remarks = NULL WHERE remarks IN ('通常', '予約')")
print("Cleaned remarks (通常/予約): {} rows".format(c.rowcount))

# Step 7: Stats
c.execute("""
    SELECT
        COUNT(*) total,
        SUM(jan_code IS NOT NULL) jan_ok,
        SUM(status IS NOT NULL) status_ok,
        SUM(warehouse IS NULL) wh_cleared,
        SUM(remarks IN ('通常','予約')) remarks_bad
    FROM product
""")
r = c.fetchone()
print("\nFinal stats:")
print("  Total: {}, jan_code: {}, status: {}".format(r[0], r[1], r[2]))
print("  warehouse cleared: {}, remarks (通常/予約 left): {}".format(r[3], r[4]))

conn.close()
print("\nDone.")
