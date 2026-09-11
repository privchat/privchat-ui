#!/usr/bin/env python3
"""生成 CjkPinyinInitials.kt：汉字 → 拼音首字母表。

    pip3 install "pypinyin==0.55.0"
    python3 scripts/gen-pinyin-initials.py

🔴 这个脚本存在的理由：那张两万字的表不能是"某次手工生成后没人知道怎么再来一次"的产物。
表要改（换数据源、扩范围、修某个字），必须能重新跑出来并 diff。

🔴 **版本必须钉死**。不钉版本的"可重新生成"是假的：换一版 pypinyin 可能改掉若干字的
默认读音，重跑出来的表与库里的不一致，而 diff 里看不出哪一边才是对的。脚本会自检版本，
不匹配直接报错——宁可让人显式改版本号，也不要悄悄产出另一张表。

数据来源：pypinyin（MIT，https://github.com/mozillazg/python-pinyin），其字音数据源自
Unihan 数据库。生成物只含"汉字 → 单个大写字母"，不含 pypinyin 的词库。
"""
import sys

import pypinyin
from pypinyin import pinyin, Style

# 钉死数据源版本，见上面的说明。
EXPECTED_PYPINYIN = "0.55.0"

START, END = 0x4E00, 0x9FFF  # CJK 基本区
OUT = "src/commonMain/kotlin/com/netonstream/privchat/ui/i18n/CjkPinyinInitials.kt"

HEADER = '''package com.netonstream.privchat.ui.i18n

/**
 * 汉字 → 拼音首字母表，覆盖 CJK 基本区 U+4E00–U+9FFF（20992 字）。
 *
 * 由 `scripts/gen-pinyin-initials.py` 用 pypinyin 0.55.0（MIT，数据源自 Unihan）生成，
 * 不是手写的。查不到读音的 68 个字落在 `#`。改表请改脚本后重跑，不要手改这个文件。
 *
 * 为什么是"按字"而不是"按词"：索引条只需要**首字**的首字母。按词表能把多音姓氏
 * （单/仇/曾/朴…）判对，但那需要把整个词库搬进来。代价是这类姓氏会按常用读音归组——
 * 这是已知取舍，不是 bug。
 */
internal const val CJK_PINYIN_INITIALS: String =
'''


def main() -> None:
    if pypinyin.__version__ != EXPECTED_PYPINYIN:
        sys.exit(
            f"pypinyin {pypinyin.__version__} != {EXPECTED_PYPINYIN}；"
            "要换版本请改 EXPECTED_PYPINYIN 并把重新生成的表一起提交"
        )
    letters = []
    for cp in range(START, END + 1):
        r = pinyin(chr(cp), style=Style.FIRST_LETTER, errors=lambda x: None)
        c = (r[0][0][:1].upper() if r and r[0] and r[0][0] else "#")
        letters.append(c if "A" <= c <= "Z" else "#")
    table = "".join(letters)
    lines = [table[i:i + 120] for i in range(0, len(table), 120)]
    body = "\n".join('    "%s" +' % line for line in lines).rstrip(" +")
    with open(OUT, "w", encoding="utf-8") as f:
        f.write(HEADER + body + "\n")
    print(f"{OUT}: {len(table)} chars, {table.count('#')} without a reading")


if __name__ == "__main__":
    main()
