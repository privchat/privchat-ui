package com.netonstream.privchat.ui.emoji

private val bundledCatalog by lazy {
    AppleEmojiCatalog.fromManifestChunks("18.0", AppleEmojiCatalogData.chunks())
}

/** Same generated collection used by production and acceptance hosts, not a sample list. */
fun bundledAppleEmojiCatalog(): AppleEmojiCatalog = bundledCatalog

fun initializeAppleEmojiCatalog() {
    AppleEmojiUiBridge.configure(bundledAppleEmojiCatalog(), supportsInput = true)
}
