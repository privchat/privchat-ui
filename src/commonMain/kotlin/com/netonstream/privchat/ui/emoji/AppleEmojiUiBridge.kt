package com.netonstream.privchat.ui.emoji

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.extension.textPostProcessor
import com.tencent.kuikly.compose.ui.Modifier

object AppleEmojiUiBridge {
    var catalog by mutableStateOf(AppleEmojiCatalog.Empty)
        private set
    var supportsInput by mutableStateOf(false)
        private set

    /** Parent registers native assets/processor first, then installs its generated manifest data. */
    fun configure(catalog: AppleEmojiCatalog, supportsInput: Boolean = true) {
        this.catalog = catalog
        this.supportsInput = supportsInput
    }
}

/** Native post-processing changes only glyphs; source Unicode and annotated-link offsets stay intact. */
fun Modifier.appleEmoji(): Modifier = textPostProcessor(APPLE_EMOJI_PROCESSOR)
