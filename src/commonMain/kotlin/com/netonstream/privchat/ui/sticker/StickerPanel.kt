package com.netonstream.privchat.ui.sticker

import androidx.compose.runtime.*
import com.gearui.components.button.Button
import com.gearui.components.button.ButtonSize
import com.gearui.foundation.primitives.GearLazyColumn
import com.gearui.foundation.primitives.Text
import com.gearui.theme.Theme
import com.netonstream.privchat.sdk.dto.StickerMessagePayload
import com.netonstream.privchat.ui.error.UserFacingError
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.netonstream.privchat.ui.i18n.stickerPanelStrings
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.draw.clipToBounds
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.semantics.contentDescription
import com.tencent.kuikly.compose.ui.semantics.selected
import com.tencent.kuikly.compose.ui.semantics.semantics
import com.tencent.kuikly.compose.ui.semantics.testTag
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private enum class StickerPanelSection { INSTALLED, STORE, MANAGE }

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
    val panelStrings = stickerPanelStrings(language)
    val scope = rememberCoroutineScope()
    val state = remember(catalog, session) { StickerPanelState(catalog, session) }
    var section by remember { mutableStateOf(StickerPanelSection.INSTALLED) }
    LaunchedEffect(catalog, session) {
        launch { state.refreshInstalled() }
        launch { state.refreshStore() }
    }
    Column(Modifier.fillMaxSize().testTag(StickerPanelTestTags.Panel).padding(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(text = strings.stickerInstalled, modifier = Modifier.weight(1f).testTag(StickerPanelTestTags.Installed),
                size = ButtonSize.SMALL, onClick = { section = StickerPanelSection.INSTALLED })
            Button(text = strings.stickerStore, modifier = Modifier.weight(1f).testTag(StickerPanelTestTags.Store),
                size = ButtonSize.SMALL, onClick = { section = StickerPanelSection.STORE })
            Button(text = panelStrings.manage, modifier = Modifier.weight(1f).testTag(StickerPanelTestTags.Manage),
                size = ButtonSize.SMALL, onClick = { section = StickerPanelSection.MANAGE })
            Button(text = strings.stickerRefresh, modifier = Modifier.weight(1f).testTag(StickerPanelTestTags.Refresh),
                size = ButtonSize.SMALL, disabled = state.busy, onClick = {
                    scope.launch { state.refreshInstalled() }
                    scope.launch { state.refreshStore() }
                })
        }
        val isStore = section == StickerPanelSection.STORE
        if (if (isStore) state.storeLoading else state.installedLoading) Text(strings.loading, style = Theme.typography.label)
        val error = state.actionError ?: if (isStore) state.storeError else state.installedError
        if (error != null) Text(UserFacingError.message(error, strings.networkError), style = Theme.typography.label)
        when (section) {
            StickerPanelSection.INSTALLED -> {
                if (state.installedReady && state.installed.isEmpty()) Text(strings.noData)
                InstalledPackTabs(state, language)
                val pack = state.selectedPack
                if (pack != null) {
                    Text(pack.displayName(language), modifier = Modifier.testTag(StickerPanelTestTags.SelectedPack),
                        style = Theme.typography.label, maxLines = 1)
                    key(pack.packId) {
                        StickerGrid(pack, state, Modifier.weight(1f)) { sticker ->
                            scope.launch { state.perform {
                                if (catalog.session != session || !state.installedReady ||
                                    !pack.canSend(state.installed.any { it.packId == pack.packId })) {
                                    throw CancellationException("sticker installation changed")
                                }
                                onSend(StickerMessagePayload(sticker.stickerId, sticker.imageUrl)).getOrThrow()
                            } }
                        }
                    }
                }
            }
            StickerPanelSection.STORE, StickerPanelSection.MANAGE -> {
                val packs = if (isStore) state.store else state.installed
                if ((if (isStore) state.storeReady else state.installedReady) && packs.isEmpty()) Text(strings.noData)
                GearLazyColumn(Modifier.fillMaxWidth().weight(1f).clipToBounds(), avoidKeyboard = false,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(packs.size, key = { packs[it].packId }) { index ->
                        val pack = packs[index]
                        PackCard(pack, state, language, manage = !isStore,
                            onInstall = { scope.launch { state.install(pack.packId) } },
                            onUninstall = { scope.launch { state.uninstall(pack.packId) } },
                            onMove = { delta -> scope.launch { state.move(pack.packId, delta) } })
                    }
                }
            }
        }
    }
}

@Composable
private fun InstalledPackTabs(state: StickerPanelState, language: String) {
    val colors = Theme.colors
    LazyRow(Modifier.fillMaxWidth().height(60.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items(state.installed.size, key = { state.installed[it].packId }) { index ->
            val pack = state.installed[index]
            val isSelected = pack.packId == state.selectedPack?.packId
            Column(Modifier.width(64.dp).fillMaxHeight().testTag(StickerPanelTestTags.pack(pack.packId))
                .semantics { contentDescription = pack.displayName(language); selected = isSelected }
                .clip(Theme.shapes.lg).background(if (isSelected) colors.muted else colors.background)
                .clickable { state.selectPack(pack.packId) }.padding(3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PackCover(pack, language, Modifier.size(34.dp))
                Text(pack.displayName(language), style = Theme.typography.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PackCover(pack: StickerPack, language: String, modifier: Modifier) {
    val url = pack.coverImageUrl?.takeIf { it.isNotBlank() } ?: pack.stickers.firstOrNull()?.imageUrl
    Box(modifier.testTag(StickerPanelTestTags.cover(pack.packId)).clip(Theme.shapes.lg).background(Theme.colors.muted),
        contentAlignment = Alignment.Center) {
        if (url != null) Image(rememberAsyncImagePainter(url), contentDescription = pack.displayName(language),
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        else Text(pack.displayName(language).take(1), style = Theme.typography.titleMedium)
    }
}

@Composable
private fun PackCard(pack: StickerPack, state: StickerPanelState, language: String, manage: Boolean,
    onInstall: () -> Unit, onUninstall: () -> Unit, onMove: (Int) -> Unit) {
    val strings = PrivChatI18n.strings
    val panelStrings = stickerPanelStrings(language)
    val index = state.installed.indexOfFirst { it.packId == pack.packId }
    val installed = index >= 0
    val disabled = state.busy || !state.installedReady
    Column(Modifier.fillMaxWidth().testTag(StickerPanelTestTags.card(pack.packId)).padding(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PackCover(pack, language, Modifier.size(48.dp))
            Column(Modifier.weight(1f)) {
                Text(pack.displayName(language), modifier = Modifier.testTag(StickerPanelTestTags.name(pack.packId)),
                    style = Theme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(panelStrings.count(pack.stickers.size), modifier = Modifier.testTag(StickerPanelTestTags.count(pack.packId)),
                    style = Theme.typography.label, color = Theme.colors.mutedForeground)
                if (pack.publicationState != 2) Text(strings.stickerOffline, style = Theme.typography.label)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(text = if (installed) strings.stickerUninstall else strings.stickerInstall,
                modifier = Modifier.weight(1f).testTag(if (installed) StickerPanelTestTags.uninstall(pack.packId) else StickerPanelTestTags.install(pack.packId)),
                size = ButtonSize.SMALL, disabled = disabled || (!installed && pack.publicationState != 2),
                onClick = if (installed) onUninstall else onInstall)
            if (manage) {
                Button(text = strings.stickerMoveUp, modifier = Modifier.weight(1f).testTag(StickerPanelTestTags.up(pack.packId)),
                    size = ButtonSize.SMALL, disabled = disabled || index <= 0, onClick = { onMove(-1) })
                Button(text = panelStrings.moveDown, modifier = Modifier.weight(1f).testTag(StickerPanelTestTags.down(pack.packId)),
                    size = ButtonSize.SMALL, disabled = disabled || index < 0 || index == state.installed.lastIndex, onClick = { onMove(1) })
            }
        }
    }
}

@Composable
private fun StickerGrid(pack: StickerPack, state: StickerPanelState, modifier: Modifier, onSend: (CatalogSticker) -> Unit) {
    BoxWithConstraints(modifier.fillMaxWidth().testTag(StickerPanelTestTags.grid(pack.packId)).clipToBounds()) {
        val columns = (maxWidth.value / 68f).toInt().coerceIn(1, 8)
        val rows = remember(pack, columns) { pack.stickers.chunked(columns) }
        GearLazyColumn(Modifier.fillMaxSize(), avoidKeyboard = false) {
            items(rows.size, key = { rows[it].first().stickerId }) { index ->
                Row(Modifier.fillMaxWidth()) {
                    for (sticker in rows[index]) {
                        PublishedSticker(sticker, Modifier.weight(1f).height(72.dp).padding(4.dp)
                            .testTag(StickerPanelTestTags.send(sticker.stickerId))
                            .clickable(enabled = state.canSend(pack)) { onSend(sticker) })
                    }
                    repeat(columns - rows[index].size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
