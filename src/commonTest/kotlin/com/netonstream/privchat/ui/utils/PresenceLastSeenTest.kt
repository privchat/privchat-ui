package com.netonstream.privchat.ui.utils

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 「N 分钟前在线」相对的是传入的 now：同一个 lastSeen，现在往后走，文案就得跟着变。
 * 会话页标题曾只在 presence 变化时重算，打开时算出的「刚刚在线」一挂半小时。
 */
class PresenceLastSeenTest {
    private val lastSeen = 1_790_000_000_000L

    private fun text(now: Long) = Formatter.presenceLastSeen(
        lastSeen = lastSeen,
        now = now,
        justNow = "just now",
        minutesAgo = "%d min ago",
        hoursAgo = "%d h ago",
        daysAgo = "%d d ago",
    )

    @Test
    fun underAMinuteIsJustNow() {
        assertEquals("just now", text(lastSeen + 59_000))
    }

    @Test
    fun aLastSeenAheadOfTheLocalClockIsJustNow() {
        // 本机时钟比服务端慢：差值为负，不能显示成「-1 分钟前」。
        assertEquals("just now", text(lastSeen - 90_000))
    }

    @Test
    fun theTextFollowsTheClock() {
        assertEquals("just now", text(lastSeen + 30_000))
        assertEquals("5 min ago", text(lastSeen + 5 * 60_000))
        assertEquals("2 h ago", text(lastSeen + 2 * 3_600_000))
        assertEquals("3 d ago", text(lastSeen + 3 * 86_400_000))
    }
}
