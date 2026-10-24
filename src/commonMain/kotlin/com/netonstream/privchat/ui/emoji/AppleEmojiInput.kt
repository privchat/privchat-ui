package com.netonstream.privchat.ui.emoji

import androidx.compose.runtime.*
import com.gearui.foundation.keyboard.keyboardDismissExempt
import com.gearui.theme.Theme
import com.gearui.foundation.typography.resolveFontFamily
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.foundation.text.BasicTextField
import com.tencent.kuikly.compose.foundation.text.input.InputTransformation
import com.tencent.kuikly.compose.foundation.text.input.TextPostProcessorOutputTransformation
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.focus.FocusRequester
import com.tencent.kuikly.compose.ui.focus.focusRequester
import com.tencent.kuikly.compose.ui.focus.onFocusChanged
import com.tencent.kuikly.compose.ui.graphics.SolidColor
import com.tencent.kuikly.compose.ui.layout.onGloballyPositioned
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.semantics.testTag
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.TextUnit
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp

/** Chat's multiline native TextArea only. Output processing never edits text/selection/composition. */
@Composable
fun AppleEmojiInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxLines: Int = 8,
    minHeight: Dp = 42.dp,
    verticalPadding: Dp = 8.dp,
    lineHeight: TextUnit = 19.sp,
    autoFocus: Boolean = false,
    focusRequester: FocusRequester? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    controller: AppleEmojiEditorController? = null,
) {
    val editor = controller ?: rememberAppleEmojiEditorController(value, onValueChange)
    val state = editor.state
    var laidOut by remember { mutableStateOf(false) }
    val requester = focusRequester ?: remember { FocusRequester() }
    val colors = Theme.colors
    val shape = Theme.shapes.lg
    val transform = remember { TextPostProcessorOutputTransformation(APPLE_EMOJI_PROCESSOR) }
    val input = remember(editor) { InputTransformation {
        editor.publishNative(toString())
    } }
    LaunchedEffect(autoFocus, requester, laidOut) {
        if (autoFocus && laidOut) {
            withFrameNanos { }
            requester.requestFocus()
        }
    }
    Box(modifier.heightIn(min = minHeight).clip(shape).background(colors.background)
        .border(1.dp, colors.border, shape).keyboardDismissExempt()
        .padding(horizontal = 10.dp, vertical = verticalPadding), contentAlignment = Alignment.CenterStart) {
        BasicTextField(
            state = state,
            modifier = Modifier.fillMaxWidth().testTag(AppleEmojiTestTags.Input).focusRequester(requester)
                .onGloballyPositioned { laidOut = it.isAttached && it.size.width > 0 && it.size.height > 0 }
                .onFocusChanged { onFocusChanged?.invoke(it.isFocused) },
            singleLine = false,
            maxLines = maxLines,
            inputTransformation = input,
            outputTransformation = if (AppleEmojiUiBridge.supportsInput) transform else null,
            textStyle = TextStyle(color = colors.foreground, fontSize = 16.sp, lineHeight = lineHeight,
                fontFamily = Theme.typography.bodyMedium.resolveFontFamily()),
            cursorBrush = SolidColor(colors.primary),
        )
    }
}
