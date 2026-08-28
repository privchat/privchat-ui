package com.netonstream.privchat.ui.components

import androidx.compose.runtime.Composable
import com.gearui.components.button.Button
import com.gearui.components.button.ButtonSize
import com.gearui.components.button.ButtonTheme
import com.gearui.components.button.ButtonType
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * NavBar 右上角的文字主操作（保存/创建/邀请/完成…），全局统一样式：
 * 实心主题色小按钮（品牌黄底黑字），禁用态由 Button 统一渲染。
 *
 * 🔴 使用时 NavBar 必须传 `rightWidgetWidth`：默认槽宽 56dp 是按纯图标定的，
 * 两个汉字的按钮会被裁掉。两字用 [NavBarActionSlotWidth]，带计数的（"创建(3)"）
 * 用 [NavBarActionSlotWidthWide]。
 */
@Composable
fun NavBarAction(
    text: String,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.padding(end = 12.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Button(
            text = text,
            type = ButtonType.FILL,
            theme = ButtonTheme.PRIMARY,
            size = ButtonSize.SMALL,
            disabled = !enabled,
            loading = loading,
            onClick = { if (enabled) onClick() },
        )
    }
}

/** [NavBarAction] 两字文案的 NavBar 槽宽。 */
val NavBarActionSlotWidth: Dp = 84.dp

/** [NavBarAction] 带计数文案（"创建(3)"）的 NavBar 槽宽。 */
val NavBarActionSlotWidthWide: Dp = 108.dp
