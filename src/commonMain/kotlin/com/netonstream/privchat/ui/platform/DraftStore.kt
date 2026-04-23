package com.netonstream.privchat.ui.platform

/**
 * 持久化的会话草稿（UX-9）。
 *
 * SDK 当前没有 `channel_drafts` 表，这里通过平台 KV 落地（Android: SharedPreferences；
 * iOS: NSUserDefaults）。重启 App / 冷启恢复输入框文本与回复态。
 * `PrivChat.saveDraft` 会把每次变更同步到本层。
 */
data class PersistedDraft(
    val text: String? = null,
    val replyTo: ULong? = null,
)

expect object DraftStore {
    /** 读取所有已保存的草稿。应用启动 / 登录完成时调用一次即可。 */
    fun loadAll(): Map<ULong, PersistedDraft>

    /** 保存 / 删除（传 null 或空草稿即删除）。 */
    fun save(channelId: ULong, draft: PersistedDraft?)
}

internal const val DRAFT_KEY_PREFIX = "privchat.draft."
private const val FIELD_SEP = '\u001F'

internal fun encodeDraft(draft: PersistedDraft): String {
    val text = draft.text ?: ""
    val r = draft.replyTo?.toString() ?: ""
    return "$text$FIELD_SEP$r"
}

internal fun decodeDraft(raw: String): PersistedDraft {
    val sep = raw.indexOf(FIELD_SEP)
    return if (sep < 0) {
        PersistedDraft(text = raw.ifEmpty { null })
    } else {
        val text = raw.substring(0, sep).ifEmpty { null }
        val replyTo = raw.substring(sep + 1).toULongOrNull()
        PersistedDraft(text = text, replyTo = replyTo)
    }
}

internal fun isEmpty(draft: PersistedDraft?): Boolean =
    draft == null || (draft.text.isNullOrBlank() && draft.replyTo == null)
