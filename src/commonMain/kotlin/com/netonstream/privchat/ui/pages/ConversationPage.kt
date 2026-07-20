package com.netonstream.privchat.ui.pages

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
import com.gearui.foundation.typography.Typography
import com.gearui.foundation.avatar.AvatarSizeTokens
import com.gearui.primitives.Badge
import com.gearui.primitives.BadgeTheme
import com.gearui.primitives.HorizontalSpacer
import com.gearui.primitives.VerticalSpacer
import com.tencent.kuikly.compose.ui.unit.Dp
import com.gearui.components.navbar.NavBar
import com.gearui.components.navbar.NavBarItem
import com.gearui.components.contextmenu.ContextMenu
import com.gearui.components.contextmenu.ContextMenuItem
import com.gearui.components.popover.PopoverPlacement
import com.gearui.components.icon.Icons
import com.gearui.components.cell.Cell
import com.gearui.components.empty.EmptyState
import com.gearui.components.searchbar.SearchBar
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
    onPinChannel: (suspend (ULong, Boolean) -> Result<Boolean>)? = null,
    onMuteChannel: (suspend (ULong, Boolean) -> Result<Boolean>)? = null,
    onHideChannel: (suspend (ULong) -> Result<Boolean>)? = null,
    onDeleteChannel: (suspend (ULong) -> Result<Unit>)? = null,
    onError: ((String) -> Unit)? = null,
    showNavBar: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    val channels by PrivChat.channels.collectAsState()
    // 解析 DM 对端 username(系统用户识别,SystemUser 内部有 uid 去重缓存)
    androidx.compose.runtime.LaunchedEffect(channels) {
        channels.forEach { c ->
            if (c.isDm) c.peerUserId?.let { com.netonstream.privchat.ui.models.SystemUser.resolveUid(it) }
        }
    }
    val localStates by PrivChat.channelLocalStates.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
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

    // 任意频道收到新消息时自动滚动到列表顶部
    // index=0 是搜索栏，index=1 是第一条会话，滚到 1 保持搜索栏隐藏（与 iOS 效果一致）
    val channelUpdateMarker = remember(channels) {
        channels.maxOfOrNull { it.lastTs } ?: 0UL
    }
    LaunchedEffect(channelUpdateMarker) {
        if (channelUpdateMarker > 0UL && filteredChannels.isNotEmpty()) {
            delay(50)
            listState.scrollToItem(1)
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
                    title = strings.conversationTitle,
                    rightWidgetWidth = 96.dp,
                    rightWidget = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                        // 全局搜索(聊天记录)入口
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 8.dp)
                                .clickable(onClick = onGlobalSearch),
                        ) {
                            Icon(
                                name = Icons.search,
                                size = 24.dp,
                                tint = Theme.colors.foreground,
                            )
                        }
                        // gearui ContextMenu：定位、阴影、点击外部消失、按下高亮都内置好了
                        ContextMenu(
                            placement = PopoverPlacement.BOTTOM_RIGHT,
                            items = listOf(
                                ContextMenuItem(
                                    label = strings.menuCreateGroup,
                                    icon = Icons.groups,
                                    onClick = onCreateGroup,
                                ),
                                ContextMenuItem(
                                    label = strings.menuAddFriend,
                                    icon = Icons.person_add,
                                    onClick = onAddFriend,
                                ),
                                ContextMenuItem(
                                    label = strings.menuScan,
                                    icon = Icons.camera_alt,
                                    onClick = onScan,
                                ),
                                ContextMenuItem(
                                    label = strings.menuMyQrCode,
                                    icon = Icons.open_in_new,
                                    onClick = onMyQrCode,
                                ),
                            ),
                        ) { onOpen ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .clickable(onClick = onOpen),
                            ) {
                                Icon(
                                    name = Icons.add,
                                    size = 24.dp,
                                    tint = Theme.colors.foreground,
                                )
                            }
                        }
                        }
                    },
                )
            }
            networkStatusBar?.invoke()

        // 会话列表（搜索栏作为第一个 item，下拉时出现，上划时隐藏）
        GearLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
        ) {
            // 搜索栏 item：随列表滚动，下拉显示，上滑消失
            item {
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
                                    onError?.invoke(error.message ?: strings.networkError)
                                }
                            }
                        },
                        onMute = { mute ->
                            scope.launch {
                                val result = onMuteChannel?.invoke(channel.channelId, mute)
                                    ?: PrivChat.client.muteChannel(channel.channelId, mute)
                                result.onFailure { error ->
                                    onError?.invoke(error.message ?: strings.networkError)
                                }
                            }
                        },
                        onHide = {
                            scope.launch {
                                val handler = onHideChannel ?: return@launch
                                handler(channel.channelId).onFailure { error ->
                                    onError?.invoke(error.message ?: strings.networkError)
                                }
                            }
                        },
                        onDelete = {
                            scope.launch {
                                val handler = onDeleteChannel ?: return@launch
                                handler(channel.channelId).onFailure { error ->
                                    onError?.invoke(error.message ?: strings.networkError)
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
                        style = Typography.BodyLarge,
                        color = colors.foreground,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    HorizontalSpacer(8.dp)

                    // 未读消息气泡或勿扰标识
                    if (item.isMuted) {
                        Icon(
                            name = Icons.notifications_off,
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
                                style = Typography.BodySmall,
                                color = colors.destructive,
                                maxLines = 1,
                            )
                            HorizontalSpacer(2.dp)
                            Text(
                                text = draft,
                                style = Typography.BodySmall,
                                color = colors.mutedForeground,
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        Text(
                            text = buildDescription(channel, draft, strings),
                            style = Typography.BodySmall,
                            color = colors.mutedForeground,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalSpacer(8.dp)

                    // 时间
                    Text(
                        text = Formatter.conversationTime(channel.lastMessageTime),
                        style = Typography.Label,
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
