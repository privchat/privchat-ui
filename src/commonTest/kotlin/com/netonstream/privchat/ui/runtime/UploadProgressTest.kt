package com.netonstream.privchat.ui.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * 附件上传进度 store。
 *
 * 🔴 这几条盯的是「进度条什么时候该消失」。不消失的后果是用户看到一个永远停在 37% 的
 * 气泡：他分不清「还在传」和「早就停了」，于是既不重试也不放弃——而那正是断点续传
 * 想要消除的那种卡死感。
 */
class UploadProgressTest {

    @Test
    fun 进度按已确认字节推进_传完即摘除() {
        val id = "1001"
        ClientRuntime.onAttachmentUploadFinished(id) // 起点干净

        ClientRuntime.onAttachmentUploadProgress(id, 0, 400)
        assertEquals(0f, ClientRuntime.uploads.value.fractionOf(id))

        ClientRuntime.onAttachmentUploadProgress(id, 100, 400)
        assertEquals(0.25f, ClientRuntime.uploads.value.fractionOf(id))

        ClientRuntime.onAttachmentUploadProgress(id, 400, 400)
        assertNull(
            ClientRuntime.uploads.value.fractionOf(id),
            "传完就该摘掉，否则进度条永远挂在气泡上",
        )
    }

    @Test
    fun 发送以失败告终时进度条必须消失() {
        val id = "1002"
        ClientRuntime.onAttachmentUploadProgress(id, 50, 400)
        assertNotNull(ClientRuntime.uploads.value.fractionOf(id))

        ClientRuntime.onAttachmentUploadFinished(id)
        assertNull(ClientRuntime.uploads.value.fractionOf(id))
    }

    @Test
    fun 两个附件同时上传时进度不会画到对方身上() {
        ClientRuntime.onAttachmentUploadProgress("2001", 30, 100)
        ClientRuntime.onAttachmentUploadProgress("2002", 90, 100)
        assertEquals(0.3f, ClientRuntime.uploads.value.fractionOf("2001"))
        assertEquals(0.9f, ClientRuntime.uploads.value.fractionOf("2002"))

        ClientRuntime.onAttachmentUploadFinished("2001")
        assertNull(ClientRuntime.uploads.value.fractionOf("2001"))
        assertEquals(
            0.9f,
            ClientRuntime.uploads.value.fractionOf("2002"),
            "摘掉一个不该影响另一个",
        )
    }

    @Test
    fun 总量为零时不报进度() {
        // 总量未知（0）时给不出有意义的百分比，画一个 0% 或 100% 都是骗人。
        ClientRuntime.onAttachmentUploadProgress("3001", 0, 0)
        assertNull(ClientRuntime.uploads.value.fractionOf("3001"))
    }

    @Test
    fun 没有在上传的消息不报进度() {
        assertNull(ClientRuntime.uploads.value.fractionOf("不存在"))
    }
}
