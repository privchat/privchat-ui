package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.*
import com.gearui.components.cell.Cell
import com.gearui.components.empty.EmptyState
import com.gearui.components.navbar.NavBar
import com.gearui.components.searchbar.SearchBar
import com.gearui.foundation.primitives.GearLazyColumn
import com.gearui.foundation.primitives.Text
import com.gearui.theme.Theme
import com.netonstream.privchat.sdk.dto.ChannelListEntry
import com.netonstream.privchat.sdk.dto.SearchHistoryHit
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.netonstream.privchat.ui.models.displayName
import com.netonstream.privchat.ui.runtime.ClientRuntime
import com.netonstream.privchat.ui.runtime.ClientRuntimeError
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 全局搜索页（MESSAGE_HISTORY spec §7，Telegram 式）。
 *
 * 两源聚合：
 * 1. 本地会话（PrivChat.channels 名称即时过滤，无网络）；
 * 2. 远程云端历史（message/history/search，snippet 投影）。
 *
 * 远程搜索约束（spec §4，服务端限频 300ms/user）：
 * - query 输入 debounce 400ms（LaunchedEffect(query) 天然取消过期请求）；
 * - query < 2 字符不发起远程；
 * - 命中 snippet **不落本地 message 表**，点击后由上层走 around 拉完整上下文。
 */
@Composable
fun GlobalSearchPage(
    onBack: () -> Unit,
    /** 点击消息命中：上层负责 push 聊天页并带 anchorMessageId 定位 */
    onOpenMessageHit: (channel: ChannelListEntry, anchorMessageId: ULong) -> Unit,
    /** 点击会话命中：正常打开聊天页 */
    onOpenChannel: (ChannelListEntry) -> Unit,
    /** 点击联系人命中：打开与该好友的 DM（宿主走 direct/get_or_create 链路）。 */
    onOpenUserChat: (ULong) -> Unit = {},
    /**
     * 会话内搜索（CHANNEL scope）：非空时只搜该会话云端历史，不显示会话命中区，
     * 从聊天页「…」菜单进入；null = 全局搜索（会话列表入口，本地会话 + 云端全局）。
     */
    scopeChannel: ChannelListEntry? = null,
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    val colors = Theme.colors
    val scope = rememberCoroutineScope()
    val channels by PrivChat.channels.collectAsState()
    val channelsById = remember(channels) { channels.associateBy { it.channelId } }
    val scopeId = scopeChannel?.channelId

    var query by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<SearchHistoryHit>>(emptyList()) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    // 终态失败可见（P5：不再 onSuccess 静默——失败与「无结果」必须区分，
    // 否则网络/限频挂掉时用户看到假的「无搜索结果」）。retryNonce 变化 = 重发。
    var error by remember { mutableStateOf(false) }
    var retryNonce by remember { mutableStateOf(0) }

    // 钻取态（spec §7.1）：null=分组总览；CONTACTS/GROUPS/MESSAGES=该组全量。
    // 页内状态切换而不是 push 新路由——query、已拉取的远程结果全部原地保留。
    var drill by remember { mutableStateOf<SearchDrill?>(null) }

    val friends by PrivChat.friends.collectAsState()

    // 本地组即时过滤（无 debounce，纯内存；spec §7.1）。会话内搜索不展示。
    val contactHits = remember(query, friends, scopeId) {
        val q = query.trim()
        if (q.isEmpty() || scopeId != null) emptyList()
        else friends.filter { f ->
            (f.remark?.contains(q, ignoreCase = true) == true) ||
                (f.nickname?.contains(q, ignoreCase = true) == true) ||
                f.username.contains(q, ignoreCase = true)
        }
    }
    val groupHits = remember(query, channels, scopeId) {
        val q = query.trim()
        if (q.isEmpty() || scopeId != null) emptyList()
        else channels.filter { !it.isDm && it.displayName.contains(q, ignoreCase = true) }
    }

    // 远程搜索：debounce 400ms；query 变化自动取消 in-flight（过期结果天然丢弃）
    LaunchedEffect(query, retryNonce) {
        val q = query.trim()
        hits = emptyList()
        nextCursor = null
        searched = false
        error = false
        if (q.length < 2) {
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(400L)
        val result = PrivChat.client.searchMessageHistory(q, channelId = scopeId)
        isSearching = false
        searched = true
        result
            .onSuccess { page ->
                hits = page.hits
                nextCursor = page.nextCursor
            }
            .onFailure {
                error = true
                // P5：搜索 RPC 的 busy/限流失败也喂统一运行时横幅（与发送失败同款判据）。
                if (ClientRuntimeError.isServerBusySignal(null, it.message)) {
                    ClientRuntime.onServerBusySignal()
                }
            }
    }

    fun loadMore() {
        val cursor = nextCursor ?: return
        val q = query.trim()
        if (q.length < 2 || isLoadingMore) return
        isLoadingMore = true
        scope.launch {
            PrivChat.client.searchMessageHistory(q, channelId = scopeId, cursor = cursor)
                .onSuccess { page ->
                    hits = hits + page.hits
                    nextCursor = page.nextCursor
                }
            isLoadingMore = false
        }
    }

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        NavBar(
            title = when {
                scopeChannel != null -> strings.globalSearchPlaceholder
                drill == SearchDrill.CONTACTS -> strings.globalSearchSectionContacts
                drill == SearchDrill.GROUPS -> strings.globalSearchSectionGroups
                drill == SearchDrill.MESSAGES -> strings.globalSearchSectionMessages
                else -> strings.globalSearchTitle
            },
            useDefaultBack = true,
            // 钻取态返回先回总览（同一 query 不丢），总览态才真正退出。
            onBackClick = { if (drill != null) drill = null else onBack() },
        )

        SearchBar(
            value = query,
            onValueChange = { query = it; drill = null },
            placeholder = strings.globalSearchPlaceholder,
            autoFocus = true,
            showCancel = true,
            onCancel = onBack,
            shape = com.gearui.components.searchbar.SearchBarShape.SQUARE,
            alignment = com.gearui.components.searchbar.SearchBarAlignment.CENTER,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        val nothing = contactHits.isEmpty() && groupHits.isEmpty() && hits.isEmpty()
        if (error && contactHits.isEmpty() && groupHits.isEmpty() && !isSearching) {
            // 失败终态：可见错误 + 可点重试（复用 networkError/retry，避免新增 i18n key）。
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = strings.networkError, color = colors.mutedForeground)
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clickable(onClick = { retryNonce += 1 }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = strings.retry, color = colors.primary)
                }
            }
        } else if (query.trim().length >= 2 && searched && nothing && !isSearching) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(message = strings.globalSearchNoResult)
            }
        } else {
            // 每组在总览态最多 3 条（spec §7.1）；钻取态该组全量、其余组隐藏。
            val overviewCap = 3
            val showContacts = scopeId == null && contactHits.isNotEmpty() &&
                (drill == null || drill == SearchDrill.CONTACTS)
            val showGroups = scopeId == null && groupHits.isNotEmpty() &&
                (drill == null || drill == SearchDrill.GROUPS)
            val showMessages = hits.isNotEmpty() && (drill == null || drill == SearchDrill.MESSAGES)
            val visibleContacts = if (drill == SearchDrill.CONTACTS) contactHits else contactHits.take(overviewCap)
            val visibleGroups = if (drill == SearchDrill.GROUPS) groupHits else groupHits.take(overviewCap)
            val visibleHits = if (drill == SearchDrill.MESSAGES) hits else hits.take(overviewCap)

            GearLazyColumn(modifier = Modifier.fillMaxSize()) {
                if (showContacts) {
                    item { SearchSectionHeader(strings.globalSearchSectionContacts) }
                    items(visibleContacts.size) { i ->
                        val f = visibleContacts[i]
                        val display = f.remark?.takeIf { it.isNotBlank() }
                            ?: f.nickname?.takeIf { it.isNotBlank() }
                            ?: f.username
                        Cell(
                            title = display,
                            leading = {
                                com.netonstream.privchat.ui.avatar.PrivChatAvatar(
                                    model = com.netonstream.privchat.ui.avatar.AvatarModel(
                                        userId = f.userId.toLong(),
                                        displayName = display,
                                        username = f.username,
                                        remoteUrl = f.avatarUrl,
                                        seed = "u:" + f.userId,
                                    ),
                                    size = 40.dp,
                                )
                            },
                            arrow = true,
                            onClick = { onOpenUserChat(f.userId) },
                        )
                    }
                    if (drill == null && contactHits.size > overviewCap) {
                        item { SearchMoreRow(strings.globalSearchMoreContacts) { drill = SearchDrill.CONTACTS } }
                    }
                }
                if (showGroups) {
                    item { SearchSectionHeader(strings.globalSearchSectionGroups) }
                    items(visibleGroups.size) { i ->
                        val ch = visibleGroups[i]
                        Cell(
                            title = ch.displayName,
                            arrow = true,
                            onClick = { onOpenChannel(ch) },
                        )
                    }
                    if (drill == null && groupHits.size > overviewCap) {
                        item { SearchMoreRow(strings.globalSearchMoreGroups) { drill = SearchDrill.GROUPS } }
                    }
                }
                if (showMessages) {
                    item { SearchSectionHeader(strings.globalSearchSectionMessages) }
                    items(visibleHits.size) { i ->
                        val hit = visibleHits[i]
                        MessageHitRow(
                            hit = hit,
                            channelName = scopeChannel?.displayName
                                ?: channelsById[hit.channelId]?.displayName
                                ?: hit.channelId.toString(),
                            onClick = {
                                // 会话内搜索：命中一定属于 scopeChannel，直接用它兜底。
                                (scopeChannel ?: channelsById[hit.channelId])?.let { ch ->
                                    onOpenMessageHit(ch, hit.messageId)
                                }
                            },
                        )
                    }
                    if (drill == null && scopeId == null && (hits.size > overviewCap || nextCursor != null)) {
                        item { SearchMoreRow(strings.globalSearchMoreMessages) { drill = SearchDrill.MESSAGES } }
                    }
                    if ((drill == SearchDrill.MESSAGES || scopeId != null) && nextCursor != null) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .clickable(onClick = { loadMore() }),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (isLoadingMore) strings.loading else strings.globalSearchLoadMore,
                                    color = colors.mutedForeground,
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(title: String) {
    Text(
        text = title,
        color = Theme.colors.mutedForeground,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** 消息命中行：会话名 + snippet（第一段命中用主题色，三段拆分，单行省略） */
@Composable
private fun MessageHitRow(
    hit: SearchHistoryHit,
    channelName: String,
    onClick: () -> Unit,
) {
    val colors = Theme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(text = channelName, color = colors.foreground)
        Spacer(modifier = Modifier.height(2.dp))
        val range = hit.highlightRanges.firstOrNull()
        if (range != null && range.first in 0 until hit.snippet.length && range.second <= hit.snippet.length && range.first < range.second) {
            Row(modifier = Modifier.fillMaxWidth()) {
                val chars = hit.snippet.toCharArray()
                val prefix = chars.concatToString(0, range.first)
                val match = chars.concatToString(range.first, range.second)
                val suffix = chars.concatToString(range.second, chars.size)
                if (prefix.isNotEmpty()) {
                    Text(text = prefix, color = colors.mutedForeground, maxLines = 1)
                }
                Text(text = match, color = colors.primary, maxLines = 1)
                if (suffix.isNotEmpty()) {
                    Text(text = suffix, color = colors.mutedForeground, maxLines = 1)
                }
            }
        } else {
            Text(text = hit.snippet, color = colors.mutedForeground, maxLines = 1)
        }
    }
}

/** 钻取组（spec §7.1）。 */
private enum class SearchDrill { CONTACTS, GROUPS, MESSAGES }

/** 「更多X」行：整行可点，右侧箭头。 */
@Composable
private fun SearchMoreRow(label: String, onClick: () -> Unit) {
    Cell(
        title = label,
        arrow = true,
        onClick = onClick,
    )
}
