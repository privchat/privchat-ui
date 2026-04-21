package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.*
import com.netonstream.privchat.sdk.dto.FriendEntry
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.gearui.components.navbar.NavBar
import com.gearui.components.cell.Cell
import com.gearui.components.switch.Switch
import com.gearui.components.dialog.Dialog
import com.gearui.components.dialog.DialogContent
import com.gearui.components.button.Button
import com.gearui.components.button.ButtonType
import com.gearui.components.button.ButtonTheme
import com.gearui.components.button.ButtonSize
import com.gearui.theme.Theme
import com.gearui.foundation.primitives.Text
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 好友设置页面
 *
 * 功能列表：
 * 1. 编辑备注
 * 2. 推荐分享给别人
 * 3. 特别关注（Switch开关）
 * 4. 加入黑名单（Switch开关）
 * 5. 分割线
 * 6. 删除联系人
 *
 * @param friend 好友信息
 * @param onBack 返回回调
 * @param onEditRemark 编辑备注回调
 * @param onShareFriend 分享好友回调
 * @param onSetSpecialFollow 设置特别关注回调
 * @param onSetBlocked 设置黑名单回调
 * @param onDeleteFriend 删除好友回调
 * @param modifier Modifier
 */
@Composable
fun FriendSettingsPage(
    friend: FriendEntry,
    onBack: () -> Unit,
    onEditRemark: () -> Unit = {},
    onShareFriend: () -> Unit = {},
    onSetSpecialFollow: (Boolean) -> Unit = {},
    onSetBlocked: suspend (Boolean) -> Result<Boolean> = { Result.success(true) },
    onDeleteFriend: suspend () -> Result<Boolean> = { Result.failure(NotImplementedError()) },
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    val colors = Theme.colors
    val scope = rememberCoroutineScope()

    // 特别关注状态
    var isSpecialFollow by remember { mutableStateOf(false) }

    // 黑名单状态
    var isBlocked by remember { mutableStateOf(false) }

    // 删除确认 Dialog
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        // 顶部导航栏
        NavBar(
            title = strings.friendSettingsTitle,
            useDefaultBack = true,
            onBackClick = onBack,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 设置选项
        Column(modifier = Modifier.fillMaxWidth().background(colors.surface)) {
            // 编辑备注
            Cell(
                title = strings.userProfileRemark,
                note = friend.remark ?: strings.settingsNotSet,
                arrow = true,
                onClick = onEditRemark
            )

            // 分隔线
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .height(0.5.dp)
                    .background(colors.divider)
            )

            // 推荐分享给别人
            Cell(
                title = strings.friendSettingsShare,
                arrow = true,
                onClick = onShareFriend
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 开关选项
        Column(modifier = Modifier.fillMaxWidth().background(colors.surface)) {
            // 特别关注
            Cell(
                title = strings.friendSettingsSpecialFollow,
                trailing = {
                    Switch(
                        checked = isSpecialFollow,
                        onCheckedChange = { checked ->
                            isSpecialFollow = checked
                            onSetSpecialFollow(checked)
                        }
                    )
                }
            )

            // 分隔线
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .height(0.5.dp)
                    .background(colors.divider)
            )

            // 加入黑名单
            Cell(
                title = strings.userProfileBlockUser,
                trailing = {
                    Switch(
                        checked = isBlocked,
                        onCheckedChange = { checked ->
                            scope.launch {
                                onSetBlocked(checked).onSuccess {
                                    isBlocked = checked
                                }
                            }
                        }
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 删除联系人
        Column(modifier = Modifier.fillMaxWidth().background(colors.surface)) {
            Cell(
                title = strings.friendSettingsDelete,
                onClick = { showDeleteConfirmDialog = true },
                trailing = {
                    Text(
                        text = "",
                        color = colors.danger
                    )
                }
            )
        }
    }

    // 删除确认 Dialog
    Dialog.Host(
        visible = showDeleteConfirmDialog,
        dismissOnOutside = true,
        onDismiss = { showDeleteConfirmDialog = false }
    ) {
        DialogContent(
            title = strings.userProfileDeleteFriendConfirmTitle,
            message = strings.userProfileDeleteFriendConfirmMessage,
            actions = {
                Button(
                    text = strings.cancel,
                    type = ButtonType.TEXT,
                    theme = ButtonTheme.DEFAULT,
                    size = ButtonSize.SMALL,
                    onClick = { showDeleteConfirmDialog = false }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    text = if (isDeleting) strings.userProfileDeleting else strings.confirm,
                    type = ButtonType.FILL,
                    theme = ButtonTheme.DANGER,
                    size = ButtonSize.SMALL,
                    onClick = {
                        if (!isDeleting) {
                            isDeleting = true
                            scope.launch {
                                onDeleteFriend().fold(
                                    onSuccess = {
                                        isDeleting = false
                                        showDeleteConfirmDialog = false
                                        onBack()
                                    },
                                    onFailure = {
                                        isDeleting = false
                                    }
                                )
                            }
                        }
                    }
                )
            }
        )
    }
}
