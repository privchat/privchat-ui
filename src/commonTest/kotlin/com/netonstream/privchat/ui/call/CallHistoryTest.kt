package com.netonstream.privchat.ui.call

import com.netonstream.privchat.sdk.dto.call.*
import com.netonstream.privchat.ui.i18n.*
import kotlin.test.*

class CallHistoryTest {
    @Test fun callIdentityUsesCachedNamesOrExplicitUnsignedIds() {
        assertEquals("Project group", enCall.channelLabel(41u, "Project group"))
        assertEquals("Caller: Alice", enCall.initiatorLabel(2u, "Alice"))
        assertEquals("Channel #41", enCall.channelLabel(41u, ""))
        assertEquals("发起人：用户 #2", zhCall.initiatorLabel(2u, null))
        assertEquals("Caller: User #18446744073709551615", enCall.initiatorLabel(ULong.MAX_VALUE, "-1"))
    }

    @Test fun serverSummaryProducesLocalizedTextAndDuration() {
        val raw = """{"event":"call.ended","call_id":41,"media_kind":"video","duration_ms":65000,"end_reason":"normal"}"""
        val history = assertNotNull(parseCallHistory(raw))
        assertEquals("Video call · Ended · 1:05", enCall.historySummary(history))
        assertEquals("视频通话 · 已结束 · 1:05", zhCall.historySummary(history))
        assertEquals("視訊通話 · 已結束 · 1:05", hantCall.historySummary(history))
        assertEquals("Gọi video · Đã kết thúc · 1:05", viCall.historySummary(history))
        for (reason in CallEndReason.entries) {
            val text = enCall.historySummary(history.copy(endReason = reason, durationMs = 0))
            assertFalse(text.contains("{")); assertTrue(text.endsWith("0:00"))
        }
    }

    @Test fun malformedOrUnrelatedSystemMessagesDoNotBecomeCalls() {
        assertNull(parseCallHistory("not json"))
        assertNull(parseCallHistory("""{"event":"other","call_id":41,"media_kind":"audio","duration_ms":0}"""))
        assertNull(parseCallHistory("""{"event":"call.ended","call_id":41,"media_kind":"audio","duration_ms":-1}"""))
    }
}
