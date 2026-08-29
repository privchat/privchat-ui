package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.*
import com.netonstream.privchat.sdk.dto.GroupMemberEntry
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.components.ChatAvatar
import com.netonstream.privchat.ui.models.displayName
import com.netonstream.privchat.ui.models.isAdmin
import com.netonstream.privchat.ui.models.isOwner
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.typography.Typography
import com.tencent.kuikly.compose.ui.graphics.Color
import com.gearui.foundation.avatar.AvatarSizeTokens
import com.gearui.foundation.primitives.GearLazyColumn
import com.gearui.components.cell.Cell
import com.gearui.components.empty.EmptyState
import com.gearui.components.navbar.NavBar
import com.gearui.components.navbar.NavBarItem
import com.gearui.components.icon.Icons
import com.gearui.components.actionsheet.ActionSheet
import com.gearui.components.actionsheet.ActionSheetItem
import com.gearui.components.toast.Toast
import com.gearui.components.swipecell.SwipeCell
import com.gearui.components.swipecell.SwipeCellAction
import com.gearui.components.swipecell.SwipeCellActionTheme
import com.gearui.components.swipecell.rememberSwipeCellState
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.ui.Alignment
import com.gearui.components.dialog.Dialog
import com.gearui.components.dialog.DialogContent
import com.gearui.components.button.Button
import com.gearui.components.button.ButtonSize
import com.gearui.components.button.ButtonTheme
import com.gearui.components.button.ButtonType
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GroupMembersPage(
    groupName: String,
    members: List<GroupMemberEntry>,
    onBack: () -> Unit,
    onInviteClick: () -> Unit = {},
    onMemberClick: (GroupMemberEntry) -> Unit = {},
    onRemoveMember: suspend (GroupMemberEntry) -> Result<Unit> = { Result.success(Unit) },
    /** 设置/取消管理员(仅群主可见;role = "admin" | "member")。 */
    onSetRole: suspend (GroupMemberEntry, String) -> Result<Unit> = { _, _ -> Result.success(Unit) },
    /** 转让群主(仅群主可见;二次确认后调用)。 */
    onTransferOwner: suspend (GroupMemberEntry) -> Result<Unit> = { Result.success(Unit) },
    /** 当前用户是否为群主/管理员；为 true 时显示「禁言/解除禁言」操作。 */
    canManage: Boolean = false,
    /** 当前用户是否为群主;为 true 时额外显示「设/取消管理员」「转让群主」。 */
    isOwner: Boolean = false,
    onError: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    // 系统类型账号(user_type==1)已在上游 loadGroupMembers 按 user_type 滤除,这里只排序。
    val sorted = members
        .sortedWith(compareByDescending<GroupMemberEntry> { it.role }.thenBy { it.displayName })
    val scope = rememberCoroutineScope()

    // 禁言：选时长 → groupMuteMember(groupId, userId, seconds)。群聊 channelId == groupId。
    // 时长预设（秒）：10 分钟 / 1 小时 / 1 天 / 永久（null=永久）。
    fun showMuteDurationSheet(member: GroupMemberEntry) {
        val presets: List<Pair<String, ULong?>> = listOf(
            strings.groupMuteDuration10m to 600uL,
            strings.groupMuteDuration1h to 3600uL,
            strings.groupMuteDuration1d to 86_400uL,
            strings.groupMuteDurationForever to null,
        )
        ActionSheet.showList(
            description = strings.groupMuteDurationTitle,
            items = presets.map { ActionSheetItem(label = it.first) },
            onSelected = { _, index ->
                val seconds = presets.getOrNull(index)?.second
                scope.launch {
                    withContext(Dispatchers.Default) {
                        PrivChat.client.groupMuteMember(member.channelId, member.userId, seconds)
                    }.onSuccess { Toast.success(strings.groupMuteSuccess) }
                        .onFailure { onError?.invoke(com.netonstream.privchat.ui.error.UserFacingError.message(it, strings.networkError)) }
                }
            },
        )
    }

    fun unmuteMember(member: GroupMemberEntry) {
        scope.launch {
            withContext(Dispatchers.Default) {
                PrivChat.client.groupUnmuteMember(member.channelId, member.userId)
            }.onSuccess { Toast.success(strings.groupUnmuteSuccess) }
                .onFailure { onError?.invoke(com.netonstream.privchat.ui.error.UserFacingError.message(it, strings.networkError)) }
        }
    }

    // 群主管理动作(设/取消管理员、转让群主)——收进一个 ActionSheet,避免
    // 滑动操作条过宽;服务端 group/role/* 仅群主可调,UI gate 只是显隐。
    var transferTarget by remember { mutableStateOf<GroupMemberEntry?>(null) }
    fun showOwnerManageSheet(member: GroupMemberEntry) {
        val roleLabel =
            if (member.isAdmin) strings.groupRoleRemoveAdmin else strings.groupRoleSetAdmin
        ActionSheet.showList(
            items = listOf(
                ActionSheetItem(label = roleLabel),
                ActionSheetItem(label = strings.groupTransferOwner),
            ),
            onSelected = { _, index ->
                when (index) {
                    0 -> scope.launch {
                        val next = if (member.isAdmin) "member" else "admin"
                        onSetRole(member, next).onFailure {
                            onError?.invoke(com.netonstream.privchat.ui.error.UserFacingError.message(it, strings.networkError))
                        }
                    }
                    1 -> transferTarget = member
                }
            },
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        NavBar(
            title = "$groupName (${sorted.size})",
            useDefaultBack = true,
            onBackClick = onBack,
            rightItems = listOf(
                NavBarItem(icon = Icons.add, onClick = onInviteClick),
            ),
        )

        if (sorted.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(message = strings.noData)
            }
        } else {
            GearLazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sorted.size) { index ->
                    val member = sorted[index]
                    val swipeState = rememberSwipeCellState()
                    // 管理员/群主可对「普通成员」禁言；不对群主/管理员显示禁言操作。
                    val canMuteThis = canManage && !member.isOwner && !member.isAdmin
                    // 移除同样只有群主/管理员可见（服务端 kick 已有 RBAC 强制，这里是 UI gate），
                    // 且群主不可被移除。普通成员滑动不出现任何操作。
                    val canRemoveThis = canManage && !member.isOwner
                    val rightActions = buildList {
                        if (canMuteThis) {
                            add(
                                SwipeCellAction(
                                    label = strings.groupMemberMute,
                                    theme = SwipeCellActionTheme.WARNING,
                                    onClick = { showMuteDurationSheet(member) },
                                )
                            )
                            add(
                                SwipeCellAction(
                                    label = strings.groupMemberUnmute,
                                    theme = SwipeCellActionTheme.PRIMARY,
                                    onClick = { unmuteMember(member) },
                                )
                            )
                        }
                        if (isOwner && !member.isOwner) {
                            add(
                                SwipeCellAction(
                                    label = strings.chatSettingsGroupManage,
                                    theme = SwipeCellActionTheme.PRIMARY,
                                    onClick = { showOwnerManageSheet(member) },
                                )
                            )
                        }
                        if (canRemoveThis) {
                            add(
                                SwipeCellAction(
                                    label = strings.groupMemberRemove,
                                    theme = SwipeCellActionTheme.DANGER,
                                    onClick = {
                                        scope.launch {
                                            onRemoveMember(member).onFailure {
                                                onError?.invoke(com.netonstream.privchat.ui.error.UserFacingError.message(it, strings.networkError))
                                            }
                                        }
                                    },
                                )
                            )
                        }
                    }
                    SwipeCell(
                        state = swipeState,
                        rightActions = rightActions,
                    ) {
                        Cell(
                            title = member.displayName,
                            // 用户 ID 是底层协议标识，不在任何 UI 展示；副标题只显示角色
                            // (走语言包,不再用硬编码 roleName)。
                            description = when {
                                member.isOwner -> strings.groupOwner
                                member.isAdmin -> strings.groupAdmin
                                else -> strings.groupMember
                            },
                            onClick = { onMemberClick(member) },
                            leading = {
                                ChatAvatar(
                                    url = member.avatar,
                                    name = member.displayName,
                                    size = AvatarSizeTokens.Small.size,
                                    seed = "u:${member.userId}",
                                    userId = member.userId.toLong(),
                                )
                            },
                            // 三端统一角色标签:群主橙字、管理红字(web/h5 同色)。
                            trailing = when {
                                member.isOwner -> ({
                                    Text(
                                        text = strings.groupOwner,
                                        style = Typography.Label,
                                        color = Color(0xFFF97316),
                                    )
                                })
                                member.isAdmin -> ({
                                    Text(
                                        text = strings.groupAdmin,
                                        style = Typography.Label,
                                        color = Color(0xFFEF4444),
                                    )
                                })
                                else -> null
                            },
                        )
                    }
                }
            }
        }
    }

    // 禁言时长选择走全局 ActionSheet 单例，需在页面根部挂 Host。

    // 转让群主二次确认(不可逆操作)。
    val pendingTransfer = transferTarget
    Dialog.Host(
        visible = pendingTransfer != null,
        onDismiss = { transferTarget = null },
    ) {
        DialogContent(
            title = strings.groupTransferOwner,
            message = strings.groupTransferOwnerConfirm,
            actions = {
                Button(
                    text = strings.cancel,
                    type = ButtonType.TEXT,
                    theme = ButtonTheme.DEFAULT,
                    size = ButtonSize.SMALL,
                    onClick = { transferTarget = null },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    text = strings.confirm,
                    type = ButtonType.TEXT,
                    theme = ButtonTheme.DANGER,
                    size = ButtonSize.SMALL,
                    onClick = {
                        val target = pendingTransfer ?: return@Button
                        transferTarget = null
                        scope.launch {
                            onTransferOwner(target).onFailure {
                                onError?.invoke(com.netonstream.privchat.ui.error.UserFacingError.message(it, strings.networkError))
                            }
                        }
                    },
                )
            },
        )
    }
}
