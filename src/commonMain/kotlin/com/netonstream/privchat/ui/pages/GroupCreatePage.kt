package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.*
import com.netonstream.privchat.sdk.dto.FriendEntry
import com.netonstream.privchat.ui.components.ChatAvatar
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.netonstream.privchat.ui.i18n.withArgs
import com.netonstream.privchat.ui.models.displayName
import com.gearui.theme.Theme
import com.netonstream.privchat.ui.search.SearchField
import com.netonstream.privchat.ui.search.PeopleSearch
import com.netonstream.privchat.ui.components.SelectionDot
import com.netonstream.privchat.ui.components.HighlightedText
import com.gearui.foundation.avatar.AvatarSizeTokens
import com.gearui.foundation.primitives.Text
import com.tencent.kuikly.compose.foundation.layout.Row
import com.gearui.primitives.HorizontalSpacer
import com.gearui.foundation.primitives.GearLazyColumn
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


/**
 * 群创建页：多选好友 + 可选群名输入 + "创建"按钮。
 *
 * - 不输群名时调用方应基于成员昵称自动拼一个（"张三、李四、王五"）
 * - [onCreate] 返回成功后由调用方导航到新会话；失败时显示 [onError]
 * - 列表头部固定一条搜索栏 + "已选 N/[GROUP_INVITE_BATCH_LIMIT]" 计数
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

    // 搜索走 PeopleSearch：拼音 + 多字段 + 命中信息，与联系人页、@ 选人面板同一套规则。
    // 这里过去是三个字段各做一次 contains，搜不了拼音，也说不清命中在哪个字段。
    val results = remember(friends, searchQuery) {
        PeopleSearch.search(
            items = friends,
            query = searchQuery,
            fieldsOf = { PeopleSearch.fieldsOf(it) },
            nameOf = { it.displayName },
            tieBreaker = { it.userId },
        )
    }
    val filtered = remember(results) { results.map { it.first } }
    val hitByUser = remember(results) {
        results.mapNotNull { (f, h) -> h?.let { f.userId to it } }.toMap()
    }

    val strings = PrivChatI18n.strings
    val coroutineScope = rememberCoroutineScope()
    val canCreate = selected.isNotEmpty() && !isCreating

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        NavBar(
            title = strings.groupCreateTitle,
            useDefaultBack = true,
            onBackClick = onBack,
            rightWidgetWidth = com.netonstream.privchat.ui.components.NavBarActionSlotWidthWide,
            rightWidget = {
                val label = if (selected.isEmpty()) strings.groupCreateAction
                    else "${strings.groupCreateAction}(${selected.size})"
                com.netonstream.privchat.ui.components.NavBarAction(
                    text = label,
                    enabled = canCreate,
                    loading = isCreating,
                ) {
                    val ids = selected.values.map { it.userId }
                    val displayName = groupName.trim().ifEmpty {
                        selected.values.joinToString(strings.groupCreateNameSeparator) { it.displayName }.take(40)
                    }
                    isCreating = true
                    coroutineScope.launch {
                        onCreate(displayName, ids).fold(
                            onSuccess = { isCreating = false },
                            onFailure = { e ->
                                isCreating = false
                                onError(com.netonstream.privchat.ui.error.UserFacingError.message(e, strings.groupCreateFailed))
                            },
                        )
                    }
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
                placeholder = strings.groupCreateNamePlaceholder,
                size = InputSize.LARGE,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        SearchBar(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = strings.groupPickerSearchPlaceholder,
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
                text = strings.groupPickerSelectedCount.withArgs(selected.size, GROUP_INVITE_BATCH_LIMIT),
                style = Theme.typography.bodySmall,
                color = colors.mutedForeground,
            )
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    message = if (friends.isEmpty()) strings.groupCreateNoFriends
                    else strings.groupPickerNoMatch,
                )
            }
        } else {
            GearLazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered.size) { idx ->
                    val friend = filtered[idx]
                    val isSelected = selected.containsKey(friend.userId)
                    val atLimit = selected.size >= GROUP_INVITE_BATCH_LIMIT && !isSelected
                    val hit = hitByUser[friend.userId]
                    Cell(
                        title = friend.displayName,
                        titleContent = {
                            HighlightedText(
                                text = friend.displayName,
                                match = hit?.match?.takeIf { hit.field == SearchField.DisplayName },
                                style = Theme.typography.bodyLarge,
                            )
                        },
                        description = friend.username,
                        descriptionContent = {
                            // 命中备注/账号名时显示并高亮那一行；没命中就照旧显示账号名。
                            val sub = hit?.takeIf { it.field != SearchField.DisplayName }
                            HighlightedText(
                                text = sub?.text ?: friend.username,
                                match = sub?.match,
                                style = Theme.typography.label,
                                color = colors.mutedForeground,
                            )
                        },
                        // 选择标记在**左侧**、整行可点（微信式）。原来是行尾一个方形复选框：
                        // 选择状态属于这一行的人，标记跟在头像前面才读得顺；行尾那个还会让
                        // 视线在名字和行尾之间来回跑，多选时尤其明显。
                        leading = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SelectionDot(selected = isSelected, enabled = !atLimit)
                                HorizontalSpacer(10.dp)
                                ChatAvatar(
                                    url = friend.avatarUrl,
                                    name = friend.displayName,
                                    size = AvatarSizeTokens.Small.size,
                                    userId = friend.userId.toLong(),
                                )
                            }
                        },
                        onClick = {
                            if (isSelected) {
                                selected.remove(friend.userId)
                            } else if (!atLimit) {
                                selected[friend.userId] = friend
                            } else {
                                onError(strings.groupPickerMaxReached.withArgs(GROUP_INVITE_BATCH_LIMIT))
                            }
                        },
                    )
                }
            }
        }
    }
}
