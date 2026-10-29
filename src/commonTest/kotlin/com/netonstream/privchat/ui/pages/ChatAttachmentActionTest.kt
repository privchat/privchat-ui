package com.netonstream.privchat.ui.pages

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatAttachmentActionTest {
    @Test
    fun everyAttachmentDismissesBeforeDispatch() {
        val actions = listOf("album", "camera", "call", "location", "redPacket", "transfer", "file", "contact")
        for (name in actions) {
            var panelOpen = true
            val events = mutableListOf<String>()
            dispatchChatAttachmentAction(
                dismiss = { panelOpen = false; events += "dismiss" },
                action = {
                    assertFalse(panelOpen, name)
                    events += name
                },
            )
            assertEquals(listOf("dismiss", name), events)
        }
    }

    @Test
    fun failedActionDoesNotUndoDismissal() {
        var panelOpen = true
        assertFailsWith<IllegalStateException> {
            dispatchChatAttachmentAction(
                dismiss = { panelOpen = false },
                action = { error("Picker unavailable") },
            )
        }
        assertFalse(panelOpen)
    }

    @Test
    fun failedDismissalNeverDispatches() {
        var dispatched = false
        assertFailsWith<IllegalStateException> {
            dispatchChatAttachmentAction(
                dismiss = { error("Dismiss failed") },
                action = { dispatched = true },
            )
        }
        assertFalse(dispatched)
    }

    @Test
    fun repeatedOpenAndDispatchDismissesEachTime() {
        var dismissals = 0
        var dispatched = 0
        repeat(2) {
            var panelOpen = true
            dispatchChatAttachmentAction(
                dismiss = { assertTrue(panelOpen); panelOpen = false; dismissals++ },
                action = { assertFalse(panelOpen); dispatched++ },
            )
        }
        assertEquals(2, dismissals)
        assertEquals(2, dispatched)
    }
}
