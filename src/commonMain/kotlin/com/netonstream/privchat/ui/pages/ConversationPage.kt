package com.netonstream.privchat.ui.pages

import com.tencent.kuikly.compose.ui.graphics.graphicsLayer
import com.gearui.foundation.layout.Spacing
import com.gearui.components.searchbar.SearchBarAlignment
import com.gearui.components.searchbar.SearchBarButton
import com.gearui.components.icon.*
import androidx.compose.runtime.*
import com.netonstream.privchat.sdk.dto.ChannelListEntry
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.models.*
import com.netonstream.privchat.ui.components.ChatAvatar
import com.netonstream.privchat.ui.avatar.GroupCollageAvatar
import com.netonstream.privchat.ui.avatar.PrivChatAvatar
import com.netonstream.privchat.ui.utils.Formatter
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.gearui.theme.Theme
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.primitives.GearLazyColumn
import com.gearui.foundation.avatar.AvatarSizeTokens
import com.gearui.primitives.Badge
import com.gearui.primitives.BadgeTheme
import com.gearui.primitives.HorizontalSpacer
import com.gearui.primitives.VerticalSpacer
import com.tencent.kuikly.compose.ui.unit.Dp
import com.gearui.components.loading.Loading
import com.gearui.components.loading.LoadingIcon
import com.gearui.components.loading.LoadingLayout
import com.gearui.components.loading.LoadingSize
import com.gearui.components.navbar.NavBar
import com.gearui.components.navbar.NavBarActionSlot
import com.gearui.components.navbar.NavBarDefaults
import com.gearui.components.navbar.NavBarItem
import com.gearui.components.contextmenu.ContextMenu
import com.gearui.components.contextmenu.ContextMenuItem
import com.gearui.components.popover.PopoverPlacement
import com.gearui.components.icon.Icons
import com.gearui.components.cell.Cell
import com.gearui.components.empty.EmptyState
import com.gearui.components.swipecell.SwipeCell
import com.gearui.components.swipecell.SwipeCellAction
import com.gearui.components.swipecell.SwipeCellActionTheme
import com.gearui.components.swipecell.SwipeCellGroupState
import com.gearui.components.swipecell.rememberSwipeCellGroupState
import com.gearui.components.swipecell.rememberSwipeCellState
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.zIndex
import com.gearui.foundation.primitives.Icon
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * 会话列表页面
 *
 * 直接使用 SDK 的 ChannelListEntry 类型
 *
 * @param onChannelClick 点击频道回调
 * @param onCreateChat 点击创建聊天回调
 * @param modifier Modifier
 */
@Composable
fun ConversationPage(
    onChannelClick: (ChannelListEntry) -> Unit,
    onGlobalSearch: () -> Unit = {},
    onCreateChat: () -> Unit = {},
    onCreateGroup: () -> Unit = {},
    onAddFriend: () -> Unit = {},
    onScan: () -> Unit = {},
    onMyQrCode: () -> Unit = {},
    networkStatusBar: (@Composable () -> Unit)? = null,
    /**
     * 连接/同步状态文案，显示在**标题位**取代「消息」。
     *
     * Telegram 的做法：标题栏本来就是空的，断线时借用它，恢复了自然变回去——不必为一条
     * 临时状态永久让出一行高度，列表也不会在状态出现/消失时整体跳动。
     */
    statusTitle: String? = null,
    /**
     * 状态是否「进行中」。true 时标题前面转圈。
     *
     * 只有连接中/重连中/同步中该转——它们表示**正在努力**，转圈是在说"还没放弃"。
     * 「网络已断开」「登录失效」是停下来的状态，给它们配个转圈等于承诺一个不会发生的
     * 恢复。
     */
    statusBusy: Boolean = false,
    onPinChannel: (suspend (ULong, Boolean) -> Result<Boolean>)? = null,
    onMuteChannel: (suspend (ULong, Boolean) -> Result<Boolean>)? = null,
    onHideChannel: (suspend (ULong) -> Result<Boolean>)? = null,
    onDeleteChannel: (suspend (ULong) -> Result<Unit>)? = null,
    onError: ((String) -> Unit)? = null,
    showNavBar: Boolean = true,
    /** 双击底部「消息」Tab 的回顶信号：外部每次递增即滚回列表顶部（0 = 不动）。 */
    scrollToTopSignal: Int = 0,
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    val channels by PrivChat.channels.collectAsState()
    // 解析 DM 对端 username(系统用户识别,SystemUser 内部有 uid 去重缓存)
    androidx.compose.runtime.LaunchedEffect(channels) {
        channels.forEach { c ->
            if (c.isDm) c.peerUserId?.let {
                com.netonstream.privchat.ui.models.SystemUser.resolveUid(it, sourceChannelId = c.channelId)
            }
        }
    }
    val localStates by PrivChat.channelLocalStates.collectAsState()
    val scope = rememberCoroutineScope()
    // 第 0 项是搜索入口，平时藏在导航栏下面：列表从第 1 项（第一条会话）开始，下拉才露出来。
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = FIRST_CONVERSATION_INDEX)
    val swipeGroup = rememberSwipeCellGroupState()

    // 搜索关键词
    var searchQuery by remember { mutableStateOf("") }

    // 过滤后的会话列表
    val filteredChannels = remember(channels, searchQuery) {
        val base = if (searchQuery.isBlank()) {
            channels
        } else {
            channels.filter { channel ->
                channel.displayName.contains(searchQuery, ignoreCase = true)
            }
        }
        base.sortedWith(
            compareByDescending<ChannelListEntry> { it.isPinned }
                .thenByDescending { it.lastMessageTime }
        )
    }

    // 搜索入口露出的比例（0 = 藏着，1 = 全露）。导航栏的放大镜随之淡出：两个搜索入口不同时出现（微信的做法）。
    val searchReveal by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex != SEARCH_ENTRY_INDEX) return@derivedStateOf 0f
            val entry = listState.layoutInfo.visibleItemsInfo.firstOrNull() ?: return@derivedStateOf 0f
            if (entry.size <= 0) 0f
            else (1f - listState.firstVisibleItemScrollOffset.toFloat() / entry.size).coerceIn(0f, 1f)
        }
    }

    // 双击底部「消息」Tab 回到列表顶部：外部每次双击把计数 +1，这里响应变化滚动。
    // 落点是第一条会话（搜索入口仍藏着），不是第 0 项。
    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) {
            listState.animateScrollToItem(FIRST_CONVERSATION_INDEX)
        }
    }

    // 任意频道收到新消息时自动回到顶部，让新消息那一行露出来。搜索入口正露着时列表已经
    // 在顶部，不动它；否则落到第一条会话——不能落到第 0 项，否则每来一条消息搜索框都冒出来。
    val channelUpdateMarker = remember(channels) {
        channels.maxOfOrNull { it.lastTs } ?: 0UL
    }
    LaunchedEffect(channelUpdateMarker) {
        if (channelUpdateMarker > 0UL && filteredChannels.isNotEmpty()) {
            delay(50)
            if (listState.firstVisibleItemIndex != SEARCH_ENTRY_INDEX) {
                listState.scrollToItem(FIRST_CONVERSATION_INDEX)
            }
        }
    }

    // 搜索入口只停在「全露」或「全藏」：松手时露出超过一半就展开，否则收回（iOS 列表顶部
    // 搜索栏的行为）。只看滚动停下的那一刻，拖动中不干预。
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (scrolling || listState.firstVisibleItemIndex != SEARCH_ENTRY_INDEX) return@collect
            val entry = listState.layoutInfo.visibleItemsInfo.firstOrNull() ?: return@collect
            val hidden = listState.firstVisibleItemScrollOffset
            if (hidden <= 0 || hidden >= entry.size) return@collect
            if (hidden * 2 < entry.size) {
                listState.animateScrollToItem(SEARCH_ENTRY_INDEX)
            } else {
                listState.animateScrollToItem(FIRST_CONVERSATION_INDEX)
            }
        }
    }

    // 确保会话数据已加载
    LaunchedEffect(Unit) {
        if (channels.isEmpty() && PrivChat.isInitialized) {
            PrivChat.client.getChannels(100u, 0u).onSuccess { list ->
                PrivChat.updateChannels(list)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部导航栏
            if (showNavBar) {
                NavBar(
                    title = if (statusTitle == null) strings.conversationTitle else "",
                    titleWidget = if (statusTitle != null && statusBusy) {
                        {
                            Loading(
                                size = LoadingSize.SMALL,
                                icon = LoadingIcon.CIRCLE,
                                text = statusTitle,
                                layout = LoadingLayout.HORIZONTAL,
                                color = Theme.colors.foreground,
                            )
                        }
                    } else if (statusTitle != null) {
                        { Text(text = statusTitle, style = Theme.typography.titleMedium, color = Theme.colors.foreground) }
                    } else null,
                    // 🔴 用 kit 的槽位宽度算总宽，别写死。写死过 96dp，和 NavBarItem 的两个槽位
                    // 不一致——于是这一页的顶部图标比联系人页窄、间距也不一样。同一个顶栏两套
                    // 几何，只能靠肉眼比截图才发现。
                    rightWidgetWidth = NavBarDefaults.actionSlotWidth * 2,
                    rightWidget = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                        // 全局搜索(聊天记录)入口。列表顶部的搜索框露出时淡出；全露时不再接点击，
                        // 不留一个看不见却点得到的按钮。
                        NavBarActionSlot(
                            modifier = Modifier.graphicsLayer { alpha = 1f - searchReveal },
                            onClick = if (searchReveal < 1f) onGlobalSearch else null,
                        ) {
                            Icon(Icons.magnifyingGlass,
                                size = NavBarDefaults.actionIconSize,
                                tint = Theme.colors.foreground,
                            )
                        }
                        // gearui ContextMenu：定位、阴影、点击外部消失、按下高亮都内置好了
                        ContextMenu(
                            placement = PopoverPlacement.BOTTOM_RIGHT,
                            items = listOf(
                                ContextMenuItem(
                                    label = strings.menuCreateGroup,
                                    icon = Icons.usersThree,
                                    onClick = onCreateGroup,
                                ),
                                ContextMenuItem(
                                    label = strings.menuAddFriend,
                                    icon = Icons.userPlus,
                                    onClick = onAddFriend,
                                ),
                                ContextMenuItem(
                                    label = strings.menuScan,
                                    icon = Icons.camera,
                                    onClick = onScan,
                                ),
                                ContextMenuItem(
                                    label = strings.menuMyQrCode,
                                    icon = Icons.arrowSquareOut,
                                    onClick = onMyQrCode,
                                ),
                            ),
                        ) { onOpen ->
                            NavBarActionSlot(onClick = onOpen) {
                                Icon(Icons.plus,
                                    size = NavBarDefaults.actionIconSize,
                                    tint = Theme.colors.foreground,
                                )
                            }
                        }
                        }
                    },
                )
            }
            networkStatusBar?.invoke()

        // 会话列表。第 0 项是下拉露出的搜索入口，和 NavBar 的放大镜同一个去处（全局搜索页）。
        GearLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
        ) {
            item(key = "search-entry") {
                SearchBarButton(
                    onClick = onGlobalSearch,
                    placeholder = strings.search,
                    alignment = SearchBarAlignment.CENTER,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                )
            }
            if (filteredChannels.isEmpty()) {
                // 空状态
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyState(
                            message = strings.conversationEmpty,
                        )
                    }
                }
            } else {
                items(filteredChannels.size) { index ->
                    val channel = filteredChannels[index]
                    val draft = localStates[channel.channelId]?.draftText

                    ChannelItem(
                        channel = channel,
                        draft = draft,
                        swipeGroup = swipeGroup,
                        onClick = { onChannelClick(channel) },
                        onPin = { pin ->
                            scope.launch {
                                val result = onPinChannel?.invoke(channel.channelId, pin)
                                    ?: PrivChat.client.pinChannel(channel.channelId, pin)
                                result.onFailure { error ->
                                    onError?.invoke(com.netonstream.privchat.ui.error.UserFacingError.message(error, strings.networkError))
                                }
                            }
                        },
                        onMute = { mute ->
                            scope.launch {
                                val result = onMuteChannel?.invoke(channel.channelId, mute)
                                    ?: PrivChat.client.muteChannel(channel.channelId, mute)
                                result.onFailure { error ->
                                    onError?.invoke(com.netonstream.privchat.ui.error.UserFacingError.message(error, strings.networkError))
                                }
                            }
                        },
                        onHide = {
                            scope.launch {
                                val handler = onHideChannel ?: return@launch
                                handler(channel.channelId).onFailure { error ->
                                    onError?.invoke(com.netonstream.privchat.ui.error.UserFacingError.message(error, strings.networkError))
                                }
                            }
                        },
                        onDelete = {
                            scope.launch {
                                val handler = onDeleteChannel ?: return@launch
                                handler(channel.channelId).onFailure { error ->
                                    onError?.invoke(com.netonstream.privchat.ui.error.UserFacingError.message(error, strings.networkError))
                                }
                            }
                        },
                    )
                }
            }
        }
        }

    }
}

/**
 * 单个会话项
 */
@Composable
private fun ChannelItem(
    channel: ChannelListEntry,
    draft: String?,
    swipeGroup: SwipeCellGroupState,
    onClick: () -> Unit,
    onPin: (Boolean) -> Unit,
    onMute: (Boolean) -> Unit,
    onHide: () -> Unit,
    onDelete: () -> Unit,
) {
    val strings = PrivChatI18n.strings
    val colors = Theme.colors
    val swipeCellState = rememberSwipeCellState()
    // presence 只读全局真源；collect 触发在线态变化重组。
    val presences by PrivChat.presences.collectAsState()
    val scope = rememberCoroutineScope()

    // P3.5：会话行只消费统一聚合 ViewState（标题/头像/presence/未读…），不再各自拼。
    // 不缓存进 remember——标题的系统用户本地化依赖 SystemUser 的 Compose State（resolveUid 异步就绪后
    // 需重组刷新），每次重组重算（很轻），presences 传入保证在线态响应。
    val item = ConversationListItemState.from(channel, presences)
    val isOnline = item.isOnline

    // 背景色
    val backgroundColor = when {
        item.isPinned -> colors.muted
        else -> colors.surface
    }

    // 右滑操作：置顶/取消置顶、隐藏、删除
    val rightActions = listOf(
        SwipeCellAction(
            label = if (channel.isPinned) strings.conversationUnpin else strings.conversationPin,
            theme = SwipeCellActionTheme.SUCCESS,
            onClick = { onPin(!channel.isPinned) },
        ),
        SwipeCellAction(
            label = strings.conversationHide,
            theme = SwipeCellActionTheme.WARNING,
            onClick = onHide,
        ),
        SwipeCellAction(
            label = strings.conversationDelete,
            theme = SwipeCellActionTheme.DANGER,
            onClick = onDelete,
        ),
    )

    SwipeCell(
        state = swipeCellState,
        groupState = swipeGroup,
        rightActions = rightActions,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .clickable {
                    if (swipeGroup.isAnyOpen) {
                        scope.launch { swipeGroup.closeAll() }
                    } else {
                        onClick()
                    }
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧头像：群 → 成员九宫格拼贴；DM → 统一 AvatarModel（local-first，presence 小绿点）。
            if (item.isGroup) {
                GroupCollageAvatar(
                    channelId = item.channelId,
                    name = item.title,
                    size = AvatarSizeTokens.Medium.size,
                )
            } else if (item.avatar != null) {
                PrivChatAvatar(
                    model = item.avatar,
                    size = AvatarSizeTokens.Medium.size,
                    isOnline = item.isOnline,
                )
            }

            HorizontalSpacer(12.dp)

            // 中间内容区域
            Column(modifier = Modifier.weight(1f)) {
                // 第一行：标题 + 未读/勿扰标识
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 标题
                    Text(
                        text = item.title,
                        style = Theme.typography.bodyLarge,
                        color = colors.foreground,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    HorizontalSpacer(8.dp)

                    // 未读消息气泡或勿扰标识
                    if (item.isMuted) {
                        Icon(Icons.bellSlash,
                            size = 14.dp,
                            tint = colors.mutedForeground
                        )
                    } else if (item.unreadCount > 0) {
                        // 未读消息气泡：走 gearui-kit Badge 规范（红底白字由 BadgeTheme.Error token 决定）
                        Badge(
                            count = item.unreadCount,
                            theme = BadgeTheme.Error,
                        )
                    }
                }

                VerticalSpacer(4.dp)

                // 第二行：消息预览 + 时间
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 消息预览（UX-9.4：有草稿时 `[草稿]` 红色前缀 + 草稿正文灰色）
                    if (!draft.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = strings.conversationDraft,
                                style = Theme.typography.bodySmall,
                                color = colors.destructive,
                                maxLines = 1,
                            )
                            HorizontalSpacer(2.dp)
                            Text(
                                text = draft,
                                style = Theme.typography.bodySmall,
                                color = colors.mutedForeground,
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        Text(
                            text = buildDescription(channel, draft, strings),
                            style = Theme.typography.bodySmall,
                            color = colors.mutedForeground,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalSpacer(8.dp)

                    // 时间
                    Text(
                        text = Formatter.conversationTime(channel.lastMessageTime),
                        style = Theme.typography.label,
                        color = colors.mutedForeground,
                    )
                }
            }
        }
    }
}


/**
 * 构建会话描述文本（不含草稿分支——草稿态在 UI 层用独立的彩色 Text 渲染，见会话行）。
 */
private fun buildDescription(
    channel: ChannelListEntry,
    draft: String?,
    strings: com.netonstream.privchat.ui.i18n.PrivChatStrings
): String {
    val builder = StringBuilder()

    // @提及
    if (channel.mentions > 0u) {
        builder.append("${strings.conversationAtMe} ")
    }

    // 最后消息预览（i18n + 系统消息模板渲染）——架构归正后的唯一入口
    builder.append(channel.lastMessagePreviewLocalized(strings))

    return builder.toString()
}

/** 列表第 0 项：下拉露出的搜索入口。 */
private const val SEARCH_ENTRY_INDEX = 0

/** 第一条会话（或空状态）所在的位置；列表的「顶部」指这里，搜索入口藏在它上面。 */
private const val FIRST_CONVERSATION_INDEX = 1
