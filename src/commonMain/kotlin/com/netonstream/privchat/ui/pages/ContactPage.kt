package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.*
import com.netonstream.privchat.sdk.dto.FriendEntry
import com.netonstream.privchat.sdk.dto.GroupEntry
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.models.displayName
import com.netonstream.privchat.ui.components.ChatAvatar
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.gearui.theme.Theme
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.primitives.GearLazyColumn
import com.gearui.foundation.typography.Typography
import com.gearui.foundation.avatar.AvatarSizeTokens
import com.gearui.primitives.Badge
import com.gearui.components.navbar.NavBar
import com.gearui.components.icon.Icons
import com.gearui.components.cell.Cell
import com.gearui.components.empty.EmptyState
import com.gearui.components.searchbar.SearchBar
import com.gearui.components.tabs.Tab
import com.gearui.components.tabs.Tabs
import com.gearui.components.tabs.TabsOutlineType
import com.gearui.components.tabs.TabsSize
import com.gearui.foundation.primitives.Icon
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.unit.dp

private const val CONTACT_TAB_FRIENDS = "friends"
private const val CONTACT_TAB_GROUPS = "groups"

/**
 * 联系人页面。
 *
 * 顶部 Tabs 切换【好友 / 群组】两个面板。
 * - **好友**：保留"好友申请"入口（带 badge），下方是按首字母分组的好友列表 +
 *   搜索过滤。
 * - **群组**：列出本人参与的所有群（按 [PrivChat.groups]）+ 搜索过滤。点击进
 *   入群会话。
 *
 * 旧的"我的群组"二级页（GroupListPage）保留但不再是默认入口——本页 group tab
 * 已经把它的列表平铺出来。`onMyGroupsClick` 回调保留兼容老调用方，建议宿主
 * 删除关联导航。
 *
 * @param onFriendClick 点击好友回调
 * @param onGroupClick 点击群组回调
 * @param onFriendRequestClick 点击好友申请入口回调
 * @param onMyGroupsClick （deprecated）保留兼容；当前 tab 已平铺群组列表
 */
@Composable
fun ContactPage(
    onFriendClick: (FriendEntry) -> Unit,
    onGroupClick: (GroupEntry) -> Unit = {},
    onAddFriend: () -> Unit = {},
    onFriendRequestClick: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onMyGroupsClick: () -> Unit = {},
    onFriendSettings: (FriendEntry) -> Unit = {},
    networkStatusBar: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    val friends by PrivChat.friends.collectAsState()
    val groups by PrivChat.groups.collectAsState()
    // F-sync.3: 联系人页"好友申请"入口的红点 count 切到本地投影。
    // PrivChatSDKManager.loadReceivedFriendRequests 默认 statuses=[0]，所以
    // receivedFriendRequests 天然只含 pending；防御性再过滤一次 status==0
    // 防御 server 误发或本地数据脏。
    val receivedFriendRequests by PrivChat.receivedFriendRequests.collectAsState()
    val pendingReceivedCount = receivedFriendRequests.count { it.status.toInt() == 0 }
    val presences by PrivChat.presences.collectAsState()

    var selectedTab by remember { mutableStateOf(CONTACT_TAB_FRIENDS) }
    var searchQuery by remember { mutableStateOf("") }

    // Tab label 带 count，与好友申请页一致
    val friendsTabLabel = "${strings.contactFriends} ${friends.size}"
    val groupsTabLabel = "${strings.contactGroups} ${groups.size}"

    Column(modifier = modifier.fillMaxSize()) {
        NavBar(title = strings.contactTitle)
        networkStatusBar?.invoke()

        Tabs(
            items = listOf(
                Tab(id = CONTACT_TAB_FRIENDS, label = friendsTabLabel),
                Tab(id = CONTACT_TAB_GROUPS, label = groupsTabLabel),
            ),
            selectedId = selectedTab,
            onSelect = {
                selectedTab = it
                searchQuery = ""
            },
            size = TabsSize.MEDIUM,
            outlineType = TabsOutlineType.UNDERLINE,
            showDivider = true,
        )

        SearchBar(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = strings.search,
            shape = com.gearui.components.searchbar.SearchBarShape.SQUARE,
            alignment = com.gearui.components.searchbar.SearchBarAlignment.CENTER,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        when (selectedTab) {
            CONTACT_TAB_FRIENDS -> FriendsTabContent(
                friends = friends,
                presences = presences,
                friendRequestCount = pendingReceivedCount,
                searchQuery = searchQuery,
                onFriendClick = onFriendClick,
                onFriendRequestClick = onFriendRequestClick,
            )
            CONTACT_TAB_GROUPS -> GroupsTabContent(
                groups = groups,
                searchQuery = searchQuery,
                onGroupClick = onGroupClick,
            )
        }
    }
}

@Composable
private fun FriendsTabContent(
    friends: List<FriendEntry>,
    presences: Map<ULong, com.netonstream.privchat.sdk.dto.PresenceEntry>,
    friendRequestCount: Int,
    searchQuery: String,
    onFriendClick: (FriendEntry) -> Unit,
    onFriendRequestClick: () -> Unit,
) {
    val strings = PrivChatI18n.strings

    val filtered = if (searchQuery.isBlank()) friends else friends.filter { f ->
        f.displayName.contains(searchQuery, ignoreCase = true) ||
            f.username.contains(searchQuery, ignoreCase = true)
    }

    GearLazyColumn(modifier = Modifier.fillMaxSize()) {
        // 好友申请入口（始终顶置；搜索时也保留，便于直接进入）
        item {
            FriendRequestEntry(
                requestCount = friendRequestCount,
                onClick = onFriendRequestClick,
            )
        }

        if (filtered.isNotEmpty()) {
            item {
                SectionHeader(title = "${strings.contactFriends} (${filtered.size})")
            }

            val grouped = filtered.groupBy {
                it.displayName.firstOrNull()?.uppercaseChar() ?: '#'
            }
            grouped.entries.sortedBy { it.key }.forEach { entry ->
                val letter = entry.key
                val list = entry.value
                item { LetterHeader(letter = letter.toString()) }
                items(list.size) { idx ->
                    val friend = list[idx]
                    FriendItem(
                        friend = friend,
                        isOnline = presences[friend.userId]?.isOnline == true,
                        onClick = { onFriendClick(friend) },
                    )
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        message = strings.contactEmpty,
                        description = strings.contactAddFriend,
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupsTabContent(
    groups: List<GroupEntry>,
    searchQuery: String,
    onGroupClick: (GroupEntry) -> Unit,
) {
    val strings = PrivChatI18n.strings

    val filtered = if (searchQuery.isBlank()) groups else groups.filter { g ->
        g.displayName.contains(searchQuery, ignoreCase = true)
    }

    if (filtered.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            EmptyState(message = strings.contactGroupsEmpty)
        }
        return
    }

    GearLazyColumn(modifier = Modifier.fillMaxSize()) {
        items(filtered.size) { idx ->
            val group = filtered[idx]
            Cell(
                onClick = { onGroupClick(group) },
                compact = true,
                leading = {
                    ChatAvatar(
                        url = group.avatar.ifBlank { null },
                        name = group.displayName,
                        size = AvatarSizeTokens.Small.size,
                    )
                },
                title = group.displayName,
                arrow = true,
            )
        }
    }
}

@Composable
private fun FriendRequestEntry(
    requestCount: Int,
    onClick: () -> Unit,
) {
    val strings = PrivChatI18n.strings

    Cell(
        onClick = onClick,
        compact = true,
        leading = { ContactEntryIcon(Icons.person_add) },
        title = strings.contactFriendRequest,
        arrow = true,
        trailing = if (requestCount > 0) {
            { Badge(count = requestCount) }
        } else null,
    )
}

@Composable
private fun ContactEntryIcon(icon: String) {
    val colors = Theme.colors
    Box(
        modifier = Modifier
            .size(AvatarSizeTokens.Small.size)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(name = icon, size = 18.dp, tint = colors.primaryForeground)
    }
}

@Composable
private fun SectionHeader(title: String) {
    val colors = Theme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = title,
            style = Typography.TitleSmall,
            color = colors.foreground,
        )
    }
}

@Composable
private fun LetterHeader(letter: String) {
    val colors = Theme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.muted)
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(
            text = letter,
            style = Typography.Label,
            color = colors.mutedForeground,
        )
    }
}

@Composable
private fun FriendItem(
    friend: FriendEntry,
    isOnline: Boolean,
    onClick: () -> Unit,
) {
    Cell(
        onClick = onClick,
        compact = true,
        leading = {
            ChatAvatar(
                url = friend.avatarUrl,
                name = friend.displayName,
                size = AvatarSizeTokens.Small.size,
                isOnline = isOnline,
                userId = friend.userId.toLong(),
            )
        },
        title = friend.displayName,
        arrow = true,
    )
}
