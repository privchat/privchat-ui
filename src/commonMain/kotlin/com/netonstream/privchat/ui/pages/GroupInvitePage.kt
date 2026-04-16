package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.Composable
import om.netonstream.privchat.sdk.dto.FriendEntry
import com.netonstream.privchat.ui.components.ChatAvatar
import com.netonstream.privchat.ui.models.displayName
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.gearui.components.cell.Cell
import com.gearui.components.empty.EmptyState
import com.gearui.components.navbar.NavBar
import com.gearui.foundation.AvatarSpecs
import com.gearui.foundation.primitives.GearLazyColumn
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier

@Composable
fun GroupInvitePage(
    friends: List<FriendEntry>,
    onBack: () -> Unit,
    onInvite: (FriendEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    Column(modifier = modifier.fillMaxSize()) {
        NavBar(
            title = "邀请好友",
            useDefaultBack = true,
            onBackClick = onBack,
        )

        if (friends.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(message = "暂无可邀请好友")
            }
        } else {
            GearLazyColumn(modifier = Modifier.fillMaxSize()) {
                items(friends.size) { index ->
                    val friend = friends[index]
                    Cell(
                        title = friend.displayName,
                        description = friend.username,
                        leading = {
                            ChatAvatar(
                                url = friend.avatarUrl,
                                name = friend.displayName,
                                size = AvatarSpecs.Size.small,
                            )
                        },
                        arrow = true,
                        onClick = { onInvite(friend) },
                    )
                }
            }
        }
    }
}
