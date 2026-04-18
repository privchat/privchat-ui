package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.gearui.components.input.Input
import com.gearui.components.navbar.NavBar
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.typography.Typography
import com.gearui.theme.Theme
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 编辑备注页面
 *
 * @param currentRemark 当前备注（已有备注值）
 * @param defaultValue 默认填充值（昵称 > 账号 > 空）
 * @param onBack 返回回调
 * @param onSave 保存回调，传入 null 表示清除备注
 * @param onError 错误回调
 */
@Composable
fun EditRemarkPage(
    currentRemark: String,
    defaultValue: String = "",
    onBack: () -> Unit,
    onSave: suspend (String?) -> Result<Unit>,
    onError: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    val initialValue = currentRemark.ifEmpty { defaultValue }
    var remark by remember(currentRemark) { mutableStateOf(initialValue) }
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }

    val canSave = !isSaving && remark.trim() != currentRemark

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.background)
    ) {
        NavBar(
            title = strings.userProfileRemark,
            useDefaultBack = true,
            onBackClick = onBack,
            rightWidget = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = canSave) {
                            isSaving = true
                            scope.launch {
                                val alias = remark.trim().ifEmpty { null }
                                onSave(alias).fold(
                                    onSuccess = { onBack() },
                                    onFailure = { onError?.invoke(it.message ?: strings.networkError) },
                                )
                                isSaving = false
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.save,
                        style = Typography.BodyMedium,
                        color = if (canSave) Theme.colors.primary else Theme.colors.textDisabled,
                    )
                }
            }
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Input(
                value = remark,
                onValueChange = { remark = it },
                placeholder = strings.userProfileRemarkPlaceholder,
                clearable = true,
                onClear = { remark = "" },
                autoFocus = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
