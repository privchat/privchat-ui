package com.netonstream.privchat.ui.pages

import com.gearui.foundation.field.FieldDescription
import com.gearui.foundation.layout.Spacing
import com.gearui.components.cellgroup.CellGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.foundation.interaction.collectIsPressedAsState
import com.tencent.kuikly.compose.foundation.interaction.MutableInteractionSource
import com.gearui.foundation.interaction.pressedSurfaceColor
import androidx.compose.runtime.*
import com.netonstream.privchat.sdk.dto.ChannelListEntry
import com.netonstream.privchat.sdk.dto.GroupSettingsUpdateInput
import com.netonstream.privchat.ui.error.UserFacingError
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.components.ChatAvatar
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.gearui.theme.Theme
import com.gearui.theme.groupedBackground
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.primitives.GearLazyColumn
import com.gearui.foundation.avatar.AvatarSizeTokens
import com.gearui.components.navbar.NavBar
import com.gearui.components.cell.Cell
import com.gearui.components.switch.Switch
import com.gearui.components.dialog.DialogAction
import com.gearui.components.dialog.DialogActionRole
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
    onGroupApprovalClick: () -> Unit = {},
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
                .onFailure { Toast.error(UserFacingError.message(it, strings.groupSettingsUpdateFailed)) }
        }
    }

    fun joinPolicyLabel(value: UByte?): String = when (value?.toInt()) {
        0 -> strings.groupSettingsJoinPolicyNone
        2 -> strings.groupSettingsJoinPolicyOpen
        else -> strings.groupSettingsJoinPolicyApproval
    }

    Column(modifier = modifier.fillMaxSize().background(colors.groupedBackground)) {
        // 顶部导航栏
        NavBar(
            title = strings.chatSettingsTitle,
            useDefaultBack = true,
            onBackClick = onBack,
        )

        // 设置列表
        GearLazyColumn(modifier = Modifier.fillMaxSize()) {
            // 群聊特有设置：一张卡片，不是一条条通栏白带。
            if (isGroup) {
                item {
                    val groupRows = buildList {
                        add(GroupRow.Name)
                        add(GroupRow.QrCode)
                        add(GroupRow.Members)
                        add(GroupRow.Invite)
                        if (isAdmin) add(GroupRow.Manage)
                        if (isManager) add(GroupRow.Approval)
                    }
                    CellGroup(
                        items = groupRows,
                        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    ) { row ->
                        when (row) {
                            GroupRow.Name -> Cell(
                                title = strings.chatSettingsGroupName,
                                // P6-1：收口到 GroupDisplay.titleOf（此前裸 channel.name 无 fallback，空群名显示空白）。
                                description = com.netonstream.privchat.ui.models.GroupDisplay.titleOf(channel.name),
                                // P6-3 上线要求：群改名尚未持久化（GROUP_SETTINGS_PERSISTENCE 未完成）→ 暂隐藏编辑入口，
                                // 只读展示群名，避免用户改了 resync 后丢失的假成功。
                                arrow = false,
                            )
                            GroupRow.QrCode -> Cell(
                                title = strings.chatSettingsGroupQrCode,
                                arrow = true,
                                onClick = onGroupQrCodeClick,
                            )
                            GroupRow.Members -> Cell(
                                title = strings.chatSettingsGroupMembers,
                                description = "($groupMemberCount)",
                                arrow = true,
                                onClick = onGroupMembersClick,
                            )
                            GroupRow.Invite -> Cell(
                                title = strings.groupInviteMembers,
                                arrow = true,
                                onClick = onGroupInviteClick,
                            )
                            GroupRow.Manage -> Cell(
                                title = strings.chatSettingsGroupManage,
                                arrow = true,
                                onClick = onGroupManageClick,
                            )
                            GroupRow.Approval -> Cell(
                                title = strings.groupApprovalTitle,
                                arrow = true,
                                onClick = onGroupApprovalClick,
                            )
                        }
                    }
                }

                // 群管理设置（仅群主/管理员可见可改；服务端鉴权）：一张带标题的卡片，
                // 不是夹在两张卡片之间的一串通栏白条。
                if (isManager) {
                    item {
                        CellGroup(
                            items = ManageRow.entries,
                            title = strings.groupSettingsSectionTitle,
                            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        ) { row ->
                            when (row) {
                                // 加群方式（0/1/2）→ ActionSheet 选择。
                                ManageRow.JoinPolicy -> Cell(
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
                                ManageRow.AllowSearch -> SettingSwitchCell(
                                    title = strings.groupSettingsAllowSearch,
                                    checked = allowSearch == true,
                                    onToggle = { v -> patchSetting(apply = { this.allowSearch = v }, onOk = { allowSearch = v }) },
                                )
                                ManageRow.MemberCanInvite -> SettingSwitchCell(
                                    title = strings.groupSettingsMemberCanInvite,
                                    checked = memberCanInvite == true,
                                    onToggle = { v -> patchSetting(apply = { this.memberCanInvite = v }, onOk = { memberCanInvite = v }) },
                                )
                                ManageRow.AllowMemberAddFriend -> SettingSwitchCell(
                                    title = strings.groupSettingsAllowMemberAddFriend,
                                    checked = allowMemberAddFriend == true,
                                    onToggle = { v -> patchSetting(apply = { this.allowMemberAddFriend = v }, onOk = { allowMemberAddFriend = v }) },
                                )
                                // allMuted（全员禁言）：统一走 groupUpdateSettings（单一路径）。
                                ManageRow.AllMuted -> SettingSwitchCell(
                                    title = strings.groupSettingsAllMuted,
                                    checked = allMuted == true,
                                    onToggle = { v -> patchSetting(apply = { this.allMuted = v }, onOk = { allMuted = v }) },
                                )
                            }
                        }
                    }
                }
            }


            // 通用设置：一张卡片，开关行之间有分隔线。裸 Cell 在分组背景上是
            // 一条条没有容器的白带，行与行之间只剩空白。
            item {
                val generalRows = buildList {
                    if (showMute) add(ChatSettingRow.Mute)
                    add(ChatSettingRow.Pin)
                }
                CellGroup(
                    items = generalRows,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                ) { row ->
                    when (row) {
                        ChatSettingRow.Mute -> Cell(
                            title = strings.chatSettingsMute,
                            trailing = {
                                Switch(
                                    checked = isMuted,
                                    onCheckedChange = { newValue ->
                                        scope.launch {
                                            onMuteChange(newValue).onSuccess { isMuted = newValue }
                                        }
                                    }
                                )
                            },
                        )
                        ChatSettingRow.Pin -> Cell(
                            title = strings.chatSettingsPin,
                            trailing = {
                                Switch(
                                    checked = isPinned,
                                    onCheckedChange = { newValue ->
                                        scope.launch {
                                            onPinChange(newValue).onSuccess { isPinned = newValue }
                                        }
                                    }
                                )
                            },
                        )
                    }
                }
            }

            // 群聊特有操作
            if (isGroup) {
                // 退出群聊：自己一张卡片，红字居中——平台设置页里危险操作的做法。
                // 群主不可直接退出（须先转让群主或解散群——后续 Phase B 实现），说明放在卡片下方。
                item {
                    val leaveEnabled = !isOwner
                    CellGroup(
                        items = listOf(Unit),
                        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    ) {
                        val leaveInteraction = remember { MutableInteractionSource() }
                        val leavePressed by leaveInteraction.collectIsPressedAsState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(pressedSurfaceColor(colors.surface, leavePressed))
                                .clickable(
                                    enabled = leaveEnabled,
                                    interactionSource = leaveInteraction,
                                    indication = null,
                                ) { showLeaveConfirmDialog = true }
                                .padding(Spacing.lg),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = strings.chatSettingsLeaveGroup,
                                style = Theme.typography.bodyMedium,
                                color = if (leaveEnabled) colors.destructive else colors.mutedForeground
                            )
                        }
                    }
                    if (isOwner) {
                        FieldDescription(
                            text = strings.groupOwnerCannotLeave,
                            modifier = Modifier.padding(horizontal = Spacing.lg * 2),
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
            actions = listOf(
                DialogAction(
                    text = strings.cancel,
                    role = DialogActionRole.CANCEL,
                    enabled = !(isLeaving),
                    onClick = { showLeaveConfirmDialog = false },
                ),
                DialogAction(
                    text = if (isLeaving) strings.loading else strings.confirm,
                    role = DialogActionRole.DESTRUCTIVE,
                    enabled = !(isLeaving),
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
                    },
                ),
            )
        )
    }

    // 加群方式选择走全局 ActionSheet 单例，需在页面根部挂 Host。
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

/** 通用设置卡片里的行，用来把开关行喂给 CellGroup。 */
private enum class ChatSettingRow { Mute, Pin }

/** 群设置卡片里的行。条件显隐先算成一个列表，再交给 CellGroup。 */
private enum class GroupRow { Name, QrCode, Members, Invite, Manage, Approval }

/** 群管理卡片里的行。 */
private enum class ManageRow { JoinPolicy, AllowSearch, MemberCanInvite, AllowMemberAddFriend, AllMuted }
