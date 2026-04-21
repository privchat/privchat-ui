package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.*
import com.netonstream.privchat.sdk.dto.GroupEntry
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.models.displayName
import com.netonstream.privchat.ui.components.ChatAvatar
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.gearui.theme.Theme
import com.gearui.foundation.primitives.GearLazyColumn
import com.gearui.foundation.AvatarSpecs
import com.gearui.components.navbar.NavBar
import com.gearui.components.navbar.NavBarItem
import com.gearui.components.icon.Icons
import com.gearui.components.cell.Cell
import com.gearui.components.empty.EmptyState
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 我的群组页面
 */
@Composable
fun GroupListPage(
    onBack: () -> Unit,
    onGroupClick: (GroupEntry) -> Unit = {},
    onCreateGroup: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    val groups by PrivChat.groups.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        NavBar(
            title = strings.contactMyGroups,
            useDefaultBack = true,
            onBackClick = onBack,
            rightItems = listOf(
                NavBarItem(icon = Icons.add, onClick = onCreateGroup)
            ),
        )

        if (groups.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(message = strings.contactGroupsEmpty)
            }
        } else {
            GearLazyColumn(modifier = Modifier.fillMaxSize()) {
                items(groups.size) { index ->
                    val group = groups[index]
                    Cell(
                        onClick = { onGroupClick(group) },
                        compact = true,
                        leading = {
                            ChatAvatar(
                                url = group.avatar.ifBlank { null },
                                name = group.displayName,
                                size = AvatarSpecs.Size.small,
                            )
                        },
                        title = group.displayName,
                        arrow = true,
                    )
                }
            }
        }
    }
}
