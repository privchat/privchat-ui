package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.*
import com.netonstream.privchat.sdk.dto.ChannelListEntry
import com.netonstream.privchat.sdk.dto.FriendEntry
import com.netonstream.privchat.sdk.dto.GroupEntry
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.components.ChatAvatar
import com.netonstream.privchat.ui.models.displayName
import com.gearui.theme.Theme
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.primitives.GearLazyColumn
import com.gearui.foundation.avatar.AvatarSizeTokens
import com.gearui.runtime.LocalRuntimeEnvironment
import com.gearui.runtime.LocalRuntimeFlags
import com.gearui.components.navbar.NavBar
import com.gearui.components.cell.Cell
import com.gearui.components.empty.EmptyState
import com.gearui.components.searchbar.SearchBar
import com.gearui.components.button.Button
import com.gearui.components.button.ButtonTheme
import com.gearui.components.button.ButtonSize
import com.gearui.components.input.Input
import com.gearui.components.input.InputSize
import com.gearui.components.toast.Toast
import com.gearui.primitives.HorizontalSpacer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.ui.Alignment
import com.netonstream.privchat.ui.search.PeopleSearch
import com.netonstream.privchat.ui.search.SearchField
import com.netonstream.privchat.ui.search.FieldHit
import com.netonstream.privchat.ui.components.HighlightedText
import com.netonstream.privchat.ui.components.SelectionDot
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.netonstream.privchat.ui.i18n.withArgs
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.unit.dp

/** 转发目标上限（产品定）。 */
private const val FORWARD_MAX_TARGETS = 10

/** 留言最大长度。 */
private const val FORWARD_NOTE_MAX = 200

/**
 * 转发目标的统一表示，调用方在 onSend 回调中负责解析到实际 channel。
 *
 * DirectMessage 只暴露 peerUserId 是因为新好友可能尚未创建 DM channel，
 * 需要调用方在发送时调用 `getOrCreateDirectChannel` 懒创建。
 */
sealed class ForwardTarget {
    abstract val key: String
    abstract val displayName: String
    abstract val avatarUrl: String?

    data class DirectMessage(
        val peerUserId: ULong,
        override val displayName: String,
        override val avatarUrl: String?,
    ) : ForwardTarget() {
        override val key: String = "dm:$peerUserId"
    }

    data class Group(
        val groupId: ULong,
        override val displayName: String,
        override val avatarUrl: String?,
    ) : ForwardTarget() {
        override val key: String = "grp:$groupId"
    }
}

private fun ChannelListEntry.toForwardTarget(): ForwardTarget? = when {
    isDm -> peerUserId?.let { ForwardTarget.DirectMessage(it, name, avatarUrl) }
    // 群聊的 channelId 约定 == groupId。
    channelType == 2 -> ForwardTarget.Group(channelId, name, avatarUrl)
    else -> null
}

private fun FriendEntry.toForwardTarget(): ForwardTarget =
    ForwardTarget.DirectMessage(userId, displayName, avatarUrl)

private fun GroupEntry.toForwardTarget(): ForwardTarget =
    ForwardTarget.Group(groupId, name?.takeIf { it.isNotBlank() } ?: groupId.toString(), avatar.takeIf { it.isNotBlank() })

/**
 * 转发目标选择页：最近聊天置顶 + 好友 + 群组，支持搜索过滤、多选上限、可选留言。
 *
 * 选完后点「发送」触发 [onSend]，调用方拿 targets 列表循环调用
 * `PrivchatClient.forwardMessage`，note 非空则再追加一条文本消息。
 */
@Composable
fun ForwardPickerPage(
    onBack: () -> Unit,
    onSend: (targets: List<ForwardTarget>, note: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    val colors = Theme.colors
    val runtimeFlags = LocalRuntimeFlags.current
    val runtimeEnvironment = LocalRuntimeEnvironment.current
    val safeAreaBottom = if (runtimeFlags.unifiedSafeAreaPipeline) {
        runtimeEnvironment.safeArea.bottom
    } else {
        0.dp
    }
    val channels by PrivChat.channels.collectAsState()
    val friends by PrivChat.friends.collectAsState()
    val groups by PrivChat.groups.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    // key -> ForwardTarget
    val selected = remember { mutableStateMapOf<String, ForwardTarget>() }

    // 最近：只取能解析成 DM/Group 的 channel，按 lastTs 降序。
    val recentTargets = remember(channels) {
        channels
            .sortedByDescending { it.lastTs }
            .mapNotNull { it.toForwardTarget() }
    }
    val recentKeys = remember(recentTargets) { recentTargets.map { it.key }.toSet() }

    // 好友：去掉已经在「最近」里的 DM。
    val friendTargets = remember(friends, recentKeys) {
        friends.map { it.toForwardTarget() }.filter { it.key !in recentKeys }
    }
    // 群组：去掉已经在「最近」里的群，并过滤已解散。
    val groupTargets = remember(groups, recentKeys) {
        groups
            .filter { !it.isDismissed }
            .map { it.toForwardTarget() }
            .filter { it.key !in recentKeys }
    }

    // 搜索走 PeopleSearch，和联系人页、选人面板、全局搜索同一套规则：
    // 原来是 `contains`，搜不了拼音，也说不清为什么命中。转发目标只有一个可搜字段
    // （会话/好友/群的显示名），备注和成员名不在这份数据里。
    fun search(targets: List<ForwardTarget>): List<Pair<ForwardTarget, FieldHit?>> =
        PeopleSearch.search(
            items = targets,
            query = searchQuery.trim(),
            fieldsOf = { listOf(SearchField.DisplayName to it.displayName) },
            nameOf = { it.displayName },
            // 目标没有数值 id（DM 和群共用一个 key 空间），用 key 的哈希收尾即可：
            // 只要同分时定序稳定，列表就不会每次刷新互换位置。
            tieBreaker = { it.key.hashCode().toULong() },
        )

    val filteredRecent = search(recentTargets)
    val filteredFriends = search(friendTargets)
    val filteredGroups = search(groupTargets)

    fun toggle(target: ForwardTarget) {
        if (selected.containsKey(target.key)) {
            selected.remove(target.key)
        } else {
            if (selected.size >= FORWARD_MAX_TARGETS) {
                Toast.show(strings.forwardMaxReached.withArgs(FORWARD_MAX_TARGETS))
                return
            }
            selected[target.key] = target
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        NavBar(
            title = strings.forwardTitle,
            useDefaultBack = true,
            onBackClick = onBack,
        )

        SearchBar(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = strings.forwardSearchPlaceholder,
            shape = com.gearui.components.searchbar.SearchBarShape.SQUARE,
            alignment = com.gearui.components.searchbar.SearchBarAlignment.CENTER,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Box(modifier = Modifier.weight(1f)) {
            val hasAnyResult =
                filteredRecent.isNotEmpty() || filteredFriends.isNotEmpty() || filteredGroups.isNotEmpty()
            if (!hasAnyResult) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        message = if (searchQuery.isBlank()) strings.forwardEmpty
                        else strings.forwardNoMatch,
                    )
                }
            } else {
                GearLazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (filteredRecent.isNotEmpty()) {
                        item { ForwardSectionHeader(strings.forwardSectionRecent) }
                        items(filteredRecent.size) { i ->
                            val (target, hit) = filteredRecent[i]
                            ForwardTargetRow(
                                target = target,
                                hit = hit,
                                checked = selected.containsKey(target.key),
                                onToggle = { toggle(target) },
                            )
                        }
                    }
                    if (filteredFriends.isNotEmpty()) {
                        item { ForwardSectionHeader(strings.forwardSectionFriends) }
                        items(filteredFriends.size) { i ->
                            val (target, hit) = filteredFriends[i]
                            ForwardTargetRow(
                                target = target,
                                hit = hit,
                                checked = selected.containsKey(target.key),
                                onToggle = { toggle(target) },
                            )
                        }
                    }
                    if (filteredGroups.isNotEmpty()) {
                        item { ForwardSectionHeader(strings.forwardSectionGroups) }
                        items(filteredGroups.size) { i ->
                            val (target, hit) = filteredGroups[i]
                            ForwardTargetRow(
                                target = target,
                                hit = hit,
                                checked = selected.containsKey(target.key),
                                onToggle = { toggle(target) },
                            )
                        }
                    }
                }
            }
        }

        // 底部：留言 + 发送按钮
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Input(
                    value = note,
                    onValueChange = { if (it.length <= FORWARD_NOTE_MAX) note = it },
                    placeholder = strings.forwardCommentPlaceholder,
                    size = InputSize.MEDIUM,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = strings.forwardSelectedCount.withArgs(selected.size, FORWARD_MAX_TARGETS),
                        style = Theme.typography.label,
                        color = colors.mutedForeground,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        text = strings.forwardSend,
                        theme = ButtonTheme.PRIMARY,
                        size = ButtonSize.MEDIUM,
                        disabled = selected.isEmpty(),
                        onClick = {
                            onSend(selected.values.toList(), note.takeIf { it.isNotBlank() })
                        },
                    )
                }
            }
            // 底部安全区占位（home indicator 等），与底栏同色，由 GearUI runtime 提供
            Spacer(modifier = Modifier.height(safeAreaBottom))
        }
    }
}

@Composable
private fun ForwardSectionHeader(title: String) {
    val colors = Theme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = title,
            style = Theme.typography.titleSmall,
            color = colors.foreground,
        )
    }
}

@Composable
private fun ForwardTargetRow(
    target: ForwardTarget,
    hit: FieldHit?,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Cell(
        onClick = onToggle,
        compact = true,
        // 选择标记在左侧、整行可点，与创建群聊、邀请进群一致；行尾的方形复选框会让
        // 视线在名字和行尾之间来回跑。
        leading = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SelectionDot(selected = checked)
                HorizontalSpacer(10.dp)
                ChatAvatar(
                    url = target.avatarUrl,
                    name = target.displayName,
                    size = AvatarSizeTokens.Small.size,
                    userId = (target as? ForwardTarget.DirectMessage)?.peerUserId?.toLong(),
                )
            }
        },
        title = target.displayName,
        titleContent = {
            HighlightedText(
                text = target.displayName,
                match = hit?.match,
                style = Theme.typography.bodyLarge,
            )
        },
    )
}
