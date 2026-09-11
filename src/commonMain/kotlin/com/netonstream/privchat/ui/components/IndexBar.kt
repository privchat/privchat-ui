package com.netonstream.privchat.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gearui.foundation.layout.Spacing
import com.gearui.foundation.primitives.Text
import com.gearui.theme.Theme
import com.netonstream.privchat.ui.platform.HapticBridge
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.gestures.awaitEachGesture
import com.tencent.kuikly.compose.foundation.gestures.awaitFirstDown
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.CircleShape
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.input.pointer.PointerEventPass
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.unit.dp

/** 每个字母占的高度。定高才能由触点 y 直接算出落在第几个字母上。 */
private val LETTER_HEIGHT = 20.dp

/**
 * 右侧 A–Z 索引条。联系人页与 @ 选人面板共用——两处各写一个，迟早会长成两种手感。
 *
 * 交互按微信：**按住不放沿着字母滑**，经过哪个字母列表就跳到哪个分组，当前字母高亮，
 * 并在左侧浮出一个大字气泡（手指会挡住字母本身，没有气泡就看不清自己选中了什么）。
 * 每换一个字母给一次轻触反馈。
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

    // 手指按住期间的当前字母；抬手后清空（高亮和气泡都只在交互中出现）。
    var active by remember(letters) { mutableStateOf<Char?>(null) }
    val letterHeightPx = with(LocalDensity.current) { LETTER_HEIGHT.toPx() }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        // 气泡在**左边**：手指压在字母条上，气泡跟在条上就被挡住了。
        val current = active
        if (current != null) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Theme.colors.muted),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = current.toString(),
                    style = Theme.typography.titleLarge,
                    color = Theme.colors.foreground,
                )
            }
            Box(modifier = Modifier.width(Spacing.sm))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(end = Spacing.xs)
                .pointerInput(letters) {
                    awaitEachGesture {
                        // requireUnconsumed=false：列表在下面，谁先拿到 down 不确定；
                        // 这一条是"按在索引条上"，不该被别人的消费挡掉。
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        var last: Char? = null

                        fun update(y: Float) {
                            val index = (y / letterHeightPx).toInt().coerceIn(0, letters.lastIndex)
                            val letter = letters[index]
                            if (letter != last) {
                                last = letter
                                active = letter
                                // 换一个字母震一下：手指挡着字母时，这是"确实切换了"的
                                // 主要反馈来源。
                                HapticBridge.selectionChanged()
                                onPick(letter)
                            }
                        }

                        update(down.position.y)
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            change.consume()
                            update(change.position.y)
                        }
                        active = null
                    }
                },
        ) {
            letters.forEach { letter ->
                val selected = letter == active
                Box(
                    modifier = Modifier
                        .size(LETTER_HEIGHT)
                        .clip(CircleShape)
                        .background(if (selected) Theme.colors.primary else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = letter.toString(),
                        style = Theme.typography.label,
                        color = if (selected) Theme.colors.primaryForeground else Theme.colors.mutedForeground,
                    )
                }
            }
        }
    }
}
