package com.netonstream.privchat.ui.components

import androidx.compose.runtime.Composable
import com.gearui.foundation.layout.Spacing
import com.gearui.foundation.primitives.Text
import com.gearui.theme.Theme
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 右侧 A–Z 索引条。联系人页与 @ 选人面板共用——两处各写一个，迟早会长成两种手感。
 *
 * 只列**当前有人的**字母：列满 26 个而多数点不动，点上去没反应会让人以为界面卡了。
 */
@Composable
fun IndexBar(
    letters: List<Char>,
    modifier: Modifier = Modifier,
    onPick: (Char) -> Unit,
) {
    if (letters.isEmpty()) return
    Column(
        modifier = modifier.padding(end = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { letter ->
            Text(
                text = letter.toString(),
                style = Theme.typography.label,
                color = Theme.colors.mutedForeground,
                modifier = Modifier
                    .clickable { onPick(letter) }
                    .padding(horizontal = Spacing.xs, vertical = 1.dp),
            )
        }
    }
}
