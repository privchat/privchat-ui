package com.netonstream.privchat.ui.components

import androidx.compose.runtime.*
import com.gearui.components.bottomsheet.BottomSheet
import com.gearui.foundation.avatar.AvatarSizeTokens
import com.gearui.foundation.layout.Spacing
import com.gearui.foundation.primitives.GearLazyColumn
import com.gearui.foundation.primitives.Text
import com.gearui.theme.Theme
import com.netonstream.privchat.sdk.dto.MessageReadUserView
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.error.UserFacingError
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.netonstream.privchat.ui.i18n.withArgs
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 一页 30 条，与服务端默认页大小一致（READ_STATUS_SPEC §6.5.7）。 */
private const val PAGE_SIZE = 30u

/**
 * 群消息已读名单弹层（READ_STATUS_SPEC §6.5）。
 *
 * 名单只在用户主动展开时才查服务端：气泡上的「已读」来自聚合水位投影，不需要
 * 知道具体是谁。这样一条群消息的常规展示是 0 次名单查询。
 *
 * 分页是**键集**的：传上一页最后一个 user_id，不是 offset。翻页途中又有人读了
 * 消息，offset 会把同一个人返回两次。
 *
 * 过期不是"空名单"：服务端到期直接拒绝，这里显示明确的过期文案。把过期渲染成
 * 空列表会让用户以为没人读过。
 */
@Composable
fun ReadReceiptsSheet(
    visible: Boolean,
    serverMessageId: ULong,
    channelId: ULong,
    onDismiss: () -> Unit,
) {
    val strings = PrivChatI18n.strings
    var readers by remember(serverMessageId) { mutableStateOf<List<MessageReadUserView>>(emptyList()) }
    var readCount by remember(serverMessageId) { mutableStateOf(0u) }
    var recipientCount by remember(serverMessageId) { mutableStateOf(0u) }
    var nextAfterUserId by remember(serverMessageId) { mutableStateOf<ULong?>(null) }
    var hasMore by remember(serverMessageId) { mutableStateOf(false) }
    var loading by remember(serverMessageId) { mutableStateOf(false) }
    var errorText by remember(serverMessageId) { mutableStateOf<String?>(null) }

    suspend fun loadPage(after: ULong) {
        if (loading) return
        loading = true
        withContext(Dispatchers.Default) {
            PrivChat.client.messageReadList(serverMessageId, channelId, after, PAGE_SIZE)
        }.fold(
            onSuccess = { page ->
                // 追加而不是替换：这是下一页，不是刷新。
                readers = if (after == 0uL) page.readers else readers + page.readers
                readCount = page.readCount
                recipientCount = page.recipientCount
                nextAfterUserId = page.nextAfterUserId
                hasMore = page.hasMore
                errorText = null
            },
            onFailure = { e ->
                // 服务端把「过期」当错误返回，文案由它决定；这里不猜原因，
                // 只在拿不到可读文案时兜底成网络错误。
                errorText = UserFacingError.message(e, strings.networkError)
            },
        )
        loading = false
    }

    LaunchedEffect(visible, serverMessageId) {
        if (visible) {
            readers = emptyList()
            loadPage(0uL)
        }
    }

    BottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = strings.readBySheetTitle,
        description = if (errorText == null) {
            strings.readBySheetSubtitle.withArgs(readCount.toInt(), recipientCount.toInt())
        } else {
            null
        },
    ) {
        val error = errorText
        if (error != null) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = error,
                    style = Theme.typography.bodyMedium,
                    color = Theme.colors.mutedForeground,
                )
            }
        } else {
            GearLazyColumn(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                items(readers.size) { index ->
                    val reader = readers[index]
                    ReaderRow(reader)
                    // 触底续拉。只在最后一行被组合时触发，且 loadPage 自带并发闸，
                    // 快速滚动不会把同一页拉两遍。
                    if (index == readers.lastIndex && hasMore) {
                        val cursor = nextAfterUserId
                        LaunchedEffect(cursor) {
                            if (cursor != null) loadPage(cursor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderRow(reader: MessageReadUserView) {
    // 资料没取到时不显示 uid：uid 不是显示名。
    val name = reader.nickname?.takeIf { it.isNotBlank() }
        ?: PrivChatI18n.strings.readByUnknownUser
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatAvatar(
            url = reader.avatarUrl,
            name = name,
            size = AvatarSizeTokens.Small.size,
            userId = reader.userId.toLong(),
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Text(
            text = name,
            style = Theme.typography.bodyMedium,
            color = Theme.colors.foreground,
        )
    }
}
