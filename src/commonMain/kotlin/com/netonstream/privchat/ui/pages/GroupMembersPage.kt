package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.*
import com.netonstream.privchat.sdk.dto.GroupMemberEntry
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.components.ChatAvatar
import com.netonstream.privchat.ui.models.displayName
import com.netonstream.privchat.ui.models.isAdmin
import com.netonstream.privchat.ui.models.isOwner
import com.netonstream.privchat.ui.models.roleName
import com.netonstream.privchat.ui.i18n.PrivChatI18n
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
    /** 当前用户是否为群主/管理员；为 true 时显示「禁言/解除禁言」操作。 */
    canManage: Boolean = false,
    onError: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    val sorted = members.sortedWith(compareByDescending<GroupMemberEntry> { it.role }.thenBy { it.displayName })
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
                        .onFailure { onError?.invoke(it.message ?: strings.networkError) }
                }
            },
        )
    }

    fun unmuteMember(member: GroupMemberEntry) {
        scope.launch {
            withContext(Dispatchers.Default) {
                PrivChat.client.groupUnmuteMember(member.channelId, member.userId)
            }.onSuccess { Toast.success(strings.groupUnmuteSuccess) }
                .onFailure { onError?.invoke(it.message ?: strings.networkError) }
        }
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
                        add(
                            SwipeCellAction(
                                label = "移除",
                                theme = SwipeCellActionTheme.DANGER,
                                onClick = {
                                    scope.launch {
                                        onRemoveMember(member).onFailure {
                                            onError?.invoke(it.message ?: strings.networkError)
                                        }
                                    }
                                },
                            )
                        )
                    }
                    SwipeCell(
                        state = swipeState,
                        rightActions = rightActions,
                    ) {
                        Cell(
                            title = member.displayName,
                            description = "${member.roleName} · ${member.userId}",
                            onClick = { onMemberClick(member) },
                            leading = {
                                ChatAvatar(
                                    url = member.avatar,
                                    name = member.displayName,
                                    size = AvatarSizeTokens.Small.size,
                                    seed = "u:${member.userId}",
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    // 禁言时长选择走全局 ActionSheet 单例，需在页面根部挂 Host。
    ActionSheet.Host()
}
