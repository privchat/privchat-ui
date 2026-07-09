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
import com.gearui.components.navbar.NavBar
import com.gearui.components.cell.Cell
import com.gearui.components.checkbox.Checkbox
import com.gearui.components.checkbox.CheckboxSize
import com.gearui.components.empty.EmptyState
import com.gearui.components.input.Input
import com.gearui.components.input.InputSize
import com.gearui.components.searchbar.SearchBar
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** 群成员上限（含自己）。微信/Telegram 普通群一般 200/500，这里按客户端选择阶段限 50。 */
private const val GROUP_CREATE_MAX_MEMBERS = 49

/**
 * 群创建页：多选好友 + 可选群名输入 + "创建"按钮。
 *
 * - 不输群名时调用方应基于成员昵称自动拼一个（"张三、李四、王五"）
 * - [onCreate] 返回成功后由调用方导航到新会话；失败时显示 [onError]
 * - 列表头部固定一条搜索栏 + "已选 N/[GROUP_CREATE_MAX_MEMBERS]" 计数
 */
@Composable
fun GroupCreatePage(
    friends: List<FriendEntry>,
    onBack: () -> Unit,
    onCreate: suspend (name: String, memberIds: List<ULong>) -> Result<Unit>,
    onError: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = Theme.colors

    var groupName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    val selected = remember { mutableStateMapOf<ULong, FriendEntry>() }
    var isCreating by remember { mutableStateOf(false) }

    val filtered = remember(friends, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) friends
        else friends.filter {
            (it.remark?.contains(q, ignoreCase = true) == true) ||
                (it.nickname?.contains(q, ignoreCase = true) == true) ||
                it.username.contains(q, ignoreCase = true)
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val canCreate = selected.isNotEmpty() && !isCreating

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        NavBar(
            title = "创建群聊",
            useDefaultBack = true,
            onBackClick = onBack,
            rightWidgetWidth = 96.dp,
            rightWidget = {
                val label = if (selected.isEmpty()) "创建" else "创建(${selected.size})"
                val color = if (canCreate) colors.primary else colors.mutedForeground
                Box(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .clickable(enabled = canCreate) {
                            val ids = selected.values.map { it.userId }
                            val displayName = groupName.trim().ifEmpty {
                                selected.values.joinToString("、") { it.displayName }.take(40)
                            }
                            isCreating = true
                            coroutineScope.launch {
                                onCreate(displayName, ids).fold(
                                    onSuccess = { isCreating = false },
                                    onFailure = { e ->
                                        isCreating = false
                                        onError(e.message ?: "创建失败")
                                    },
                                )
                            }
                        },
                ) {
                    Text(
                        text = label,
                        style = Typography.BodyMedium,
                        color = color,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            },
        )

        // 群名输入 + 搜索栏 + 计数提示，三段固定在列表上方
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Input(
                value = groupName,
                onValueChange = { groupName = it },
                placeholder = "群名称（选填，留空将自动生成）",
                size = InputSize.LARGE,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        SearchBar(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "搜索好友",
            shape = com.gearui.components.searchbar.SearchBarShape.SQUARE,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "已选 ${selected.size}/$GROUP_CREATE_MAX_MEMBERS",
                style = Typography.BodySmall,
                color = colors.mutedForeground,
            )
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(message = if (friends.isEmpty()) "暂无好友可加入" else "未匹配到好友")
            }
        } else {
            GearLazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered.size) { idx ->
                    val friend = filtered[idx]
                    val isSelected = selected.containsKey(friend.userId)
                    val atLimit = selected.size >= GROUP_CREATE_MAX_MEMBERS && !isSelected
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
                                onError("最多选择 $GROUP_CREATE_MAX_MEMBERS 位好友")
                            }
                        },
                    )
                }
            }
        }
    }
}
