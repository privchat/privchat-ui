package com.netonstream.privchat.ui.platform

import platform.Foundation.NSUserDefaults

actual object DraftStore {

    private val defaults get() = NSUserDefaults.standardUserDefaults

    actual fun loadAll(): Map<ULong, PersistedDraft> {
        val dict = defaults.dictionaryRepresentation()
        val out = HashMap<ULong, PersistedDraft>()
        for ((rawKey, rawValue) in dict) {
            val key = rawKey as? String ?: continue
            if (!key.startsWith(DRAFT_KEY_PREFIX)) continue
            val id = key.removePrefix(DRAFT_KEY_PREFIX).toULongOrNull() ?: continue
            val value = rawValue as? String ?: continue
            out[id] = decodeDraft(value)
        }
        return out
    }

    actual fun save(channelId: ULong, draft: PersistedDraft?) {
        val key = "$DRAFT_KEY_PREFIX$channelId"
        if (isEmpty(draft)) {
            defaults.removeObjectForKey(key)
        } else {
            defaults.setObject(encodeDraft(draft!!), forKey = key)
        }
    }
}
