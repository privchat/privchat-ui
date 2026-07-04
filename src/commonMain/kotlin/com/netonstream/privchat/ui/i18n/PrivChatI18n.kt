package com.netonstream.privchat.ui.i18n

import kotlin.concurrent.Volatile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.gearui.i18n.LocalFallbackLanguageTag
import com.gearui.i18n.LocalLanguageTag
import com.gearui.i18n.normalizeLanguageTag
import com.gearui.i18n.resolveLanguagePack

val LocalPrivChatStrings = staticCompositionLocalOf { PrivChatStringPacks.Chinese }

/**
 * Provider for privchat-ui's strings. Reads [LocalLanguageTag] /
 * [LocalFallbackLanguageTag] set by gearui-kit's `App` (or `I18nRoot`),
 * resolves the matching pack from [PrivChatStringPacks.builtIn], and
 * applies any [overrides] keyed by language tag.
 *
 * Apps must NOT pass languageTag here — that lives on `App(languageTag = ...)`.
 * See gearui-kit `docs/I18N_INTEGRATION.md` and SPEC 2026 §12.
 */
@Composable
fun PrivChatI18nProvider(
    overrides: Map<String, PrivChatStringsPatch> = emptyMap(),
    content: @Composable () -> Unit,
) {
    val tag = LocalLanguageTag.current
    val fallback = LocalFallbackLanguageTag.current
    val strings = remember(tag, fallback, overrides) {
        val base = resolveLanguagePack(
            languageTag = tag,
            packs = PrivChatStringPacks.builtIn,
            defaultTag = fallback,
        )
        base.merge(resolvePrivChatPatch(tag, fallback, overrides))
    }
    // Keep a non-composable snapshot so background / error paths (coroutine folds,
    // SDK callbacks) can resolve localized strings without a Composable scope.
    SideEffect { PrivChatI18n.current = strings }
    CompositionLocalProvider(LocalPrivChatStrings provides strings, content = content)
}

object PrivChatI18n {
    val strings: PrivChatStrings
        @Composable get() = LocalPrivChatStrings.current

    /**
     * Non-composable snapshot of the active strings pack, updated by
     * [PrivChatI18nProvider]. Use only where a Composable scope is unavailable
     * (e.g. [com.netonstream.privchat.ui.error.UserFacingError]); prefer [strings] in UI.
     */
    @Volatile
    var current: PrivChatStrings = PrivChatStringPacks.Chinese
        internal set

    val languageTag: String
        @Composable get() = LocalLanguageTag.current
}

private fun resolvePrivChatPatch(
    languageTag: String,
    fallbackTag: String,
    overrides: Map<String, PrivChatStringsPatch>,
): PrivChatStringsPatch? {
    if (overrides.isEmpty()) return null
    val normalized = overrides.entries.associate { normalizeLanguageTag(it.key) to it.value }

    val candidates = mutableListOf<String>()
    var current = languageTag
    while (true) {
        candidates += current
        val idx = current.lastIndexOf('-')
        if (idx <= 0) break
        current = current.substring(0, idx)
    }
    if (!candidates.contains(fallbackTag)) candidates += fallbackTag

    for (c in candidates) {
        normalized[c]?.let { if (!it.isEmpty) return it }
    }
    return null
}
