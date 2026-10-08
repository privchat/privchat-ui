package com.netonstream.privchat.ui.platform

import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.module.SharedPreferencesModule

// HarmonyOS actuals for the platform bridges. Clipboard, haptics, the message alert,
// system bars and opening links need ArkTS host APIs that are not bridged yet; until
// they are these do nothing and report failure where the contract allows it.

actual object HapticBridge {
    actual fun selectionChanged() {}
}

actual object SystemChromeController {
    actual fun setSystemBarsHidden(hidden: Boolean) {}
}

actual object ClipboardBridge {
    actual fun setText(text: String) {}
}

actual object MessageAlertController {
    actual fun playIncomingMessageAlert(sound: Boolean, vibration: Boolean) {}
}

actual object ExternalLinkBridge {
    actual fun openUri(uri: String): Boolean = false

    actual fun openMap(latitude: Double, longitude: Double, coordinateSystem: String?, label: String?): Boolean = false
}

/**
 * Drafts through Kuikly's SharedPreferences module, which the ohos render implements.
 * The module cannot enumerate keys, so all drafts live under one key as
 * `channelId RS encodedDraft` records separated by GS.
 */
actual object DraftStore {
    private const val KEY = "privchat.drafts"
    private const val RECORD_SEP = '\u001D'
    private const val ID_SEP = '\u001E'

    private fun prefs(): SharedPreferencesModule? = runCatching {
        PagerManager.getCurrentPager()
            .acquireModule<SharedPreferencesModule>(SharedPreferencesModule.MODULE_NAME)
    }.getOrNull()

    private fun readAll(): MutableMap<ULong, String> {
        val raw = prefs()?.getString(KEY).orEmpty()
        val out = LinkedHashMap<ULong, String>()
        if (raw.isEmpty()) return out
        for (record in raw.split(RECORD_SEP)) {
            val sep = record.indexOf(ID_SEP)
            if (sep <= 0) continue
            val id = record.substring(0, sep).toULongOrNull() ?: continue
            out[id] = record.substring(sep + 1)
        }
        return out
    }

    actual fun loadAll(): Map<ULong, PersistedDraft> = readAll().mapValues { decodeDraft(it.value) }

    actual fun save(channelId: ULong, draft: PersistedDraft?) {
        val all = readAll()
        if (isEmpty(draft)) all.remove(channelId) else all[channelId] = encodeDraft(draft!!)
        prefs()?.setString(KEY, all.entries.joinToString(RECORD_SEP.toString()) { "${it.key}$ID_SEP${it.value}" })
    }
}
