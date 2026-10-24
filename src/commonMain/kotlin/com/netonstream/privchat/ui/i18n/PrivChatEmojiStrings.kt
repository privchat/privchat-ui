package com.netonstream.privchat.ui.i18n

data class PrivChatEmojiStrings(val categoryLabels: List<String>)

fun privChatEmojiStrings(languageTag: String): PrivChatEmojiStrings {
    val language = languageTag.lowercase().replace('_', '-')
    val labels = when {
        language.startsWith("vi") -> listOf("Mặt cười và cảm xúc", "Người và cơ thể", "Thành phần", "Động vật và thiên nhiên", "Đồ ăn và thức uống", "Du lịch và địa điểm", "Hoạt động", "Đồ vật", "Biểu tượng", "Cờ", "Khác")
        language.startsWith("zh") && ("hant" in language || language.startsWith("zh-tw") || language.startsWith("zh-hk") || language.startsWith("zh-mo")) -> listOf("表情與情緒", "人物與身體", "組件", "動物與自然", "食物與飲品", "旅行與地點", "活動", "物品", "符號", "旗幟", "其他")
        language.startsWith("zh") -> listOf("表情与情绪", "人物与身体", "组件", "动物与自然", "食物与饮品", "旅行与地点", "活动", "物品", "符号", "旗帜", "其他")
        else -> listOf("Smileys and emotion", "People and body", "Components", "Animals and nature", "Food and drink", "Travel and places", "Activities", "Objects", "Symbols", "Flags", "Other")
    }
    return PrivChatEmojiStrings(labels)
}
