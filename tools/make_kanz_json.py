#!/usr/bin/env python3
"""Turn a plain-text list into the JSON file Easy Quran imports.

Input: UTF-8 text, one ayah per line:   surah|ayah|urdu translation|optional tafseer
Usage: python3 make_kanz_json.py kanz.txt kanz.json "Kanz ul Iman - Ahmad Raza Khan"
"""
import json, sys
src, dst, label = sys.argv[1], sys.argv[2], sys.argv[3]
out = []
for n, line in enumerate(open(src, encoding="utf-8"), 1):
    line = line.strip()
    if not line:
        continue
    p = line.split("|")
    if len(p) < 3:
        sys.exit(f"line {n}: expected surah|ayah|translation")
    item = {"s": int(p[0]), "a": int(p[1]), "ur": p[2].strip()}
    if len(p) > 3 and p[3].strip():
        item["tf"] = p[3].strip()
    out.append(item)
json.dump({"source": label, "ayat": out}, open(dst, "w", encoding="utf-8"), ensure_ascii=False)
print(f"wrote {len(out)} ayat (full Quran = 6236)")
