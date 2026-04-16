package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.gearui.components.button.Button
import com.gearui.components.button.ButtonSize
import com.gearui.components.button.ButtonTheme
import com.gearui.components.input.Input
import com.gearui.components.navbar.NavBar
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.typography.Typography
import com.gearui.theme.Theme
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun GroupNameEditPage(
    currentName: String,
    onBack: () -> Unit,
    onSave: suspend (String) -> Result<Unit>,
    onError: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    var groupName by remember(currentName) { mutableStateOf(currentName) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.background)
    ) {
        NavBar(
            title = strings.chatSettingsGroupName,
            useDefaultBack = true,
            onBackClick = onBack,
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = strings.chatSettingsGroupName,
                style = Typography.BodySmall,
                color = Theme.colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Input(
                value = groupName,
                onValueChange = { groupName = it },
                placeholder = strings.chatSettingsGroupName,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                text = strings.confirm,
                theme = ButtonTheme.PRIMARY,
                size = ButtonSize.SMALL,
                disabled = groupName.trim().isEmpty() || groupName.trim() == currentName,
                onClick = {
                    scope.launch {
                        onSave(groupName.trim()).fold(
                            onSuccess = { onBack() },
                            onFailure = { onError?.invoke(it.message ?: strings.networkError) },
                        )
                    }
                }
            )
        }
    }
}
