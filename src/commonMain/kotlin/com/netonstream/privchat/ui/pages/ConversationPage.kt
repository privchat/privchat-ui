package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.*
import om.netonstream.privchat.sdk.dto.ChannelListEntry
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.models.*
import com.netonstream.privchat.ui.components.ChatAvatar
import com.netonstream.privchat.ui.utils.Formatter
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.gearui.theme.Theme
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.primitives.GearLazyColumn
import com.gearui.foundation.typography.Typography
import com.gearui.foundation.AvatarSpecs
import com.gearui.primitives.HorizontalSpacer
import com.gearui.primitives.VerticalSpacer
import com.tencent.kuikly.compose.ui.unit.Dp
import com.gearui.components.navbar.NavBar
import com.gearui.components.navbar.NavBarItem
import com.gearui.components.icon.Icons
import com.gearui.components.cell.Cell
import com.gearui.components.empty.EmptyState
import com.gearui.components.searchbar.SearchBar
import com.gearui.components.swipecell.SwipeCell
import com.gearui.components.swipecell.SwipeCellAction
import com.gearui.components.swipecell.SwipeCellActionTheme
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
    onCreateChat: () -> Unit = {},
    onCreateGroup: () -> Unit = {},
    onAddFriend: () -> Unit = {},
    onScan: () -> Unit = {},
    onMyQrCode: () -> Unit = {},
    networkStatusBar: (@Composable () -> Unit)? = null,
    onPinChannel: (suspend (ULong, Boolean) -> Result<Boolean>)? = null,
    onMuteChannel: (suspend (ULong, Boolean) -> Result<Boolean>)? = null,
    onHideChannel: (suspend (ULong) -> Result<Boolean>)? = null,
    onError: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    val channels by PrivChat.channels.collectAsState()
    val localStates by PrivChat.channelLocalStates.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

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

    // 下拉菜单状态
    var showPlusMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部导航栏
            NavBar(
                title = strings.conversationTitle,
                rightItems = listOf(
                    NavBarItem(icon = Icons.add, onClick = { showPlusMenu = true })
                )
            )
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
                                val result = onHideChannel?.invoke(channel.channelId)
                                    ?: PrivChat.client.hideChannel(channel.channelId)
                                result.onFailure { error ->
                                    onError?.invoke(error.message ?: strings.networkError)
                                }
                            }
                        },
                    )
                }
            }
        }
        }

        // "+" 下拉菜单浮层（覆盖在整个页面之上）
        if (showPlusMenu) {
            PlusDropdownMenu(
                onDismiss = { showPlusMenu = false },
                onCreateGroup = { showPlusMenu = false; onCreateGroup() },
                onAddFriend = { showPlusMenu = false; onAddFriend() },
                onScan = { showPlusMenu = false; onScan() },
                onMyQrCode = { showPlusMenu = false; onMyQrCode() },
            )
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
    onClick: () -> Unit,
    onPin: (Boolean) -> Unit,
    onMute: (Boolean) -> Unit,
    onHide: () -> Unit,
) {
    val strings = PrivChatI18n.strings
    val colors = Theme.colors
    val swipeCellState = rememberSwipeCellState()

    // 背景色
    val backgroundColor = when {
        channel.isPinned -> colors.surfaceVariant
        else -> colors.surface
    }

    // 右滑操作：置顶/取消置顶、删除
    val rightActions = listOf(
        SwipeCellAction(
            label = if (channel.isPinned) strings.conversationUnpin else strings.conversationPin,
            theme = SwipeCellActionTheme.PRIMARY,
            onClick = { onPin(!channel.isPinned) },
        ),
        SwipeCellAction(
            label = strings.delete,
            theme = SwipeCellActionTheme.DANGER,
            onClick = onHide,
        ),
    )

    SwipeCell(
        state = swipeCellState,
        rightActions = rightActions,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧头像
            ChatAvatar(
                url = channel.avatarUrl,
                name = channel.displayName,
                size = AvatarSpecs.Size.medium,
            )

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
                        text = channel.displayName,
                        style = Typography.BodyLarge,
                        color = colors.textPrimary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    HorizontalSpacer(8.dp)

                    // 未读消息气泡或勿扰标识
                    if (channel.isMuted) {
                        Icon(
                            name = Icons.notifications_off,
                            size = 14.dp,
                            tint = colors.textSecondary
                        )
                    } else if (channel.unreadCount.toInt() > 0) {
                        // 未读消息气泡
                        UnreadBadge(count = channel.unreadCount.toInt())
                    }
                }

                VerticalSpacer(4.dp)

                // 第二行：消息预览 + 时间
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 消息预览
                    Text(
                        text = buildDescription(channel, draft, strings),
                        style = Typography.BodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    HorizontalSpacer(8.dp)

                    // 时间
                    Text(
                        text = Formatter.conversationTime(channel.lastMessageTime),
                        style = Typography.Label,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

/**
 * 未读消息气泡
 */
@Composable
private fun UnreadBadge(count: Int) {
    val colors = Theme.colors
    val displayText = if (count > 99) "99+" else count.toString()

    Box(
        modifier = Modifier
            .height(18.dp)
            .widthIn(min = 18.dp)
            .background(colors.danger, shape = com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape(9.dp))
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayText,
            style = Typography.Label,
            color = colors.textAnti,
        )
    }
}

/**
 * 构建会话描述文本
 */
private fun buildDescription(
    channel: ChannelListEntry,
    draft: String?,
    strings: com.netonstream.privchat.ui.i18n.PrivChatStrings
): String {
    val builder = StringBuilder()

    // 草稿优先
    if (!draft.isNullOrBlank()) {
        return "${strings.conversationDraft} $draft"
    }

    // @提及
    if (channel.mentions > 0u) {
        builder.append("${strings.conversationAtMe} ")
    }

    // 最后消息预览
    builder.append(channel.lastMessagePreview)

    return builder.toString()
}

/**
 * 首页 "+" 下拉菜单（仿微信风格）
 */
@Composable
private fun PlusDropdownMenu(
    onDismiss: () -> Unit,
    onCreateGroup: () -> Unit,
    onAddFriend: () -> Unit,
    onScan: () -> Unit,
    onMyQrCode: () -> Unit,
) {
    val strings = PrivChatI18n.strings

    // 全屏透明遮罩，点击关闭菜单
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
            .clickable { onDismiss() }
    ) {
        // 菜单面板，定位到右上角
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 56.dp, end = 12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF353535))
                .width(160.dp)
        ) {
            PlusMenuItem(icon = Icons.groups, text = strings.menuCreateGroup, onClick = onCreateGroup)
            PlusMenuItem(icon = Icons.person_add, text = strings.menuAddFriend, onClick = onAddFriend)
            PlusMenuItem(icon = Icons.camera_alt, text = strings.menuScan, onClick = onScan)
            PlusMenuItem(icon = Icons.open_in_new, text = strings.menuMyQrCode, onClick = onMyQrCode)
        }
    }
}

/**
 * 下拉菜单项
 */
@Composable
private fun PlusMenuItem(
    icon: String,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            name = icon,
            size = 20.dp,
            tint = Color.White,
        )
        HorizontalSpacer(12.dp)
        Text(
            text = text,
            style = Typography.BodyMedium,
            color = Color.White,
        )
    }
}
