package com.netonstream.privchat.ui.i18n

import com.netonstream.privchat.sdk.dto.MessageContent
import com.netonstream.privchat.sdk.dto.MessageContentKind
import com.netonstream.privchat.sdk.dto.MessageContentRef
import com.netonstream.privchat.sdk.dto.MessageEntry
import com.netonstream.privchat.sdk.dto.MessageStatus
import com.netonstream.privchat.ui.models.previewOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class GroupRenameSystemMessageTest {
    private val message = MessageEntry(
        id = 1u, serverMessageId = 2u, localMessageId = null, channelId = 3u,
        channelType = 2, fromUid = 0u, content = "", status = MessageStatus.Sent,
        timestamp = 0u, messageType = 5,
        body = MessageContent(MessageContentKind.System, "", systemTemplate = "system.group_name_changed",
            systemRefs = listOf(MessageContentRef("user", "42", "Operator"), MessageContentRef("text", text = "New <Name>"))),
    )

    private fun check(strings: PrivChatStrings, expected: String) {
        val rendered = strings.previewOf(message)
        assertEquals(expected, rendered)
        assertFalse(rendered.contains("system.group_name_changed"))
        assertFalse(rendered.contains("{0}") || rendered.contains("{1}"))
    }

    @Test fun simplifiedChineseUsesActorAndGroupName() = check(PrivChatStringPacks.Chinese, "Operator 将群名称修改为“New <Name>”")
    @Test fun traditionalChineseUsesActorAndGroupName() = check(PrivChatStringPacks.ChineseTraditional, "Operator 將群組名稱改為「New <Name>」")
    @Test fun englishUsesActorAndGroupName() = check(PrivChatStringPacks.English, "Operator renamed the group to \"New <Name>\"")
    @Test fun vietnameseUsesActorAndGroupName() = check(PrivChatStringPacks.Vietnamese, "Operator đã đổi tên nhóm thành “New <Name>”")
}
