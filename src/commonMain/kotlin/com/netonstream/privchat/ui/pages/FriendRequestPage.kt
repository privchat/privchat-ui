package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.*
import com.netonstream.privchat.sdk.dto.FriendRequestEntry
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.components.ChatAvatar
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.netonstream.privchat.ui.i18n.PrivChatStrings
import com.netonstream.privchat.ui.utils.Formatter
import com.gearui.theme.Theme
import com.gearui.foundation.avatar.AvatarSizeTokens
import com.gearui.foundation.primitives.GearLazyColumn
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.typography.Typography
import com.gearui.components.navbar.NavBar
import com.gearui.components.empty.EmptyState
import com.gearui.components.tabs.Tab
import com.gearui.components.tabs.Tabs
import com.gearui.components.tabs.TabsOutlineType
import com.gearui.components.tabs.TabsSize
import com.gearui.components.button.Button
import com.gearui.components.button.ButtonSize
import com.gearui.components.button.ButtonShape
import com.gearui.components.button.ButtonTheme
import com.gearui.components.button.ButtonType
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp

private const val TAB_RECEIVED = "received"
private const val TAB_SENT = "sent"

// friendships.status 值；与 server FriendshipStatus 对齐。
private const val STATUS_PENDING: Short = 0
private const val STATUS_ACCEPTED: Short = 1
private const val STATUS_REJECTED: Short = 3
private const val STATUS_RECALLED: Short = 4
private const val STATUS_EXPIRED: Short = 5

/**
 * 好友申请页（F-sync.3：数据源切到本地 friendships 投影）。
 *
 * 顶部 Tabs：【收到的 / 我发送的】。两个 tab 的数据都来自
 * [PrivChat.receivedFriendRequests] / [PrivChat.sentFriendRequests]，由 SDK 通过
 * `entity/sync_entities("friend")` 多端同步——离线后第一次连接也能正确回放。
 *
 * - **收到的**：按 local 月份分两段（当月 / 更早）。pending(0) 行显示 Accept/Decline
 *   按钮；rejected/recalled/expired 行显示只读状态文本。
 * - **我发送的**：按时间倒序排列。pending 行带 Recall 按钮；rejected/recalled/
 *   expired/accepted 行只读显示状态。
 *
 * @param onAccept Received tab 行内"接受"按钮
 * @param onDecline Received tab 行内"拒绝"按钮
 * @param onRecall Sent tab 行内"撤回"按钮
 * @param onRequestClick 点击整行——跳用户资料页（兼容旧入口）
 */
@Composable
fun FriendRequestPage(
    onBack: () -> Unit,
    onRequestClick: (FriendRequestEntry) -> Unit = {},
    onAccept: (FriendRequestEntry) -> Unit = {},
    onDecline: (FriendRequestEntry) -> Unit = {},
    onRecall: (FriendRequestEntry) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    val received by PrivChat.receivedFriendRequests.collectAsState()
    val sent by PrivChat.sentFriendRequests.collectAsState()

    var selectedTab by remember { mutableStateOf(TAB_RECEIVED) }

    val receivedLabel = "${strings.friendRequestTabReceived} ${received.size}"
    val sentLabel = "${strings.friendRequestTabSent} ${sent.size}"

    Column(modifier = modifier.fillMaxSize()) {
        NavBar(
            title = strings.friendRequestTitle,
            useDefaultBack = true,
            onBackClick = onBack,
        )

        Tabs(
            items = listOf(
                Tab(id = TAB_RECEIVED, label = receivedLabel),
                Tab(id = TAB_SENT, label = sentLabel),
            ),
            selectedId = selectedTab,
            onSelect = { selectedTab = it },
            size = TabsSize.MEDIUM,
            outlineType = TabsOutlineType.UNDERLINE,
            showDivider = true,
        )

        when (selectedTab) {
            TAB_RECEIVED -> ReceivedTabContent(
                requests = received,
                onRequestClick = onRequestClick,
                onAccept = onAccept,
                onDecline = onDecline,
            )
            TAB_SENT -> SentTabContent(
                requests = sent,
                onRequestClick = onRequestClick,
                onRecall = onRecall,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Received tab
// ---------------------------------------------------------------------------

@Composable
private fun ReceivedTabContent(
    requests: List<FriendRequestEntry>,
    onRequestClick: (FriendRequestEntry) -> Unit,
    onAccept: (FriendRequestEntry) -> Unit,
    onDecline: (FriendRequestEntry) -> Unit,
) {
    val strings = PrivChatI18n.strings

    if (requests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(message = strings.friendRequestEmpty)
        }
        return
    }

    val sorted = requests.sortedByDescending { it.createdAt }
    val (current, older) = sorted.partition { Formatter.isInCurrentLocalMonth(it.createdAt) }

    GearLazyColumn(modifier = Modifier.fillMaxSize()) {
        if (current.isNotEmpty()) {
            item { SectionHeaderRow(title = Formatter.currentLocalMonthLabel()) }
            items(current.size) { idx ->
                val r = current[idx]
                ReceivedRow(
                    request = r,
                    showDateInSubtitle = true,
                    onRowClick = { onRequestClick(r) },
                    onAccept = { onAccept(r) },
                    onDecline = { onDecline(r) },
                )
            }
        }
        if (older.isNotEmpty()) {
            item { SectionHeaderRow(title = strings.friendRequestSectionOlder) }
            items(older.size) { idx ->
                val r = older[idx]
                ReceivedRow(
                    request = r,
                    showDateInSubtitle = false,
                    onRowClick = { onRequestClick(r) },
                    onAccept = { onAccept(r) },
                    onDecline = { onDecline(r) },
                )
            }
        }
    }
}

@Composable
private fun ReceivedRow(
    request: FriendRequestEntry,
    showDateInSubtitle: Boolean,
    onRowClick: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val strings = PrivChatI18n.strings
    val colors = Theme.colors

    val displayName = request.nickname?.takeIf { it.isNotBlank() }
        ?: request.username.takeIf { it.isNotBlank() }
        ?: "User ${request.userId}"

    val subtitleBody = request.message?.takeIf { it.isNotBlank() }
        ?: sourceLabel(request.source, strings)
    val subtitle = if (showDateInSubtitle) {
        "${Formatter.friendRequestShortDate(request.createdAt)} · $subtitleBody"
    } else {
        subtitleBody
    }

    val isPending = request.status == STATUS_PENDING

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .clickable(onClick = onRowClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChatAvatar(
                url = request.avatarUrl,
                name = displayName,
                size = AvatarSizeTokens.Medium.size,
                userId = request.userId.toLong(),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = Typography.BodyLarge,
                    color = colors.foreground,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = Typography.BodySmall,
                    color = colors.mutedForeground,
                )
            }
            if (!isPending) {
                Text(
                    text = receivedStatusLabel(request.status, strings),
                    style = Typography.BodyMedium,
                    color = colors.mutedForeground,
                )
            }
        }

        if (isPending) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    text = strings.friendRequestDecline,
                    onClick = onDecline,
                    theme = ButtonTheme.DEFAULT,
                    type = ButtonType.FILL,
                    size = ButtonSize.SMALL,
                    shape = ButtonShape.ROUND,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    text = strings.friendRequestAccept,
                    onClick = onAccept,
                    theme = ButtonTheme.PRIMARY,
                    type = ButtonType.FILL,
                    size = ButtonSize.SMALL,
                    shape = ButtonShape.ROUND,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Sent tab
// ---------------------------------------------------------------------------

@Composable
private fun SentTabContent(
    requests: List<FriendRequestEntry>,
    onRequestClick: (FriendRequestEntry) -> Unit,
    onRecall: (FriendRequestEntry) -> Unit,
) {
    val strings = PrivChatI18n.strings

    if (requests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(message = strings.friendRequestSentEmpty)
        }
        return
    }

    val sorted = requests.sortedByDescending { it.updatedAt }

    GearLazyColumn(modifier = Modifier.fillMaxSize()) {
        items(sorted.size) { idx ->
            val r = sorted[idx]
            SentRow(
                request = r,
                onRowClick = { onRequestClick(r) },
                onRecall = { onRecall(r) },
            )
        }
    }
}

@Composable
private fun SentRow(
    request: FriendRequestEntry,
    onRowClick: () -> Unit,
    onRecall: () -> Unit,
) {
    val strings = PrivChatI18n.strings
    val colors = Theme.colors

    val displayName = request.nickname?.takeIf { it.isNotBlank() }
        ?: request.username.takeIf { it.isNotBlank() }
        ?: "User ${request.userId}"
    val subtitle = sourceLabel(request.source, strings) +
        " · ${Formatter.friendRequestRelativeShort(request.updatedAt)}"
    val isPending = request.status == STATUS_PENDING

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .clickable(onClick = onRowClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatAvatar(
            url = request.avatarUrl,
            name = displayName,
            size = AvatarSizeTokens.Medium.size,
            userId = request.userId.toLong(),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = Typography.BodyLarge,
                color = colors.foreground,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = Typography.BodySmall,
                color = colors.mutedForeground,
            )
        }
        if (isPending) {
            Button(
                text = strings.friendRequestRecall,
                onClick = onRecall,
                theme = ButtonTheme.DEFAULT,
                type = ButtonType.FILL,
                size = ButtonSize.SMALL,
                shape = ButtonShape.ROUND,
            )
        } else {
            Text(
                text = sentStatusLabel(request.status, strings),
                style = Typography.BodyMedium,
                color = colors.mutedForeground,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Shared components & helpers
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeaderRow(title: String) {
    val colors = Theme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = title,
            style = Typography.Label,
            color = colors.mutedForeground,
        )
    }
}

private fun sourceLabel(source: String?, strings: PrivChatStrings): String {
    val key = source ?: return strings.friendRequestSourceUnknown
    return strings.friendRequestSourceLabels[key] ?: strings.friendRequestSourceUnknown
}

// Received tab：accepted 用 friendRequestAdded 独立文案（"已添加" 比 "已通过" 更贴近收件人语境），
// 其他态走 status map。
private fun receivedStatusLabel(status: Short, strings: PrivChatStrings): String =
    when (status) {
        STATUS_ACCEPTED -> strings.friendRequestAdded
        else -> strings.friendRequestStatusLabels[status.toInt()].orEmpty()
    }

private fun sentStatusLabel(status: Short, strings: PrivChatStrings): String =
    strings.friendRequestStatusLabels[status.toInt()].orEmpty()
