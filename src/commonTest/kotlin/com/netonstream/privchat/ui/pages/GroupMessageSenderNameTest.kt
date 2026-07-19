package com.netonstream.privchat.ui.pages

import com.netonstream.privchat.sdk.dto.GroupMemberEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupMessageSenderNameTest {
    private fun member(name: String, remark: String = "") = GroupMemberEntry(
        userId = 42u,
        channelId = 100u,
        channelType = 2,
        name = name,
        remark = remark,
        avatar = "",
        role = 0,
        status = 1,
        inviteUserId = 0u,
    )

    @Test
    fun groupRemarkTakesPrecedenceOverMemberName() {
        assertEquals("群内备注", resolveGroupMessageSenderName(42u, listOf(member("用户昵称", "群内备注"))))
    }

    @Test
    fun memberNameIsShownWhenRemarkIsBlank() {
        assertEquals("用户昵称", resolveGroupMessageSenderName(42u, listOf(member("用户昵称"))))
    }

    @Test
    fun unknownMemberFallsBackToUid() {
        assertEquals("99", resolveGroupMessageSenderName(99u, listOf(member("用户昵称"))))
    }
}
