package com.netonstream.privchat.ui.i18n

import kotlin.test.*

class PrivChatAssistantWorkflowStringsTest {
    @Test fun conversationAndWorkflowLabelsAreWiredInFourLanguages() {
        val locales = listOf(enAgent, zhAgent, hantAgent, viAgent)
        assertEquals(4, locales.map { it.conversation.title }.distinct().size)
        locales.forEach { agent ->
            val c = agent.conversation
            listOf(c.title, c.hint, c.assign, c.review, c.target, c.retry, c.accepted, c.completed,
                c.rejected, c.running, c.waiting, c.failed, c.cancelled).forEach { assertTrue(it.isNotBlank()) }
            assertNotEquals(c.accepted, c.completed)
            assertEquals(c.accepted, c.executionState("accepted"))
            assertEquals(c.completed, c.executionState("succeeded"))
            assertEquals(c.cancelling, c.executionState("cancelling"))
            assertEquals(c.accepted, c.executionState("dispatched"))
            assertEquals(c.waiting, c.executionState("waiting_user"))
            assertEquals(c.running, c.executionState("running"))
            assertEquals(c.failed, c.executionState("failed"))
            assertEquals(c.cancelled, c.executionState("cancelled"))
            assertEquals(c.waiting, c.executionState("waiting_permission"))
            assertNull(c.executionState("future_state"))
            val w = agent.workspace
            listOf(w.activity, w.latestExecution, w.noExecution, w.noFinalResult, w.resultDetails,
                w.archivedRecords, w.normalizedRecordsOnly, w.archivePosition, w.archiveGap,
                w.publicOutput, w.userInput, w.toolSummary, w.notice, w.archivedPermission, w.loading)
                .forEach { assertTrue(it.isNotBlank()) }
            listOf(w.acceptance, w.evidence, w.assign, w.review, w.recordReview, w.reviewHint,
                w.statusOpen, w.statusInProgress, w.statusDone, w.statusCancelled, w.terminalHint).forEach { assertTrue(it.isNotBlank()) }
            assertEquals(w.statusDone, w.workStatus("done"))
            assertEquals(w.unknown, w.workStatus("future_state"))
        }
        assertFalse(zhAgent.conversation.hint.contains("Free-text"))
        assertEquals("已受理，尚未完成", zhAgent.conversation.accepted)
        assertEquals(4, locales.map { it.workspace.activity }.distinct().size)
        assertEquals(4, locales.map { it.workspace.normalizedRecordsOnly }.distinct().size)
        assertTrue(zhAgent.workspace.normalizedRecordsOnly.contains("不代表完整工具日志"))
    }
}
