package com.netonstream.privchat.ui.emoji

import kotlin.test.Test
import kotlin.test.assertEquals

class EmojiTextEditBoundaryTest {
    @Test fun nonEmojiZwjDoesNotConsumeFollowingText() {
        assertEquals("A\u200D", "A\u200DB".dropLastEmojiCluster())
        assertEquals("prefix A\u200D", "prefix A\u200DB".dropLastEmojiCluster())
        assertEquals("A\u200DB", "A\u200DBx".dropLastEmojiCluster())
        assertEquals("", "A\u200D".dropLastEmojiCluster())
        assertEquals("\u200D", "\u200DB".dropLastEmojiCluster())
        assertEquals("A\u200D", "A\u200D\uD83D\uDE00".dropLastEmojiCluster())
        assertEquals("\uD83D\uDE00\u200D", "\uD83D\uDE00\u200DB".dropLastEmojiCluster())
    }

    @Test fun knownEmojiSequencesStillDeleteTogether() {
        val clusters = listOf(
            "\uD83D\uDC69\uD83C\uDFFD\u200D\uD83D\uDCBB",
            "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66",
            "\u2764\uFE0F\u200D\uD83D\uDD25",
            "\uD83D\uDC4D\uD83C\uDFFF", "1\uFE0F\u20E3",
            "\uD83C\uDDF8\uD83C\uDDEC",
            "\uD83C\uDFF4\uDB40\uDC67\uDB40\uDC62\uDB40\uDC65\uDB40\uDC6E\uDB40\uDC67\uDB40\uDC7F",
        )
        for (cluster in clusters) {
            assertEquals("prefix", ("prefix" + cluster).dropLastEmojiCluster())
            assertEquals(cluster, (cluster + "x").dropLastEmojiCluster())
        }
    }

    @Test fun gb11RequiresPictographsOnBothSidesAndOnlyExtendsBeforeZwj() {
        for (left in listOf("A", "1\uFE0F\u20E3", "\uD83C\uDDE8\uD83C\uDDF3", "\uD83C\uDFFD")) {
            assertEquals(left + "\u200D", (left + "\u200D\uD83D\uDE00").dropLastEmojiCluster())
        }
        val face = "\uD83D\uDE00"
        assertEquals("", (face + "\u0301\u200D" + face).dropLastEmojiCluster())
        assertEquals(face + "\u200D\u200D", (face + "\u200D\u200D" + face).dropLastEmojiCluster())
        assertEquals(face + "\u093E\u200D", (face + "\u093E\u200D" + face).dropLastEmojiCluster())
    }

    @Test fun accentsPresentationCrLfAndRegionalPairsArePreserved() {
        for (cluster in listOf("e\u0301", "\r\n", "\u2764\uFE0E", "\u2764\uFE0F")) {
            assertEquals("before", ("before" + cluster).dropLastEmojiCluster())
        }
        val pair = "\uD83C\uDDF8\uD83C\uDDEC"
        assertEquals(pair, (pair + "\uD83C\uDDE8").dropLastEmojiCluster())
        assertEquals(pair, (pair + "\uD83C\uDDE8\uD83C\uDDF3").dropLastEmojiCluster())
    }

    @Test fun malformedUtf16AndIncompleteJoinersRemainBounded() {
        assertEquals("", "".dropLastEmojiCluster())
        assertEquals("A\u200D", "A\u200D\uD800".dropLastEmojiCluster())
        assertEquals("A\u200D", "A\u200D\uDC00".dropLastEmojiCluster())
        assertEquals("A", "A\uD800".dropLastEmojiCluster())
        assertEquals("A", "A\uDC00".dropLastEmojiCluster())
        assertEquals("", "\uD83D\uDE00\u200D".dropLastEmojiCluster())
    }
}
