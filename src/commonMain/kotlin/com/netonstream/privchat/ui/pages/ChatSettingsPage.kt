package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.*
import com.netonstream.privchat.sdk.dto.ChannelListEntry
import com.netonstream.privchat.sdk.dto.GroupSettingsUpdateInput
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.components.ChatAvatar
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.gearui.theme.Theme
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.primitives.GearLazyColumn
import com.gearui.foundation.typography.Typography
import com.gearui.foundation.avatar.AvatarSizeTokens
import com.gearui.components.navbar.NavBar
import com.gearui.components.cell.Cell
import com.gearui.components.switch.Switch
import com.gearui.components.dialog.Dialog
import com.gearui.components.dialog.DialogContent
import com.gearui.components.button.Button
import com.gearui.components.button.ButtonType
import com.gearui.components.button.ButtonTheme
import com.gearui.components.button.ButtonSize
import com.gearui.components.actionsheet.ActionSheet
import com.gearui.components.actionsheet.ActionSheetItem
import com.gearui.components.toast.Toast
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
 * @param showMute 是否显示消息免打扰开关（默认显示；客服等单一会话场景可隐藏）
 * @param modifier Modifier
 */
@Composable
fun ChatSettingsPage(
    channel: ChannelListEntry,
    groupMemberCount: Int = channel.memberCount.toInt(),
    isAdmin: Boolean = false,
    /** 当前用户是否为该群群主；群主时禁用"退出群聊"按钮（需先转让 / 解散）。 */
    isOwner: Boolean = false,
    onBack: () -> Unit,
    onGroupNameClick: () -> Unit = {},
    onGroupQrCodeClick: () -> Unit = {},
    onGroupMembersClick: () -> Unit = {},
    onGroupInviteClick: () -> Unit = {},
    onGroupManageClick: () -> Unit = {},
    onMuteChange: suspend (Boolean) -> Result<Boolean> = { Result.success(it) },
    onPinChange: suspend (Boolean) -> Result<Boolean> = { Result.success(it) },
    onLeaveGroup: suspend () -> Result<Boolean> = { Result.success(true) },
    showMute: Boolean = true,
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
    // 群主/管理员可见并可改群管理设置（allowSearch / joinPolicy / memberCanInvite /
    // allowMemberAddFriend / allMuted）。服务端仍是权威鉴权。
    val isManager = isGroup && (isOwner || isAdmin)

    // 群设置初值（groupGetSettings 读，groupUpdateSettings 改；每次只 patch 改动项）。
    var allowSearch by remember(channel.channelId) { mutableStateOf<Boolean?>(null) }
    var memberCanInvite by remember(channel.channelId) { mutableStateOf<Boolean?>(null) }
    var allowMemberAddFriend by remember(channel.channelId) { mutableStateOf<Boolean?>(null) }
    var allMuted by remember(channel.channelId) { mutableStateOf<Boolean?>(null) }
    var joinPolicy by remember(channel.channelId) { mutableStateOf<UByte?>(null) }

    if (isManager) {
        LaunchedEffect(channel.channelId) {
            withContext(Dispatchers.Default) {
                PrivChat.client.groupGetSettings(channel.channelId)
            }.onSuccess { s ->
                allowSearch = s.allowSearch
                memberCanInvite = s.memberCanInvite
                allowMemberAddFriend = s.allowMemberAddFriend
                allMuted = s.allMuted
                joinPolicy = s.joinPolicy
            }
        }
    }

    // 单字段 patch helper：只把改动项放进 GroupSettingsUpdateInput，其余保持 null（不更新）。
    fun patchSetting(
        apply: GroupSettingsUpdateInput.() -> Unit,
        onOk: () -> Unit,
    ) {
        scope.launch {
            val input = GroupSettingsUpdateInput(groupId = channel.channelId).apply(apply)
            withContext(Dispatchers.Default) {
                PrivChat.client.groupUpdateSettings(input)
            }.onSuccess { onOk() }
                .onFailure { Toast.error(it.message ?: strings.groupSettingsUpdateFailed) }
        }
    }

    fun joinPolicyLabel(value: UByte?): String = when (value?.toInt()) {
        0 -> strings.groupSettingsJoinPolicyNone
        2 -> strings.groupSettingsJoinPolicyOpen
        else -> strings.groupSettingsJoinPolicyApproval
    }

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

                // 邀请成员
                item {
                    Cell(
                        title = "邀请成员",
                        arrow = true,
                        onClick = onGroupInviteClick,
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

                // 群管理设置（仅群主/管理员可见可改；服务端鉴权）。
                if (isManager) {
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                    item {
                        Text(
                            text = strings.groupSettingsSectionTitle,
                            style = Typography.Label,
                            color = colors.mutedForeground,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    // 加群方式（0/1/2）→ ActionSheet 选择。
                    item {
                        Cell(
                            title = strings.groupSettingsJoinPolicy,
                            description = joinPolicyLabel(joinPolicy),
                            arrow = true,
                            onClick = {
                                ActionSheet.showList(
                                    description = strings.groupSettingsJoinPolicy,
                                    items = listOf(
                                        ActionSheetItem(label = strings.groupSettingsJoinPolicyNone),
                                        ActionSheetItem(label = strings.groupSettingsJoinPolicyApproval),
                                        ActionSheetItem(label = strings.groupSettingsJoinPolicyOpen),
                                    ),
                                    onSelected = { _, index ->
                                        val newValue = index.toUByte()
                                        if (newValue != joinPolicy) {
                                            patchSetting(
                                                apply = { this.joinPolicy = newValue },
                                                onOk = { joinPolicy = newValue },
                                            )
                                        }
                                    },
                                )
                            },
                        )
                    }
                    // allowSearch
                    item {
                        SettingSwitchCell(
                            title = strings.groupSettingsAllowSearch,
                            checked = allowSearch == true,
                            onToggle = { newValue ->
                                patchSetting(
                                    apply = { this.allowSearch = newValue },
                                    onOk = { allowSearch = newValue },
                                )
                            },
                        )
                    }
                    // memberCanInvite
                    item {
                        SettingSwitchCell(
                            title = strings.groupSettingsMemberCanInvite,
                            checked = memberCanInvite == true,
                            onToggle = { newValue ->
                                patchSetting(
                                    apply = { this.memberCanInvite = newValue },
                                    onOk = { memberCanInvite = newValue },
                                )
                            },
                        )
                    }
                    // allowMemberAddFriend
                    item {
                        SettingSwitchCell(
                            title = strings.groupSettingsAllowMemberAddFriend,
                            checked = allowMemberAddFriend == true,
                            onToggle = { newValue ->
                                patchSetting(
                                    apply = { this.allowMemberAddFriend = newValue },
                                    onOk = { allowMemberAddFriend = newValue },
                                )
                            },
                        )
                    }
                    // allMuted（全员禁言）：统一走 groupUpdateSettings（单一路径）。
                    item {
                        SettingSwitchCell(
                            title = strings.groupSettingsAllMuted,
                            checked = allMuted == true,
                            onToggle = { newValue ->
                                patchSetting(
                                    apply = { this.allMuted = newValue },
                                    onOk = { allMuted = newValue },
                                )
                            },
                        )
                    }
                }
            }

            // 通用设置
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 消息免打扰
            if (showMute) {
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

                // 退出群聊：群主不可直接退出（须先转让群主或解散群——后续 Phase B 实现）
                item {
                    val leaveEnabled = !isOwner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surface)
                            .clickable(enabled = leaveEnabled) { showLeaveConfirmDialog = true }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = strings.chatSettingsLeaveGroup,
                            style = Typography.BodyMedium,
                            color = if (leaveEnabled) colors.destructive else colors.mutedForeground
                        )
                    }
                    if (isOwner) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "群主无法直接退出群聊，请先转让群主或解散群",
                                style = Typography.BodySmall,
                                color = colors.mutedForeground,
                            )
                        }
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

    // 加群方式选择走全局 ActionSheet 单例，需在页面根部挂 Host。
    ActionSheet.Host()
}

/** 群设置开关行：左标题 + 右 Switch。 */
@Composable
private fun SettingSwitchCell(
    title: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Cell(
        title = title,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = { onToggle(it) },
            )
        },
    )
}
