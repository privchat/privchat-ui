package com.netonstream.privchat.ui.emoji

const val APPLE_EMOJI_PROCESSOR = "privchat.apple-emoji.v1"

data class AppleEmojiGroup(val id: String, val label: String)

data class AppleEmojiEntry(
    val unicode: String,
    val groupId: String,
    val aliases: List<String> = emptyList(),
    val label: String = "",
)

/** Generated from the same local asset manifest as the native processor. No wire-text rewriting. */
class AppleEmojiCatalog(version: String, groups: List<AppleEmojiGroup>, entries: List<AppleEmojiEntry>) {
    val version = version
    val groups = groups.toList()
    val entries = entries.map { it.copy(aliases = it.aliases.toList()) }
    private val byUnicode = this.entries.associateBy { it.unicode }
    private val byAlias: Map<String, AppleEmojiEntry?>
    private val byGroup = this.entries.groupBy { it.groupId }

    init {
        require(this.groups.all { it.id.isNotBlank() && it.label.isNotBlank() })
        require(this.groups.map { it.id }.distinct().size == this.groups.size)
        require(byUnicode.size == this.entries.size)
        val ids = this.groups.map { it.id }.toSet()
        require(this.entries.all { it.unicode.isNotEmpty() && it.groupId in ids && it.unicode.validEmojiUtf16() && it.aliases.all { alias -> alias.isNotEmpty() && alias.validEmojiUtf16() } })
        val aliases = mutableMapOf<String, AppleEmojiEntry?>()
        for (entry in this.entries) for (alias in entry.aliases) {
            if (aliases.containsKey(alias) && aliases[alias]?.unicode != entry.unicode) aliases[alias] = null
            else if (!aliases.containsKey(alias)) aliases[alias] = entry
        }
        byAlias = aliases
    }

    fun entriesIn(groupId: String): List<AppleEmojiEntry> = byGroup[groupId].orEmpty()
    fun resolve(unicode: String): AppleEmojiEntry? = byUnicode[unicode] ?: byAlias[unicode]

    companion object {
        val Empty = AppleEmojiCatalog("", emptyList(), emptyList())

        /** Asset-generator TSV: sequence, codepoints, file, group, subgroup, name, aliases. */
        fun fromManifestChunks(version: String, chunks: List<String>): AppleEmojiCatalog {
            val groups = linkedMapOf<String, AppleEmojiGroup>()
            val entries = mutableListOf<AppleEmojiEntry>()
            for (chunk in chunks) for (line in chunk.lineSequence()) {
                if (line.isEmpty()) continue
                val fields = line.split('\t')
                require(fields.size == 7) { "Invalid emoji manifest row" }
                val group = AppleEmojiCategoryIds.fromManifestGroup(fields[3])
                groups[group] = AppleEmojiGroup(group, fields[3])
                entries += AppleEmojiEntry(fields[0], group, fields[6].split('|').filter { it.isNotEmpty() }, fields[5])
            }
            return AppleEmojiCatalog(version, groups.values.toList(), entries)
        }
    }
}

private fun String.validEmojiUtf16(): Boolean {
    var i = 0
    while (i < length) {
        val c = this[i++]
        if (c.isHighSurrogate()) {
            if (i == length || !this[i++].isLowSurrogate()) return false
        } else if (c.isLowSurrogate()) return false
    }
    return true
}
