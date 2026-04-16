package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.*
import om.netonstream.privchat.sdk.dto.FriendEntry
import om.netonstream.privchat.sdk.dto.GroupEntry
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.models.displayName
import com.netonstream.privchat.ui.components.ChatAvatar
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.gearui.theme.Theme
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.primitives.GearLazyColumn
import com.gearui.foundation.typography.Typography
import com.gearui.foundation.AvatarSpecs
import com.gearui.primitives.Badge
import com.gearui.components.navbar.NavBar
import com.gearui.components.navbar.NavBarItem
import com.gearui.components.icon.Icons
import com.gearui.components.cell.Cell
import com.gearui.components.empty.EmptyState
import com.gearui.components.searchbar.SearchBar
import com.gearui.foundation.primitives.Icon
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 联系人页面
 *
 * @param onFriendClick 点击好友回调
 * @param onGroupClick 点击群组回调
 * @param onAddFriend 添加好友/搜索用户回调
 * @param onFriendRequestClick 点击好友申请回调
 * @param onMyGroupsClick 点击我的群组回调
 * @param onFriendSettings 点击好友设置回调
 * @param modifier Modifier
 */
@Composable
fun ContactPage(
    onFriendClick: (FriendEntry) -> Unit,
    onGroupClick: (GroupEntry) -> Unit = {},
    onAddFriend: () -> Unit = {},
    onFriendRequestClick: () -> Unit = {},
    onMyGroupsClick: () -> Unit = {},
    onFriendSettings: (FriendEntry) -> Unit = {},
    networkStatusBar: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    val friends by PrivChat.friends.collectAsState()
    val groups by PrivChat.groups.collectAsState()
    val friendRequests by PrivChat.friendRequests.collectAsState()
    val presences by PrivChat.presences.collectAsState()

    // 搜索关键词
    var searchQuery by remember { mutableStateOf("") }

    // 过滤后的好友列表
    val filteredFriends = remember(friends, searchQuery) {
        if (searchQuery.isBlank()) {
            friends
        } else {
            friends.filter { friend ->
                friend.displayName.contains(searchQuery, ignoreCase = true) ||
                        friend.username.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 顶部导航栏 - 右上角添加按钮
        NavBar(
            title = strings.contactTitle,
            rightItems = listOf(
                NavBarItem(icon = Icons.add, onClick = onAddFriend)
            ),
        )
        networkStatusBar?.invoke()

        // 搜索栏 - 方形居中
        SearchBar(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = strings.search,
            shape = com.gearui.components.searchbar.SearchBarShape.SQUARE,
            alignment = com.gearui.components.searchbar.SearchBarAlignment.CENTER,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // 联系人列表
        GearLazyColumn(modifier = Modifier.fillMaxSize()) {
            // 好友申请入口
            item {
                FriendRequestEntry(
                    requestCount = friendRequests.size,
                    onClick = onFriendRequestClick
                )
            }

            // 我的群组入口
            item {
                MyGroupsEntry(
                    groupCount = groups.size,
                    onClick = onMyGroupsClick
                )
            }

            // 好友列表标题
            if (filteredFriends.isNotEmpty()) {
                item {
                    SectionHeader(title = "${strings.contactFriends} (${filteredFriends.size})")
                }

                // 按首字母分组的好友列表
                val grouped = filteredFriends.groupBy {
                    it.displayName.firstOrNull()?.uppercaseChar() ?: '#'
                }

                grouped.entries.sortedBy { it.key }.forEach { entry ->
                    val letter = entry.key
                    val friendsInGroup = entry.value
                    // 字母标签
                    item {
                        LetterHeader(letter = letter.toString())
                    }

                    // 该字母下的好友
                    items(friendsInGroup.size) { index ->
                        val friend = friendsInGroup[index]
                        FriendItem(
                            friend = friend,
                            isOnline = presences[friend.userId]?.isOnline == true,
                            onClick = { onFriendClick(friend) },
                        )
                    }
                }
            } else {
                // 空状态
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
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
}

/**
 * 好友申请入口
 */
@Composable
private fun FriendRequestEntry(
    requestCount: Int,
    onClick: () -> Unit
) {
    val strings = PrivChatI18n.strings

    Cell(
        onClick = onClick,
        compact = true,
        leading = {
            ContactEntryIcon(Icons.person_add)
        },
        title = strings.contactFriendRequest,
        arrow = true,
        trailing = if (requestCount > 0) {
            { Badge(count = requestCount) }
        } else null
    )
}

/**
 * 我的群组入口
 */
@Composable
private fun MyGroupsEntry(
    groupCount: Int,
    onClick: () -> Unit
) {
    val strings = PrivChatI18n.strings

    Cell(
        onClick = onClick,
        compact = true,
        leading = {
            ContactEntryIcon(Icons.groups)
        },
        title = strings.contactMyGroups,
        description = "$groupCount ${strings.contactGroups}",
        arrow = true,
    )
}

@Composable
private fun ContactEntryIcon(icon: String) {
    val colors = Theme.colors

    Box(
        modifier = Modifier
            .size(AvatarSpecs.Size.small)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.primaryLight),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            name = icon,
            size = 18.dp,
            tint = colors.primary
        )
    }
}

/**
 * 分组标题
 */
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
            color = colors.textPrimary,
        )
    }
}

/**
 * 字母标题
 */
@Composable
private fun LetterHeader(letter: String) {
    val colors = Theme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(
            text = letter,
            style = Typography.Label,
            color = colors.textSecondary,
        )
    }
}

/**
 * 好友项
 */
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
                size = AvatarSpecs.Size.small,
                isOnline = isOnline,
            )
        },
        title = friend.displayName,
        arrow = true,
    )
}
