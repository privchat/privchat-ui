package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.*
import om.netonstream.privchat.sdk.dto.GroupMemberEntry
import com.netonstream.privchat.ui.components.ChatAvatar
import com.netonstream.privchat.ui.models.displayName
import com.netonstream.privchat.ui.models.roleName
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.gearui.foundation.AvatarSpecs
import com.gearui.foundation.primitives.GearLazyColumn
import com.gearui.components.cell.Cell
import com.gearui.components.empty.EmptyState
import com.gearui.components.navbar.NavBar
import com.gearui.components.navbar.NavBarItem
import com.gearui.components.icon.Icons
import com.gearui.components.swipecell.SwipeCell
import com.gearui.components.swipecell.SwipeCellAction
import com.gearui.components.swipecell.SwipeCellActionTheme
import com.gearui.components.swipecell.rememberSwipeCellState
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import kotlinx.coroutines.launch

@Composable
fun GroupMembersPage(
    groupName: String,
    members: List<GroupMemberEntry>,
    onBack: () -> Unit,
    onInviteClick: () -> Unit = {},
    onRemoveMember: suspend (GroupMemberEntry) -> Result<Unit> = { Result.success(Unit) },
    onError: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    val sorted = members.sortedWith(compareByDescending<GroupMemberEntry> { it.role }.thenBy { it.displayName })
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        NavBar(
            title = "$groupName (${sorted.size})",
            useDefaultBack = true,
            onBackClick = onBack,
            rightItems = listOf(
                NavBarItem(icon = Icons.add, onClick = onInviteClick),
            ),
        )

        if (sorted.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(message = strings.noData)
            }
        } else {
            GearLazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sorted.size) { index ->
                    val member = sorted[index]
                    val swipeState = rememberSwipeCellState()
                    SwipeCell(
                        state = swipeState,
                        rightActions = listOf(
                            SwipeCellAction(
                                label = "移除",
                                theme = SwipeCellActionTheme.DANGER,
                                onClick = {
                                    scope.launch {
                                        onRemoveMember(member).onFailure {
                                            onError?.invoke(it.message ?: strings.networkError)
                                        }
                                    }
                                },
                            ),
                        ),
                    ) {
                        Cell(
                            title = member.displayName,
                            description = "${member.roleName} · ${member.userId}",
                            leading = {
                                ChatAvatar(
                                    url = member.avatar,
                                    name = member.displayName,
                                    size = AvatarSpecs.Size.small,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
