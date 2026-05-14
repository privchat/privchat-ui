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
import com.gearui.components.cell.Cell
import com.gearui.components.empty.EmptyState
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 好友申请页面。
 *
 * 列表项设计参考微信：每条申请右侧有一个"查看"按钮（primary 色），点击跳到
 * 用户资料页处理同意/拒绝；如果该用户已经在你的好友列表（即申请已被你或对方
 * 接受），按钮变成灰色"已添加"文案，不可点。
 *
 * @param onBack 返回回调
 * @param onRequestClick 点击行/查看按钮时的回调（跳用户资料页）
 */
@Composable
fun FriendRequestPage(
    onBack: () -> Unit,
    onRequestClick: (FriendPendingEntry) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    val friendRequests by PrivChat.friendRequests.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        NavBar(
            title = strings.friendRequestTitle,
            useDefaultBack = true,
            onBackClick = onBack,
        )

        if (friendRequests.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(message = strings.friendRequestEmpty)
            }
        } else {
            GearLazyColumn(modifier = Modifier.fillMaxSize()) {
                items(friendRequests.size) { index ->
                    val request = friendRequests[index]
                    FriendRequestItem(
                        request = request,
                        onClick = { onRequestClick(request) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendRequestItem(
    request: FriendPendingEntry,
    onClick: () -> Unit,
) {
    val strings = PrivChatI18n.strings
    val colors = Theme.colors

    // 显示名：昵称 > 用户名 > User {id}
    val displayName = request.user.nickname.takeIf { it.isNotBlank() }
        ?: request.user.username.takeIf { it.isNotBlank() }
        ?: "User ${request.fromUserId}"

    val alreadyFriend = request.user.isFriend

    Cell(
        // 已添加态：整行不可点（保留视觉但点也无作用）；待处理态：点行 = 点"查看"
        onClick = if (alreadyFriend) null else onClick,
        leading = {
            ChatAvatar(
                url = request.user.avatarUrl,
                name = displayName,
                size = AvatarSpecs.Size.medium,
            )
        },
        title = displayName,
        description = request.message ?: strings.friendRequestMessage,
        note = Formatter.conversationTime(request.createdAt),
        trailing = {
            if (alreadyFriend) {
                Text(
                    text = strings.friendRequestAdded,
                    style = Typography.BodyMedium,
                    color = colors.textSecondary,
                )
            } else {
                Box(
                    modifier = Modifier
                        .clickable(onClick = onClick)
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = strings.friendRequestView,
                        style = Typography.BodyMedium,
                        color = colors.primary,
                    )
                }
            }
        },
    )
}
