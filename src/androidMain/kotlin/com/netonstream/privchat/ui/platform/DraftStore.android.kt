package com.netonstream.privchat.ui.platform

import android.content.Context
import android.content.SharedPreferences

actual object DraftStore {

    private const val PREFS_NAME = "privchat_drafts"
    private var appContext: Context? = null

    fun register(context: Context) {
        appContext = context.applicationContext
    }

    fun unregister() {
        appContext = null
    }

    private fun prefs(): SharedPreferences? =
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    actual fun loadAll(): Map<ULong, PersistedDraft> {
        val sp = prefs() ?: return emptyMap()
        val out = HashMap<ULong, PersistedDraft>()
        for ((key, value) in sp.all) {
            if (!key.startsWith(DRAFT_KEY_PREFIX)) continue
            val id = key.removePrefix(DRAFT_KEY_PREFIX).toULongOrNull() ?: continue
            val raw = value as? String ?: continue
            out[id] = decodeDraft(raw)
        }
        return out
    }

    actual fun save(channelId: ULong, draft: PersistedDraft?) {
        val sp = prefs() ?: return
        val key = "$DRAFT_KEY_PREFIX$channelId"
        sp.edit().apply {
            if (isEmpty(draft)) remove(key) else putString(key, encodeDraft(draft!!))
            apply()
        }
    }
}
