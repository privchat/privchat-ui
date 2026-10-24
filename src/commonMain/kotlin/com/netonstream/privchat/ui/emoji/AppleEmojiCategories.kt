package com.netonstream.privchat.ui.emoji

/** Stable application IDs, independent from Unicode's English display headings. */
object AppleEmojiCategoryIds {
    val all = listOf("smileys-emotion", "people-body", "component", "animals-nature", "food-drink", "travel-places", "activities", "objects", "symbols", "flags")
    private val manifestNames = listOf("Smileys & Emotion", "People & Body", "Component", "Animals & Nature", "Food & Drink", "Travel & Places", "Activities", "Objects", "Symbols", "Flags")
    fun fromManifestGroup(name: String): String = manifestNames.indexOf(name).let { if (it < 0) name else all[it] }
}

data class AppleEmojiCategoryUi(val icon: String, val accessibilityLabel: String)

/** The project's four languages: English, Simplified/Traditional Chinese, Vietnamese. */
fun appleEmojiCategoryUi(groupId: String, languageTag: String): AppleEmojiCategoryUi {
    val labels = com.netonstream.privchat.ui.i18n.privChatEmojiStrings(languageTag).categoryLabels
    val index = AppleEmojiCategoryIds.all.indexOf(groupId)
    val icons = listOf("😀", "👋", "🧩", "🐻", "🍎", "🚗", "⚽", "💡", "❤️", "🏁", "😀")
    val slot = if (index < 0) 10 else index
    return AppleEmojiCategoryUi(icons[slot], labels[slot])
}
