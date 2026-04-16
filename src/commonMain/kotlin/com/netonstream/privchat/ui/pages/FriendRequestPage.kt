package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.*
import om.netonstream.privchat.sdk.dto.FriendPendingEntry
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.components.ChatAvatar
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.gearui.foundation.primitives.GearLazyColumn
import com.gearui.foundation.AvatarSpecs
import com.gearui.components.navbar.NavBar
import com.gearui.components.cell.Cell
import com.gearui.components.empty.EmptyState
import com.gearui.components.swipecell.SwipeCell
import com.gearui.components.swipecell.SwipeCellAction
import com.gearui.components.swipecell.SwipeCellActionTheme
import com.gearui.components.swipecell.rememberSwipeCellState
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import kotlinx.coroutines.launch

/**
 * 好友申请页面
 *
 * @param onBack 返回回调
 * @param onRequestClick 点击申请项回调，跳转到用户详情页
 * @param onAccept 同意申请回调
 * @param onReject 拒绝申请回调
 * @param modifier Modifier
 */
@Composable
fun FriendRequestPage(
    onBack: () -> Unit,
    onRequestClick: (FriendPendingEntry) -> Unit = {},
    onAccept: suspend (ULong) -> Result<ULong> = { Result.failure(NotImplementedError()) },
    onReject: suspend (ULong) -> Result<Boolean> = { Result.failure(NotImplementedError()) },
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    val friendRequests by PrivChat.friendRequests.collectAsState()
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        // 顶部导航栏
        NavBar(
            title = strings.friendRequestTitle,
            useDefaultBack = true,
            onBackClick = onBack,
        )

        // 内容区域
        if (friendRequests.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    message = strings.friendRequestEmpty,
                )
            }
        } else {
            // 好友申请列表
            GearLazyColumn(modifier = Modifier.fillMaxSize()) {
                items(friendRequests.size) { index ->
                    val request = friendRequests[index]

                    FriendRequestItem(
                        request = request,
                        onClick = { onRequestClick(request) },
                        onAccept = {
                            scope.launch {
                                onAccept(request.fromUserId)
                            }
                        },
                        onReject = {
                            scope.launch {
                                onReject(request.fromUserId)
                            }
                        },
                    )
                }
            }
        }
    }
}

/**
 * 单个好友申请项
 */
@Composable
private fun FriendRequestItem(
    request: FriendPendingEntry,
    onClick: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    val strings = PrivChatI18n.strings
    val swipeCellState = rememberSwipeCellState()

    // 显示名称：昵称优先 > 用户名 > "User {id}"
    val displayName = request.user.nickname.takeIf { it.isNotBlank() }
        ?: request.user.username.takeIf { it.isNotBlank() }
        ?: "User ${request.fromUserId}"

    // 右滑操作 - 同意和拒绝
    val rightActions = listOf(
        SwipeCellAction(
            label = strings.friendRequestAccept,
            theme = SwipeCellActionTheme.PRIMARY,
            onClick = onAccept,
        ),
        SwipeCellAction(
            label = strings.friendRequestReject,
            theme = SwipeCellActionTheme.DANGER,
            onClick = onReject,
        ),
    )

    SwipeCell(
        state = swipeCellState,
        rightActions = rightActions,
    ) {
        Cell(
            onClick = onClick,
            leading = {
                // 头像 - 使用用户名称生成默认头像
                ChatAvatar(
                    url = request.user.avatarUrl,
                    name = displayName,
                    size = AvatarSpecs.Size.medium,
                )
            },
            title = displayName,
            description = request.message ?: strings.friendRequestMessage,
            note = formatRequestTime(request.createdAt),
        )
    }
}

/**
 * 格式化申请时间
 */
private fun formatRequestTime(createdAt: ULong): String {
    // createdAt 使用 Unix 毫秒时间戳，UI 先展示秒级时间戳，后续可替换为本地化日期格式。
    val seconds = createdAt / 1000u
    return seconds.toString()
}
