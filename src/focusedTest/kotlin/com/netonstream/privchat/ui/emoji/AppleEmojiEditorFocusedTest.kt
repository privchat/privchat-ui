package com.netonstream.privchat.ui.emoji

import com.tencent.kuikly.compose.foundation.text.input.TextFieldState
import com.tencent.kuikly.compose.foundation.text.input.TextPostProcessorOutputTransformation
import com.tencent.kuikly.compose.ui.text.TextRange
import kotlin.test.*

/** Real Kuikly state/buffer APIs; reflection only bypasses its internal state constructor on JVM. */
class AppleEmojiEditorFocusedTest {
    private fun state(text: String, cursor: Int = text.length): TextFieldState {
        val constructor = TextFieldState::class.java.declaredConstructors.single { it.parameterCount == 3 }
        constructor.isAccessible = true
        return constructor.newInstance(text, (cursor.toLong() shl 32) or cursor.toLong(), null) as TextFieldState
    }

    @Test fun nativeCallbacksDeduplicateBeforeRecompositionAndAllowReturningToPreviousText() {
        val published = mutableListOf<String>()
        val editor = AppleEmojiEditorController(state(""), "", published::add)
        editor.publishNative("a"); editor.publishNative("a")
        editor.publishNative("ab"); editor.publishNative("ab")
        editor.publishNative("a")
        assertEquals(listOf("a", "ab", "a"), published)
    }

    @Test fun panelInsertsAtCaretAndReplacesReversedSelectionWithoutChangingEmoji() {
        val published = mutableListOf<String>()
        val state = state("abcd", 2)
        val editor = AppleEmojiEditorController(state, state.text, published::add)
        assertTrue(editor.insert("👩🏽‍💻"))
        assertEquals("ab👩🏽‍💻cd", state.text)
        assertEquals(TextRange(2 + "👩🏽‍💻".length), state.selection)
        state.setTextAndSelect("abcd", TextRange(3, 1))
        assertTrue(editor.insert("🇸🇬"))
        assertEquals("a🇸🇬d", state.text)
        assertEquals(TextRange(5), state.selection)
        assertEquals(listOf("ab👩🏽‍💻cd", "a🇸🇬d"), published)
    }

    @Test fun panelDeletesBeforeCaretOrSelectionNotAtEnd() {
        val raw = "ab👩🏽‍💻cd"
        val state = state(raw, 2 + "👩🏽‍💻".length)
        val editor = AppleEmojiEditorController(state, raw) { }
        assertTrue(editor.deleteBackward())
        assertEquals("abcd", state.text)
        assertEquals(TextRange(2), state.selection)
        state.setTextAndSelect("abcd", TextRange(3, 1))
        assertTrue(editor.deleteBackward())
        assertEquals("ad", state.text)
        assertEquals(TextRange(1), state.selection)
    }

    @Test fun panelNeverCancelsMarkedCompositionOrQueuesDelayedEdits() {
        val published = mutableListOf<String>()
        val state = state("ni中", 2)
        state.setTextAndSelect(state.text, TextRange(2), TextRange(0, 2))
        val editor = AppleEmojiEditorController(state, state.text, published::add)
        assertFalse(editor.insert("😀")); assertFalse(editor.deleteBackward())
        assertEquals("ni中", state.text)
        assertEquals(TextRange(0, 2), state.composition)
        assertTrue(published.isEmpty())
        state.setTextAndSelect(state.text, state.selection, null)
        assertEquals("ni中", state.text)
        assertTrue(editor.insert("😀"))
        assertEquals("ni😀中", state.text)
    }

    @Test fun controlledEchoDoesNotResetNativeCaretOrMarkedTextButExternalClearDoes() {
        val state = state("", 0)
        val editor = AppleEmojiEditorController(state, "") { }
        state.setTextAndSelect("abc", TextRange(1), TextRange(0, 2))
        editor.publishNative("abc")
        editor.synchronize("abc")
        assertEquals(TextRange(1), state.selection)
        assertEquals(TextRange(0, 2), state.composition)
        editor.synchronize("")
        assertEquals("", state.text)
        assertEquals(TextRange.Zero, state.selection)
        assertNull(state.composition)
    }

    @Test fun outputProcessorPreservesRawUnknownUnicodeSelectionAndComposition() {
        val raw = "a👩🏽‍💻🇸🇬1️⃣❤︎🫩z"
        val state = state(raw, 1)
        state.setTextAndSelect(raw, TextRange(1), TextRange(0, 1))
        val output = TextPostProcessorOutputTransformation(APPLE_EMOJI_PROCESSOR)
        state.edit { with(output) { transformOutput() } }
        assertEquals(raw, state.text)
        assertEquals(TextRange(1), state.selection)
        assertEquals(TextRange(0, 1), state.composition)
    }

    @Test fun programmaticCaretInsideSurrogateDoesNotSplitIt() {
        val state = state("a😀b", 2)
        val editor = AppleEmojiEditorController(state, state.text) { }
        editor.insert("1️⃣")
        assertEquals("a😀1️⃣b", state.text)
    }

    @Test fun sameValueExternalClearBeforeNativeEchoDoesNotLeaveStaleText() {
        val state = state("")
        val published = mutableListOf<String>()
        val editor = AppleEmojiEditorController(state, "", published::add)
        state.setTextAndPlaceCursorAtEnd("draft")
        editor.publishNative("draft")
        editor.synchronize("") // send/reset can happen before the parent echoes "draft".
        assertEquals("", state.text)
        assertEquals(TextRange.Zero, state.selection)
        editor.publishNative("next"); editor.publishNative("next")
        assertEquals(listOf("draft", "next"), published)
    }
}
