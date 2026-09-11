package com.netonstream.privchat.ui.components

import androidx.compose.runtime.Composable
import com.gearui.components.icon.Icons
import com.gearui.foundation.primitives.Icon
import com.gearui.theme.Theme
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.shape.CircleShape
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 选人列表左侧的圆形选择标记（微信「选择联系人」那种）。
 *
 * 为什么不是方形复选框：选人列表里，选择状态属于**这一行的人**，标记跟在头像前面读起来
 * 就是"选了这个人"；方框摆在行尾，视线要在名字和行尾之间来回跑，多选时尤其明显。
 *
 * 它**不自己接点击**：整行才是点击目标。标记可点、行也可点的话，点名字算不算选中就说不清了。
 */
@Composable
fun SelectionDot(
    selected: Boolean,
    modifier: Modifier = Modifier,
    /** 达到选择上限且本行未选中：画成灰的，明确表示点了也没用。 */
    enabled: Boolean = true,
) {
    val colors = Theme.colors
    val fill = when {
        selected -> colors.primary
        else -> Color.Transparent
    }
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(fill)
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = if (enabled) colors.border else colors.muted,
                        shape = CircleShape,
                    )
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(name = Icons.check, size = 14.dp, tint = colors.primaryForeground)
        }
    }
}
