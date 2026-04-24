#!/usr/bin/env python3
import sys
sys.stdout.reconfigure(encoding="utf-8", errors="replace")
sys.stderr.reconfigure(encoding="utf-8", errors="replace")

"""
修复 product 表 name_zh 列：
- name_zh 为空 或 不含任何中日韩字符 → 翻译 name_en → 更新 name_zh
"""
import pymysql
import requests
import time

DB_CONFIG = {
    "host": "192.168.13.202",
    "port": 23306,
    "user": "root",
    "password": "manpou23306",
    "database": "manpou",
    "charset": "utf8mb4",
}
TRANSLATE_API = "http://127.0.0.1:6060/api/translate_batch.php"
BATCH_SIZE = 30
DRY_RUN = False

CJK_RANGES = [
    (0x4E00, 0x9FFF),   # CJK Unified Ideographs
    (0x3400, 0x4DBF),   # CJK Unified Ideographs Extension A
    (0x20000, 0x2A6DF), # CJK Unified Ideographs Extension B
    (0x2A700, 0x2B73F), # CJK Unified Ideographs Extension C
    (0x2B740, 0x2B81F), # CJK Unified Ideographs Extension D
    (0x3000, 0x303F),   # CJK Symbols and Punctuation
    (0xFF00, 0xFFEF),   # Halfwidth and Fullwidth Forms
    (0x1100, 0x115F),   # Hangul Jamo
    (0xAC00, 0xD7AF),   # Hangul Syllables
    (0x3040, 0x309F),   # Hiragana
    (0x30A0, 0x30FF),   # Katakana
]

def has_cjk(text):
    if not text:
        return False
    for char in text:
        cp = ord(char)
        for start, end in CJK_RANGES:
            if start <= cp <= end:
                return True
    return False

def translate_batch(texts, target_lang="zh", source_lang="en"):
    if not texts:
        return {}
    payload = {"texts": texts, "target": target_lang, "source": source_lang}
    try:
        resp = requests.post(TRANSLATE_API, json=payload, timeout=30)
        resp.raise_for_status()
        data = resp.json()
        results = {}
        if data.get("code") == 200 and "results" in data.get("data", {}):
            for original, result in zip(texts, data["data"]["results"]):
                if isinstance(result, dict) and result.get("code") == 200:
                    results[original] = result.get("data", {}).get("translated_text", original)
                else:
                    results[original] = original
        return results
    except Exception as e:
        print(f"  [!] API error: {e}")
        return {t: t for t in texts}

def color_map():
    """常见英文颜色 → 中文"""
    return {
        "black": "黑色", "white": "白色", "red": "红色", "blue": "蓝色",
        "green": "绿色", "yellow": "黄色", "gray": "灰色", "grey": "灰色",
        "brown": "棕色", "orange": "橙色", "pink": "粉色", "purple": "紫色",
        "navy": "藏青色", "khaki": "卡其色", "beige": "米色", "cream": "奶油色",
        "wine": "酒红色", "coffee": "咖啡色", "chocolate": "巧克力色",
        "mint": "薄荷绿", "candy": "糖果色", "passion": "热情红",
        "natural": "自然色", "olive": "橄榄绿", "gold": "金色", "silver": "银色",
    }

def post_translate(text, en_colors):
    """翻译后处理：把颜色关键词替换为中文"""
    text_lower = text.lower()
    for en, zh in en_colors.items():
        if en in text_lower:
            # 精确替换（区分大小写）
            idx = text_lower.find(en)
            text = text[:idx] + zh + text[idx + len(en):]
            text_lower = text.lower()
    return text

def main():
    conn = pymysql.connect(**DB_CONFIG, cursorclass=pymysql.cursors.DictCursor)
    cursor = conn.cursor()

    cursor.execute("""
        SELECT id, COALESCE(name_en, '') as name_en, COALESCE(name_zh, '') as name_zh
        FROM product
        WHERE name_en IS NOT NULL AND name_en != ''
    """)
    rows = cursor.fetchall()
    rows = [r for r in rows if not has_cjk(r["name_zh"])]

    to_fix = [r for r in rows if not has_cjk(r["name_zh"])]
    total = len(to_fix)
    print(f"Need fix: {total} records (no CJK in name_zh, has name_en)")

    if total == 0:
        print("Nothing to fix.")
        return

    print(f"Total to fix: {total} records")
    en_colors = color_map()
    updated = 0

    for i in range(0, total, BATCH_SIZE):
        batch = to_fix[i:i + BATCH_SIZE]
        batch_en = [r["name_en"] for r in batch]
        print(f"[{i+1}-{min(i+BATCH_SIZE, total)}/{total}] Translating...")

        translations = translate_batch(batch_en)
        if not translations:
            print(f"  [!] Translation failed, skip batch")
            continue

        for row in batch:
            en_text = row["name_en"]
            zh_new = post_translate(translations.get(en_text, en_text), en_colors)
            zh_new = zh_new.strip()

            if not zh_new:
                continue

            if DRY_RUN:
                print(f"  [DRY] id={row['id']}: '{row['name_zh']}' -> '{zh_new}'")
            else:
                cursor.execute("UPDATE product SET name_zh=%s WHERE id=%s", (zh_new, row["id"]))
                print(f"  [OK] id={row['id']}: {zh_new}")

            updated += 1

        if not DRY_RUN:
            conn.commit()
        time.sleep(0.3)

    print(f"\nDone: {updated} updated")
    cursor.close()
    conn.close()

if __name__ == "__main__":
    if "--dry" in sys.argv:
        DRY_RUN = True
        print("=== DRY RUN ===")
    main()
