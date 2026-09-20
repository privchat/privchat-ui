package com.netonstream.privchat.ui.pages

import com.gearui.foundation.list.CellDefaults
import com.gearui.primitives.Divider
import com.gearui.foundation.layout.Spacing
import com.gearui.components.cellgroup.CellGroup
import com.gearui.primitives.SectionHeader
import androidx.compose.runtime.*
import com.netonstream.privchat.sdk.dto.FriendEntry
import com.netonstream.privchat.sdk.dto.GroupEntry
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.models.displayName
import com.netonstream.privchat.ui.components.ChatAvatar
import com.netonstream.privchat.ui.i18n.PinyinIndex
import kotlinx.coroutines.launch
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import com.netonstream.privchat.ui.components.IndexBar
import com.netonstream.privchat.ui.search.SearchField
import com.netonstream.privchat.ui.search.PeopleSearch
import com.netonstream.privchat.ui.search.FieldHit
import com.netonstream.privchat.ui.components.HighlightedText
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.gearui.theme.Theme
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.primitives.GearLazyColumn
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
    /** 顶栏放大镜 → 全局搜索页（与会话页同一动作，spec §7.1）。 */
    onGlobalSearch: () -> Unit = {},
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
        NavBar(
            title = strings.contactTitle,
            rightItems = listOf(
                com.gearui.components.navbar.NavBarItem(
                    icon = com.gearui.components.icon.Icons.magnifying_glass,
                    onClick = onGlobalSearch,
                ),
                com.gearui.components.navbar.NavBarItem(
                    icon = com.gearui.components.icon.Icons.user_plus,
                    onClick = onAddFriend,
                ),
            ),
        )
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

    // 过滤 + 按首字母分组排序，一次算好缓存住。
    //
    // 这三步过去裸写在组合体和 GearLazyColumn 的 scope 里，于是**每次重组都全量重算**一遍
    // filter + groupBy + sortedBy。而 presences 是 online 小绿点的来源、变化频繁，每来一次
    // presence 推送就重算整张好友表——好友一多，切到联系人页的卡顿就是从这里长出来的。
    // 会话页那边（ConversationPage 的 filteredChannels）一直是 remember 的，这里是漏了。
    // 搜索走 PeopleSearch（拼音 + 多字段 + 命中信息），与 @ 选人面板同一套规则。
    // 过去这里是 displayName/username 的 contains，搜不了拼音、也漏了备注。
    val hits = remember(friends, searchQuery) {
        PeopleSearch.search(
            items = friends,
            query = searchQuery,
            fieldsOf = { PeopleSearch.fieldsOf(it) },
            nameOf = { it.displayName },
            tieBreaker = { it.userId },
        )
    }
    val hitByUser = remember(hits) { hits.mapNotNull { (f, h) -> h?.let { f.userId to it } }.toMap() }

    val sections = remember(hits, searchQuery) {
        val filtered = hits.map { it.first }
        // 分组走 PinyinIndex：中文按拼音首字母归到 A–Z，符号/数字/emoji 落 `#` 并排在最后。
        // 过去是按"显示名首字符"分，于是每个中文名各自成一组，索引根本不是字母表。
        // @ 选人面板用的是同一个实现，两处的分组口径必须一致。
        // 有搜索词时按相关性给一整段，不分字母组——字母分组会把最准的结果压到下面。
        filtered to if (searchQuery.isBlank()) {
            PinyinIndex.group(filtered) { it.displayName }
        } else {
            listOf(null to filtered)
        }
    }
    val filtered = sections.first

    // 索引条要滚到某个字母，就得知道那个分组的首行在列表里的第几个 item。
    // 前面固定有两项：好友申请入口、以及「好友 (n)」这个分组标题。
    val sectionStarts = remember(sections) {
        // 一个字母段 = 列表里的一个 item（整张 CellGroup 卡片）。
        sections.second.mapIndexed { index, (letter, _) -> letter to LEADING_ROWS + index }
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
    GearLazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
        // 好友申请入口（始终顶置；搜索时也保留，便于直接进入）。
        // 它是一个动作，不是通讯录里的一个人——所以给它自己的卡片，和下面的名单分开。
        item {
            CellGroup(
                items = listOf(Unit),
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            ) {
                FriendRequestEntry(
                    requestCount = friendRequestCount,
                    onClick = onFriendRequestClick,
                )
            }
        }

        if (filtered.isNotEmpty()) {
            // 「好友 (n)」这个标题去掉了：数量 Tab 上已经写着（"好友 1"），
            // 而它紧挨着字母头，两个标题叠在一起反而看不出层次。
            // 每个字母段是一张卡片，字母就是卡片标题。裸 Cell 自己不画底色，
            // 之前整列人名是直接浮在页面灰底上的；CellGroup 把底色、分隔线、
            // 圆角和按压反馈一并管起来，和上面的申请卡片是同一种东西。
            sections.second.forEach { (letter, list) ->
                item {
                    CellGroup(
                        items = list,
                        title = letter?.toString(),
                        modifier = Modifier.padding(
                            horizontal = Spacing.lg,
                            vertical = Spacing.sm,
                        ),
                        separatorInset = FRIEND_SEPARATOR_INSET,
                    ) { friend ->
                        FriendItem(
                            friend = friend,
                            hit = hitByUser[friend.userId],
                            isOnline = presences[friend.userId]?.isOnline == true,
                            onClick = { onFriendClick(friend) },
                        )
                    }
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

        // 搜索态没有字母分组，索引条自然为空——列一条点不动的字母条只会误导。
        IndexBar(
            letters = sectionStarts.mapNotNull { it.first },
            modifier = Modifier.align(Alignment.CenterEnd),
            onPick = { letter ->
                sectionStarts.firstOrNull { it.first == letter }?.let { (_, row) ->
                    scope.launch { listState.scrollToItem(row) }
                }
            },
        )
    }
}

/** 好友列表在字母分组之前固定只有一项：好友申请入口。 */
private const val LEADING_ROWS = 1

/** 好友行的分隔线缩进：让线从名字起点开始，而不是从头像起点。 */
private val FRIEND_SEPARATOR_INSET =
    CellDefaults.Default.paddingHorizontal + AvatarSizeTokens.Small.size + Spacing.md

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

    // 和好友页同一种东西：一张卡片，而不是一列浮在页面底色上的裸 Cell。
    GearLazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            CellGroup(
                items = filtered,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                separatorInset = FRIEND_SEPARATOR_INSET,
            ) { group ->
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
        leading = { ContactEntryIcon(Icons.user_plus) },
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
private fun FriendItem(
    friend: FriendEntry,
    hit: FieldHit?,
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
        titleContent = {
            HighlightedText(
                text = friend.displayName,
                match = hit?.match?.takeIf { hit.field == SearchField.DisplayName },
                style = Theme.typography.bodyLarge,
            )
        },
        // 命中在备注/账号名时把那一行显示出来：否则用户看到一个与查询词无关的名字，
        // 不知道自己为什么搜到了它。
        descriptionContent = if (hit != null && hit.field != SearchField.DisplayName) {
            {
                HighlightedText(
                    text = hit.text,
                    match = hit.match,
                    style = Theme.typography.label,
                    color = Theme.colors.mutedForeground,
                )
            }
        } else {
            null
        },
        // 不画箭头：联系人整行就是"进资料页"，一行一个箭头只是把 28 行右侧排满了
        // 同一个符号，不提供任何信息（微信联系人列表也没有）。
        arrow = false,
    )
}
