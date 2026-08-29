package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.*
import com.netonstream.privchat.sdk.dto.GroupApprovalItemView
import com.netonstream.privchat.ui.components.ChatAvatar
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.netonstream.privchat.ui.state.GroupApprovalStore
import com.gearui.theme.Theme
import com.gearui.foundation.avatar.AvatarSizeTokens
import com.gearui.foundation.primitives.GearLazyColumn
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.typography.Typography
import com.gearui.components.navbar.NavBar
import com.gearui.components.empty.EmptyState
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
import kotlinx.coroutines.launch

/**
 * 群入群申请审批页（P6-3-4，CLIENT_GLOBAL_STATE §26）。
 *
 * 仅群主/管理员进入（入口在群设置页 gating）。第一版最小闭环：
 * 进页 refresh → loading / empty / error → 每行 通过/拒绝 → 处理后 [GroupApprovalStore] 乐观移除，
 * BadgeState.groupRequests 随 totalPending 自动下降。无筛选/批量/历史（后续再说）。
 */
@Composable
fun GroupApprovalPage(
    channelId: ULong,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    val scope = rememberCoroutineScope()

    val approvals = GroupApprovalStore.approvals(channelId)
    val loading = GroupApprovalStore.isLoading(channelId)
    var error by remember(channelId) { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    // 进页拉取（无推送，进页/处理后刷新）。
    LaunchedEffect(channelId) {
        error = GroupApprovalStore.refresh(channelId).isFailure
    }

    Column(modifier = modifier.fillMaxSize()) {
        NavBar(title = strings.groupApprovalTitle, useDefaultBack = true, onBackClick = onBack)

        when {
            error && approvals.isEmpty() && !loading -> Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                com.netonstream.privchat.ui.components.PageError(
                    message = strings.networkError,
                    onRetry = { scope.launch { error = GroupApprovalStore.refresh(channelId).isFailure } },
                )
            }

            approvals.isEmpty() && !loading -> Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) { EmptyState(message = strings.groupApprovalEmpty) }

            else -> GearLazyColumn(modifier = Modifier.fillMaxSize()) {
                items(approvals.size) { i ->
                    val item = approvals[i]
                    ApprovalRow(
                        item = item,
                        enabled = !busy,
                        onApprove = {
                            scope.launch {
                                busy = true
                                GroupApprovalStore.handle(channelId, item.requestId, approve = true)
                                busy = false
                            }
                        },
                        onReject = {
                            scope.launch {
                                busy = true
                                GroupApprovalStore.handle(channelId, item.requestId, approve = false)
                                busy = false
                            }
                        },
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun ApprovalRow(
    item: GroupApprovalItemView,
    enabled: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    val strings = PrivChatI18n.strings
    val colors = Theme.colors
    // 第一版不额外拉 profile：显示名先用 uid 兜底（头像 local-first 走 ChatAvatar 的 userId 通道）。
    val displayName = "User ${item.userId}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChatAvatar(
                url = null,
                name = displayName,
                size = AvatarSizeTokens.Medium.size,
                userId = item.userId.toLong(),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = displayName, style = Typography.BodyLarge, color = colors.foreground)
                val msg = item.message?.takeIf { it.isNotBlank() }
                if (msg != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = msg, style = Typography.BodySmall, color = colors.mutedForeground)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                text = strings.friendRequestDecline,
                onClick = onReject,
                disabled = !enabled,
                theme = ButtonTheme.DEFAULT,
                type = ButtonType.FILL,
                size = ButtonSize.SMALL,
                shape = ButtonShape.ROUND,
                modifier = Modifier.weight(1f),
            )
            Button(
                text = strings.friendRequestAccept,
                onClick = onApprove,
                disabled = !enabled,
                theme = ButtonTheme.PRIMARY,
                type = ButtonType.FILL,
                size = ButtonSize.SMALL,
                shape = ButtonShape.ROUND,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
