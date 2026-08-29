package com.netonstream.privchat.ui.components

import androidx.compose.runtime.Composable
import com.gearui.components.button.Button
import com.gearui.components.button.ButtonSize
import com.gearui.components.button.ButtonTheme
import com.gearui.components.button.ButtonType
import com.gearui.components.icon.Icons
import com.gearui.foundation.primitives.Icon
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.typography.Typography
import com.gearui.theme.Theme
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 页面级失败态：居中图标 + **一句人话** + 重试按钮。与 [PageLoading] 配套，
 * 所有「进页面先拉数据」的页面统一用这两个，不再各写各的。
 *
 * 🔴 [message] 必须是给人看的文案（走 `UserFacingError.message(e, fallback)`）。
 * 绝不允许把 `throwable.message` 拼进去：原始异常里是
 * `NSURLErrorDomain Code=-1004 ... http://127.0.0.1:8080/app/member/user/get`
 * 这种东西——对用户是天书，同时把服务器地址、端口、内部路径全泄了出去。
 * 诊断细节由 `UserFacingError` 写进日志。
 */
@Composable
fun PageError(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = Theme.colors
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            name = Icons.error,
            size = 40.dp,
            tint = colors.mutedForeground,
        )
        Column(modifier = Modifier.height(12.dp)) {}
        Text(
            text = message,
            style = Typography.BodyLarge,
            color = colors.mutedForeground,
        )
        if (onRetry != null) {
            Column(modifier = Modifier.height(20.dp)) {}
            Button(
                text = PrivChatI18n.strings.retry,
                icon = Icons.refresh,
                type = ButtonType.FILL,
                theme = ButtonTheme.PRIMARY,
                size = ButtonSize.MEDIUM,
                onClick = onRetry,
            )
        }
    }
}
