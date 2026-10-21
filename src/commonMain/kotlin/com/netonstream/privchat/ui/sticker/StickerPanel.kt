package com.netonstream.privchat.ui.sticker

import androidx.compose.runtime.*
import com.gearui.components.button.Button
import com.gearui.components.button.ButtonSize
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.primitives.ScrollView
import com.gearui.theme.Theme
import com.netonstream.privchat.sdk.dto.StickerMessagePayload
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.error.UserFacingError
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.foundation.rememberScrollState
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun StickerPanel(onSend: suspend (StickerMessagePayload) -> Result<ULong>) {
    val catalog = StickerUiBridge.catalog ?: return
    val session = catalog.session ?: return
    key(session, catalog) { AccountStickerPanel(session, catalog, onSend) }
}

@Composable
private fun AccountStickerPanel(session: StickerSession, catalog: StickerCatalog, onSend: suspend (StickerMessagePayload) -> Result<ULong>) {
    val strings = PrivChatI18n.strings
    val language = PrivChatI18n.languageTag
    val scope = rememberCoroutineScope()
    val state = remember(catalog, session) { StickerPanelState(catalog, session) }
    var showStore by remember { mutableStateOf(false) }
    LaunchedEffect(catalog, session) {
        launch { state.refreshInstalled() }
        launch { state.refreshStore() }
    }
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(text = strings.stickerInstalled, size = ButtonSize.SMALL, onClick = { showStore = false })
            Button(text = strings.stickerStore, size = ButtonSize.SMALL, onClick = { showStore = true })
        }
        if (if (showStore) state.storeLoading else state.installedLoading) Text(strings.loading)
        val error = state.actionError ?: if (showStore) state.storeError else state.installedError
        if (error != null) {
            Text(UserFacingError.message(error, strings.networkError))

        }
        Button(text = strings.stickerRefresh, size = ButtonSize.SMALL, disabled = state.busy, onClick = {
            scope.launch { state.refreshInstalled() }
            scope.launch { state.refreshStore() }
        })
        val installed = state.installed
        val packs = if (showStore) state.store else installed
        val loaded = if (showStore) state.storeReady else state.installedReady
        if (loaded && packs.isEmpty()) Text(strings.noData)
        ScrollView(Modifier.weight(1f)) {
          Column(Modifier.fillMaxWidth()) {
            packs.forEach { pack ->
                val installedIndex = installed.indexOfFirst { it.packId == pack.packId }
                Text(pack.displayName(language), style = Theme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(text = if (installedIndex >= 0) strings.stickerUninstall else strings.stickerInstall,
                        size = ButtonSize.SMALL, disabled = state.busy || !state.installedReady,
                        onClick = { scope.launch { state.perform(changesInstallation = true) {
                            if (installedIndex >= 0) catalog.uninstall(pack.packId) else catalog.install(pack.packId)
                        } } })
                    if (!showStore && installedIndex > 0) {
                        Button(text = strings.stickerMoveUp, size = ButtonSize.SMALL, disabled = state.busy || !state.installedReady,
                            onClick = { scope.launch { state.perform(changesInstallation = true) {
                                val ids = installed.map { it.packId }.toMutableList()
                                ids.add(installedIndex - 1, ids.removeAt(installedIndex))
                                catalog.reorder(ids)
                                } } })
                    }
                }
                if (pack.publicationState != 2) Text(strings.stickerOffline)
                pack.stickers.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { sticker ->
                            PublishedStickerImage(url = sticker.imageUrl, label = sticker.label,
                                    modifier = Modifier.weight(1f).height(72.dp).padding(4.dp)
                                    .clickable(enabled = state.canSend(pack)) {
                                        scope.launch { state.perform {
                                            if (catalog.session != session) throw CancellationException("sticker session changed")
                                            onSend(StickerMessagePayload(sticker.stickerId, sticker.imageUrl)).getOrThrow()
                                        } }
                                    })
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
          }
        }
    }
}
