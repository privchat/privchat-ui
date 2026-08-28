package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.gearui.components.input.Input
import com.gearui.components.input.InputSize
import com.gearui.components.navbar.NavBar
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.typography.Typography
import com.gearui.theme.Theme
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 群名称编辑页。
 *
 * 当前群名通过 gearui [Input] 的 `value` 预填到可编辑文本框；用户进入即可在原名基础上
 * 修改。autoFocus=true 弹键盘。Save 按钮挂在 NavBar 右上角，仅在内容变化且非空时高亮。
 */
@Composable
fun GroupNameEditPage(
    currentName: String,
    onBack: () -> Unit,
    onSave: suspend (String) -> Result<Unit>,
    onError: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    val colors = Theme.colors
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var groupName by remember { mutableStateOf(currentName) }

    val trimmed = groupName.trim()
    val canSave = trimmed.isNotEmpty() && trimmed != currentName

    val doSave: () -> Unit = {
        if (canSave) {
            scope.launch {
                onSave(trimmed).fold(
                    onSuccess = { onBack() },
                    onFailure = { onError?.invoke(it.message ?: strings.networkError) },
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        NavBar(
            title = strings.groupNameEditTitle,
            useDefaultBack = true,
            onBackClick = onBack,
            rightWidget = {
                com.netonstream.privchat.ui.components.NavBarAction(
                    text = strings.save,
                    enabled = canSave,
                    onClick = doSave,
                )
            },
            rightWidgetWidth = com.netonstream.privchat.ui.components.NavBarActionSlotWidth,
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Input(
                value = groupName,
                onValueChange = { groupName = it },
                size = InputSize.LARGE,
                clearable = true,
                onClear = { groupName = "" },
                autoFocus = true,
            )
        }
    }
}
