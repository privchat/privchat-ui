package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.*
import com.netonstream.privchat.sdk.dto.FriendEntry
import com.netonstream.privchat.ui.components.ChatAvatar
import com.netonstream.privchat.ui.models.displayName
import com.gearui.theme.Theme
import com.gearui.foundation.avatar.AvatarSizeTokens
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.primitives.GearLazyColumn
import com.gearui.foundation.typography.Typography
import com.gearui.components.cell.Cell
import com.gearui.components.checkbox.Checkbox
import com.gearui.components.checkbox.CheckboxSize
import com.gearui.components.empty.EmptyState
import com.gearui.components.navbar.NavBar
import com.gearui.components.searchbar.SearchBar
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
import kotlinx.coroutines.launch

private const val GROUP_INVITE_MAX_BATCH = 49

/**
 * 群邀请页（多选）：选好友打勾，点右上"邀请"批量提交。
 *
 * - 不在群里的好友才会出现在列表中（调用方过滤）
 * - 调用 [onInvite] 后由调用方处理 SDK + 失败展示
 */
@Composable
fun GroupInvitePage(
    friends: List<FriendEntry>,
    onBack: () -> Unit,
    onInvite: suspend (List<FriendEntry>) -> Result<Unit>,
    onError: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = Theme.colors
    var searchQuery by remember { mutableStateOf("") }
    val selected = remember { mutableStateMapOf<ULong, FriendEntry>() }
    var isSubmitting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val filtered = remember(friends, searchQuery) {
        val q = searchQuery.trim()
        // 系统类型账号(user_type==1)绝不允许被拉进群——按 user_type(非 uid)过滤;
        // 服务端 add/create 也已硬拒,这里是客户端侧的等价限制。
        val invitable = friends.filterNot {
            com.netonstream.privchat.ui.models.SystemUser.isSystemType(it.userType.toInt())
        }
        if (q.isEmpty()) invitable
        else invitable.filter {
            (it.remark?.contains(q, ignoreCase = true) == true) ||
                (it.nickname?.contains(q, ignoreCase = true) == true) ||
                it.username.contains(q, ignoreCase = true)
        }
    }

    val canSubmit = selected.isNotEmpty() && !isSubmitting

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        NavBar(
            title = "邀请好友",
            useDefaultBack = true,
            onBackClick = onBack,
            rightWidgetWidth = com.netonstream.privchat.ui.components.NavBarActionSlotWidthWide,
            rightWidget = {
                val label = if (selected.isEmpty()) "邀请" else "邀请(${selected.size})"
                com.netonstream.privchat.ui.components.NavBarAction(
                    text = label,
                    enabled = canSubmit,
                    loading = isSubmitting,
                ) {
                    isSubmitting = true
                    coroutineScope.launch {
                        onInvite(selected.values.toList()).fold(
                            onSuccess = { isSubmitting = false },
                            onFailure = { e ->
                                isSubmitting = false
                                onError(com.netonstream.privchat.ui.error.UserFacingError.message(e, "邀请失败"))
                            },
                        )
                    }
                }
            },
        )

        SearchBar(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "搜索好友",
            shape = com.gearui.components.searchbar.SearchBarShape.SQUARE,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "已选 ${selected.size}/$GROUP_INVITE_MAX_BATCH",
                style = Typography.BodySmall,
                color = colors.mutedForeground,
            )
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(message = if (friends.isEmpty()) "暂无可邀请好友" else "未匹配到好友")
            }
        } else {
            GearLazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered.size) { idx ->
                    val friend = filtered[idx]
                    val isSelected = selected.containsKey(friend.userId)
                    val atLimit = selected.size >= GROUP_INVITE_MAX_BATCH && !isSelected
                    Cell(
                        title = friend.displayName,
                        description = friend.username,
                        leading = {
                            ChatAvatar(
                                url = friend.avatarUrl,
                                name = friend.displayName,
                                size = AvatarSizeTokens.Small.size,
                                userId = friend.userId.toLong(),
                            )
                        },
                        trailing = {
                            Checkbox(
                                checked = isSelected,
                                size = CheckboxSize.MEDIUM,
                                enabled = !atLimit,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (!atLimit) selected[friend.userId] = friend
                                    } else {
                                        selected.remove(friend.userId)
                                    }
                                },
                            )
                        },
                        onClick = {
                            if (isSelected) {
                                selected.remove(friend.userId)
                            } else if (!atLimit) {
                                selected[friend.userId] = friend
                            } else {
                                onError("最多选择 $GROUP_INVITE_MAX_BATCH 位好友")
                            }
                        },
                    )
                }
            }
        }
    }
}
