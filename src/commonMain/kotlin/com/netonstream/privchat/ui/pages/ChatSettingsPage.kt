package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.*
import com.netonstream.privchat.sdk.dto.ChannelListEntry
import com.netonstream.privchat.ui.components.ChatAvatar
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.gearui.theme.Theme
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.primitives.GearLazyColumn
import com.gearui.foundation.typography.Typography
import com.gearui.foundation.AvatarSpecs
import com.gearui.components.navbar.NavBar
import com.gearui.components.cell.Cell
import com.gearui.components.switch.Switch
import com.gearui.components.dialog.Dialog
import com.gearui.components.dialog.DialogContent
import com.gearui.components.button.Button
import com.gearui.components.button.ButtonType
import com.gearui.components.button.ButtonTheme
import com.gearui.components.button.ButtonSize
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 聊天设置页面
 *
 * @param channel 频道信息
 * @param isAdmin 是否是管理员（群聊时有效）
 * @param onBack 返回回调
 * @param onGroupNameClick 点击群名称回调
 * @param onGroupQrCodeClick 点击群二维码回调
 * @param onGroupMembersClick 点击群成员回调
 * @param onGroupManageClick 点击群管理回调
 * @param onMuteChange 免打扰状态变化回调
 * @param onPinChange 置顶状态变化回调
 * @param onLeaveGroup 退出群聊回调
 * @param modifier Modifier
 */
@Composable
fun ChatSettingsPage(
    channel: ChannelListEntry,
    groupMemberCount: Int = channel.memberCount.toInt(),
    isAdmin: Boolean = false,
    onBack: () -> Unit,
    onGroupNameClick: () -> Unit = {},
    onGroupQrCodeClick: () -> Unit = {},
    onGroupMembersClick: () -> Unit = {},
    onGroupManageClick: () -> Unit = {},
    onMuteChange: suspend (Boolean) -> Result<Boolean> = { Result.success(it) },
    onPinChange: suspend (Boolean) -> Result<Boolean> = { Result.success(it) },
    onLeaveGroup: suspend () -> Result<Boolean> = { Result.success(true) },
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    val colors = Theme.colors
    val scope = rememberCoroutineScope()

    // 本地状态
    var isMuted by remember { mutableStateOf(channel.isLowPriority) }
    var isPinned by remember { mutableStateOf(channel.isFavourite) }

    // 确认对话框
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }
    var isLeaving by remember { mutableStateOf(false) }

    // 是否是群聊
    val isGroup = !channel.isDm

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        // 顶部导航栏
        NavBar(
            title = strings.chatSettingsTitle,
            useDefaultBack = true,
            onBackClick = onBack,
        )

        // 设置列表
        GearLazyColumn(modifier = Modifier.fillMaxSize()) {
            // 群聊特有设置
            if (isGroup) {
                // 群名称
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    Cell(
                        title = strings.chatSettingsGroupName,
                        description = channel.name,
                        arrow = true,
                        onClick = onGroupNameClick,
                    )
                }

                // 群二维码
                item {
                    Cell(
                        title = strings.chatSettingsGroupQrCode,
                        arrow = true,
                        onClick = onGroupQrCodeClick,
                    )
                }

                // 群成员
                item {
                    Cell(
                        title = strings.chatSettingsGroupMembers,
                        description = "($groupMemberCount)",
                        arrow = true,
                        onClick = onGroupMembersClick,
                    )
                }

                // 群管理（仅管理员可见）
                if (isAdmin) {
                    item {
                        Cell(
                            title = strings.chatSettingsGroupManage,
                            arrow = true,
                            onClick = onGroupManageClick,
                        )
                    }
                }
            }

            // 通用设置
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 消息免打扰
            item {
                Cell(
                    title = strings.chatSettingsMute,
                    trailing = {
                        Switch(
                            checked = isMuted,
                            onCheckedChange = { newValue ->
                                scope.launch {
                                    onMuteChange(newValue).onSuccess {
                                        isMuted = newValue
                                    }
                                }
                            }
                        )
                    },
                )
            }

            // 置顶聊天
            item {
                Cell(
                    title = strings.chatSettingsPin,
                    trailing = {
                        Switch(
                            checked = isPinned,
                            onCheckedChange = { newValue ->
                                scope.launch {
                                    onPinChange(newValue).onSuccess {
                                        isPinned = newValue
                                    }
                                }
                            }
                        )
                    },
                )
            }

            // 群聊特有操作
            if (isGroup) {
                // 分割线
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 退出群聊
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surface)
                            .clickable { showLeaveConfirmDialog = true }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = strings.chatSettingsLeaveGroup,
                            style = Typography.BodyMedium,
                            color = colors.danger
                        )
                    }
                }
            }

            // 底部间距
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // 退出群聊确认对话框
    Dialog.Host(
        visible = showLeaveConfirmDialog,
        dismissOnOutside = !isLeaving,
        onDismiss = { if (!isLeaving) showLeaveConfirmDialog = false }
    ) {
        DialogContent(
            title = strings.chatSettingsLeaveGroupConfirmTitle,
            message = strings.chatSettingsLeaveGroupConfirmMessage,
            actions = {
                Button(
                    text = strings.cancel,
                    type = ButtonType.TEXT,
                    theme = ButtonTheme.DEFAULT,
                    size = ButtonSize.SMALL,
                    disabled = isLeaving,
                    onClick = { showLeaveConfirmDialog = false }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    text = if (isLeaving) strings.loading else strings.confirm,
                    type = ButtonType.FILL,
                    theme = ButtonTheme.DANGER,
                    size = ButtonSize.SMALL,
                    disabled = isLeaving,
                    onClick = {
                        isLeaving = true
                        scope.launch {
                            onLeaveGroup().fold(
                                onSuccess = {
                                    showLeaveConfirmDialog = false
                                    isLeaving = false
                                    onBack()
                                },
                                onFailure = {
                                    isLeaving = false
                                }
                            )
                        }
                    }
                )
            }
        )
    }
}
