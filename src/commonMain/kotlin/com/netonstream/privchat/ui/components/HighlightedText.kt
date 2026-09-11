package com.netonstream.privchat.ui.components

import androidx.compose.runtime.Composable
import com.gearui.foundation.primitives.Text
import com.gearui.theme.Theme
import com.netonstream.privchat.ui.search.TextMatch
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.SpanStyle
import com.gearui.foundation.typography.TextStyle
import com.tencent.kuikly.compose.ui.text.buildAnnotatedString
import com.tencent.kuikly.compose.ui.text.withStyle
import com.tencent.kuikly.compose.material3.Text as KuiklyText

/**
 * 把 [match] 命中的区间标成主色。
 *
 * 🔴 高亮不是装饰，是匹配区间对不对的**唯一可见证据**。搜索结果一定要走这里渲染，
 * 别在页面里另算一次"大概是哪几个字"——那等于把匹配逻辑抄了第二遍，还抄错了。
 *
 * 区间来自 [com.netonstream.privchat.ui.search.PinyinMatcher]，是**原文坐标**。
 * 这里只做越界与倒序的防御：拼音匹配出的区间不该越界，但真越界了也只是少一段高亮，
 * 不能让整条列表崩掉。
 */
@Composable
fun HighlightedText(
    text: String,
    match: TextMatch?,
    style: TextStyle = Theme.typography.bodyMedium,
    color: Color = Theme.colors.foreground,
    highlightColor: Color = Theme.colors.primary,
    modifier: Modifier = Modifier,
) {
    val ranges = match?.ranges
        ?.filter { it.first in text.indices && it.last in text.indices && it.first <= it.last }
        ?.sortedBy { it.first }
        .orEmpty()

    if (ranges.isEmpty()) {
        // 没有命中就走普通 Text：AnnotatedString 在这条路径上是纯开销。
        Text(text = text, style = style, color = color, modifier = modifier)
        return
    }

    val annotated = buildAnnotatedString {
        var cursor = 0
        ranges.forEach { range ->
            if (range.first > cursor) append(text.substring(cursor, range.first))
            if (range.first >= cursor) {
                withStyle(SpanStyle(color = highlightColor)) {
                    append(text.substring(range.first, range.last + 1))
                }
                cursor = range.last + 1
            }
        }
        if (cursor < text.length) append(text.substring(cursor))
    }

    KuiklyText(
        text = annotated,
        color = color,
        fontSize = style.fontSize,
        fontWeight = style.fontWeight,
        lineHeight = style.lineHeight,
        modifier = modifier,
    )
}
