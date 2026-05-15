package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.*
import com.netonstream.privchat.sdk.dto.FriendPendingEntry
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.components.ChatAvatar
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.netonstream.privchat.ui.utils.Formatter
import com.gearui.theme.Theme
import com.gearui.foundation.AvatarSpecs
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

/**
 * 好友申请页。
 *
 * 顶部 Tabs 切换【收到的 / 我发送的】两个面板。
 *
 * - **收到的**：分两段——当前 local 月份的申请（标题 "YYYY 年 M 月"）+ 更早
 *   （标题 "更早"）。每行显示头像 + 名字 + 副标题（含申请附言或默认 "想加你为
 *   好友"），右侧是【拒绝】【接受】两个按钮；行整体点击仍然跳用户资料页保留
 *   旧入口（兼容老 [onRequestClick] 行为）。已被同意的行（user.isFriend）显
 *   示灰色"已添加"占位，按钮区移除。
 * - **我发送的**：v1 暂未接 SDK（缺 friend/sent_list RPC），显示 EmptyState
 *   占位。后续补 RPC 后再接数据 + Recall 按钮。
 *
 * @param onAccept 接受好友申请回调（点击行内"接受"按钮）
 * @param onDecline 拒绝好友申请回调（点击行内"拒绝"按钮）
 * @param onRequestClick 点击整行回调（跳用户资料页，与旧版兼容）
 */
@Composable
fun FriendRequestPage(
    onBack: () -> Unit,
    onRequestClick: (FriendPendingEntry) -> Unit = {},
    onAccept: (FriendPendingEntry) -> Unit = {},
    onDecline: (FriendPendingEntry) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    val friendRequests by PrivChat.friendRequests.collectAsState()

    var selectedTab by remember { mutableStateOf(TAB_RECEIVED) }

    // 截图风格：tab label 带 count，如 "收到的 4"
    val receivedLabel = "${strings.friendRequestTabReceived} ${friendRequests.size}"
    val sentLabel = "${strings.friendRequestTabSent} 0"

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
                requests = friendRequests,
                onRequestClick = onRequestClick,
                onAccept = onAccept,
                onDecline = onDecline,
            )
            TAB_SENT -> SentTabContent()
        }
    }
}

@Composable
private fun ReceivedTabContent(
    requests: List<FriendPendingEntry>,
    onRequestClick: (FriendPendingEntry) -> Unit,
    onAccept: (FriendPendingEntry) -> Unit,
    onDecline: (FriendPendingEntry) -> Unit,
) {
    val strings = PrivChatI18n.strings

    if (requests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(message = strings.friendRequestEmpty)
        }
        return
    }

    // 按"当前月 vs 更早"分两段；列表内仍按 createdAt DESC 排序
    val sorted = requests.sortedByDescending { it.createdAt }
    val (current, older) = sorted.partition { Formatter.isInCurrentLocalMonth(it.createdAt) }

    GearLazyColumn(modifier = Modifier.fillMaxSize()) {
        if (current.isNotEmpty()) {
            item { SectionHeaderRow(title = Formatter.currentLocalMonthLabel()) }
            items(current.size) { idx ->
                val r = current[idx]
                FriendRequestReceivedRow(
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
                FriendRequestReceivedRow(
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
private fun SentTabContent() {
    val strings = PrivChatI18n.strings
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(message = strings.friendRequestSentEmpty)
    }
}

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
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun FriendRequestReceivedRow(
    request: FriendPendingEntry,
    showDateInSubtitle: Boolean,
    onRowClick: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val strings = PrivChatI18n.strings
    val colors = Theme.colors

    val displayName = request.user.nickname.takeIf { it.isNotBlank() }
        ?: request.user.username.takeIf { it.isNotBlank() }
        ?: "User ${request.fromUserId}"
    val alreadyFriend = request.user.isFriend

    // 副标题：date · message-or-fallback（当月） / message-or-fallback（更早）
    val subtitleBody = request.message?.takeIf { it.isNotBlank() }
        ?: strings.friendRequestSourceUnknown
    val subtitle = if (showDateInSubtitle) {
        "${Formatter.friendRequestShortDate(request.createdAt)} · $subtitleBody"
    } else {
        subtitleBody
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .clickable(onClick = if (alreadyFriend) ({ }) else onRowClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChatAvatar(
                url = request.user.avatarUrl,
                name = displayName,
                size = AvatarSpecs.Size.medium,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = Typography.BodyLarge,
                    color = colors.textPrimary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = Typography.BodySmall,
                    color = colors.textSecondary,
                )
            }
            if (alreadyFriend) {
                Text(
                    text = strings.friendRequestAdded,
                    style = Typography.BodyMedium,
                    color = colors.textSecondary,
                )
            }
        }

        if (!alreadyFriend) {
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
