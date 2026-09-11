#!/usr/bin/env python3
"""生成 CjkPinyinData.kt：汉字 → 全拼（含多音）。

    pip3 install "pypinyin==0.55.0"
    python3 scripts/gen-pinyin-data.py

🔴 **版本必须钉死**。不钉版本的"可重新生成"是假的：换一版 pypinyin 会改掉若干字的读音，
重跑出来的表与库里的不一致，而 diff 里看不出哪一边才是对的。

数据来源：pypinyin（MIT，https://github.com/mozillazg/python-pinyin），字音数据源自 Unihan。
生成物只含"汉字 → 音节"，不含 pypinyin 的词库。

## 为什么把**所有读音**都带上，而不是只带词库判定的那一个

搜索要的是"别漏"。带上全部读音，`单` 同时能被 `dan` 和 `shan` 搜到，`重庆` 能被
`chongqing` 搜到——不需要把十几万条的词组库搬进来。代价是会多匹配（`重庆` 也能被
`zhongqing` 搜到），但多出来的结果排在原文匹配之后，比"搜不到单先生"好得多。

词库是**显示**用拼音时才需要的（把名字转成拼音文本），我们不做那件事。
"""
import sys

import pypinyin
from pypinyin import Style, pinyin

EXPECTED_PYPINYIN = "0.55.0"
START, END = 0x4E00, 0x9FFF  # CJK 基本区
OUT = "src/commonMain/kotlin/com/netonstream/privchat/ui/i18n/CjkPinyinData.kt"

# JVM 的单个字符串常量上限是 65535 字节（UTF-8），超了是编译期错误。
# 拆成多段在运行期拼接，从结构上避开这个坑。
CHUNK = 20000

HEADER = '''package com.netonstream.privchat.ui.i18n

/**
 * 汉字 → 拼音音节表，覆盖 CJK 基本区 U+4E00–U+9FFF。
 *
 * 由 `scripts/gen-pinyin-data.py` 用 pypinyin %s（MIT，数据源自 Unihan）生成。
 * 不要手改这个文件——改表请改脚本后重跑，diff 才有意义。
 *
 * 编码：
 * - [PINYIN_SYLLABLES]：去重后的音节，`|` 分隔（%d 个）。
 * - [PINYIN_PRIMARY]：每字 **2 个大写字母**的 base-26 下标，指向音节表；`..` = 无读音。
 *   2 个字母能表示 676 个音节，够用。定长是为了 O(1) 取值，不必解析分隔符。
 * - [PINYIN_ALTERNATES]：多音字的**其余**读音，`字:音节,音节;` 形式（%d 字）。
 *   这里刻意存可读的汉字与音节而不是下标——排查"为什么这个名字搜不到"时，
 *   能直接 grep 到那个字。
 *
 * 字符串按 %d 字符分段：JVM 单个字符串常量上限 65535 字节，整表会超。
 */
'''


def base26(index: int) -> str:
    assert 0 <= index < 26 * 26, index
    return chr(ord("A") + index // 26) + chr(ord("A") + index % 26)


def emit_chunked(name: str, text: str) -> str:
    parts = [text[i:i + CHUNK] for i in range(0, len(text), CHUNK)]
    body = " +\n        ".join('"%s"' % p for p in parts)
    return "internal val %s: String =\n    run {\n        %s\n    }\n" % (name, body)


def main() -> None:
    if pypinyin.__version__ != EXPECTED_PYPINYIN:
        sys.exit(
            f"pypinyin {pypinyin.__version__} != {EXPECTED_PYPINYIN}；"
            "要换版本请改 EXPECTED_PYPINYIN 并把重新生成的表一起提交"
        )

    syllables: dict[str, int] = {}
    primary: list[str] = []
    alternates: list[str] = []

    for cp in range(START, END + 1):
        ch = chr(cp)
        raw = pinyin(ch, style=Style.NORMAL, heteronym=True, errors=lambda x: None)
        reads = [s for s in (raw[0] if raw and raw[0] else []) if s.isascii() and s.isalpha()]
        if not reads:
            primary.append("..")
            continue
        seen: list[str] = []
        for s in reads:
            if s not in seen:
                seen.append(s)
        for s in seen:
            syllables.setdefault(s, len(syllables))
        primary.append(base26(syllables[seen[0]]))
        if len(seen) > 1:
            alternates.append("%s:%s;" % (ch, ",".join(seen[1:])))

    table = "|".join(sorted(syllables, key=lambda s: syllables[s]))
    header = HEADER % (EXPECTED_PYPINYIN, len(syllables), len(alternates), CHUNK)
    with open(OUT, "w", encoding="utf-8") as f:
        f.write(header)
        f.write('internal const val PINYIN_SYLLABLES: String =\n    "%s"\n\n' % table)
        f.write(emit_chunked("PINYIN_PRIMARY", "".join(primary)))
        f.write("\n")
        f.write(emit_chunked("PINYIN_ALTERNATES", "".join(alternates)))

    print(
        f"{OUT}: {len(syllables)} syllables, {len(primary)} chars, "
        f"{len(alternates)} polyphones, {primary.count('..')} without a reading"
    )


if __name__ == "__main__":
    main()
