package com.netonstream.privchat.ui.emoji

import androidx.compose.runtime.*
import com.tencent.kuikly.compose.foundation.text.input.TextFieldState
import com.tencent.kuikly.compose.foundation.text.input.rememberTextFieldState
import com.tencent.kuikly.compose.ui.text.TextRange

/** Shared by the native multiline input and picker; source Unicode is never post-processed here. */
class AppleEmojiEditorController internal constructor(
    val state: TextFieldState,
    initialValue: String,
    onValueChange: (String) -> Unit,
) {
    internal var onValueChange = onValueChange
    private var lastPublished = initialValue

    internal fun synchronize(value: String) {
        if (value == lastPublished) return
        lastPublished = value
        if (state.text != value) state.edit { replace(0, length, value) }
    }

    internal fun publishNative(value: String) {
        if (value == lastPublished) return
        lastPublished = value
        onValueChange(value)
    }

    /** Marked text belongs to the IME. Do not cancel it or queue a delayed panel edit. */
    fun insert(unicode: String): Boolean = edit { text, start, end ->
        EmojiPanelEdit(text.substring(0, start) + unicode + text.substring(end), start + unicode.length)
    }

    fun deleteBackward(): Boolean = edit { text, start, end ->
        if (start != end) EmojiPanelEdit(text.removeRange(start, end), start)
        else {
            val prefix = text.substring(0, start).dropLastEmojiCluster()
            EmojiPanelEdit(prefix + text.substring(end), prefix.length)
        }
    }

    private fun edit(operation: (String, Int, Int) -> EmojiPanelEdit): Boolean {
        if (state.composition != null) return false
        val range = emojiPanelSelection(state.text, state.selection.start, state.selection.end)
        val result = operation(state.text, range.first, range.second)
        state.edit {
            replace(0, length, result.text)
            selection = TextRange(result.cursor)
        }
        publishNative(result.text)
        return true
    }
}

private data class EmojiPanelEdit(val text: String, val cursor: Int)

// Native selections normally lie on grapheme boundaries; never split a surrogate pair even for
// a programmatic selection. Emoji-aware backspace is delegated to the shared boundary helper.
private fun emojiPanelSelection(text: String, start: Int, end: Int): Pair<Int, Int> {
    var a = minOf(start, end).coerceIn(0, text.length)
    var b = maxOf(start, end).coerceIn(0, text.length)
    fun insidePair(i: Int) = i > 0 && i < text.length && text[i - 1].isHighSurrogate() && text[i].isLowSurrogate()
    if (insidePair(a)) a += if (a == b) 1 else -1
    if (insidePair(b)) b++
    return a to b
}

@Composable
fun rememberAppleEmojiEditorController(value: String, onValueChange: (String) -> Unit): AppleEmojiEditorController {
    val state = rememberTextFieldState(value)
    val controller = remember(state) { AppleEmojiEditorController(state, value, onValueChange) }
    SideEffect { controller.onValueChange = onValueChange; controller.synchronize(value) }
    return controller
}
