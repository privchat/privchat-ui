package com.netonstream.privchat.ui.sticker

import androidx.compose.runtime.*
import com.gearui.components.button.Button
import com.gearui.components.button.ButtonSize
import com.gearui.components.icon.*
import com.gearui.components.input.Input
import com.gearui.foundation.primitives.GearLazyColumn
import com.gearui.foundation.primitives.Icon
import com.gearui.foundation.primitives.Text
import com.gearui.theme.Theme
import com.netonstream.privchat.sdk.dto.StickerMessagePayload
import com.netonstream.privchat.ui.error.UserFacingError
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.netonstream.privchat.ui.i18n.stickerPanelStrings
import com.netonstream.privchat.ui.i18n.stickerLibraryStrings
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.platform.LocalFocusManager
import com.tencent.kuikly.compose.ui.platform.LocalSoftwareKeyboardController
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.draw.clipToBounds
import com.tencent.kuikly.compose.ui.semantics.contentDescription
import com.tencent.kuikly.compose.ui.semantics.selected
import com.tencent.kuikly.compose.ui.semantics.semantics
import com.tencent.kuikly.compose.ui.semantics.testTag
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class StickerPanelSection { DEFAULT, FAVORITES, PACK, SEARCH, STORE, MANAGE }

@Composable
fun StickerPanel(onSend: suspend (StickerMessagePayload) -> Result<ULong>, onSearchActiveChange: (Boolean) -> Unit = {}) {
    val catalog = StickerUiBridge.catalog ?: return
    val session = catalog.session ?: return
    key(session, catalog) { AccountStickerPanel(session, catalog, onSend, onSearchActiveChange) }
}

@Composable
private fun AccountStickerPanel(session: StickerSession, catalog: StickerCatalog, onSend: suspend (StickerMessagePayload) -> Result<ULong>,
    onSearchActiveChange: (Boolean) -> Unit) {
    val strings = PrivChatI18n.strings
    val language = PrivChatI18n.languageTag
    val labels = stickerLibraryStrings(language)
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val state = remember(catalog, session) { StickerPanelState(catalog, session) }
    var section by remember { mutableStateOf(StickerPanelSection.DEFAULT) }
    var query by remember { mutableStateOf("") }
    var storePackId by remember { mutableStateOf<String?>(null) }
    var wasSearch by remember { mutableStateOf(false) }
    DisposableEffect(session) { onDispose { onSearchActiveChange(false) } }
    LaunchedEffect(catalog, session) {
        launch { state.refreshInstalled() }
        launch { state.refreshStore() }
    }
    LaunchedEffect(section) {
        // Keyboard ownership follows the search page, not transient native focus during re-layout.
        onSearchActiveChange(section == StickerPanelSection.SEARCH)
        if (section != StickerPanelSection.SEARCH && wasSearch) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
        wasSearch = section == StickerPanelSection.SEARCH
        if (section == StickerPanelSection.FAVORITES) state.refreshFavorites()
    }
    LaunchedEffect(query, section) {
        if (section == StickerPanelSection.SEARCH) {
            // Clear stale results immediately; debounce only the new network read.
            state.search("")
            if (query.isNotBlank()) { delay(250); state.search(query) }
        }
    }
    fun send(sticker: CatalogSticker, pack: StickerPack? = null) {
        scope.launch { state.perform {
            if (catalog.session != session || (pack != null && !state.canSendPackForAction(pack))) {
                throw CancellationException("sticker installation changed")
            }
            onSend(StickerMessagePayload(sticker.stickerId, sticker.imageUrl)).getOrThrow()
        } }
    }
    Column(Modifier.fillMaxSize().testTag(StickerPanelTestTags.Panel).background(Theme.colors.background)) {
        val browsing = section == StickerPanelSection.STORE || section == StickerPanelSection.MANAGE
        val error = state.actionError ?: when (section) {
            StickerPanelSection.STORE -> state.storeError
            StickerPanelSection.FAVORITES -> state.favoritesError
            StickerPanelSection.SEARCH -> state.searchError
            else -> state.installedError
        }
        if (section == StickerPanelSection.SEARCH) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Input(value = query, onValueChange = { query = it }, placeholder = labels.searchHint,
                    maxLength = 100, modifier = Modifier.weight(1f).testTag(StickerPanelTestTags.SearchInput), clearable = true,
                    autoFocus = true)
            }
        } else if (browsing) {
            Row(Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                PanelIcon(Icons.caretLeft, labels.back, "sticker-back", false) {
                    if (section == StickerPanelSection.MANAGE || storePackId != null) {
                        storePackId = null
                        section = StickerPanelSection.STORE
                    } else section = StickerPanelSection.DEFAULT
                }
                Text(if (section == StickerPanelSection.STORE) labels.addPacks else labels.manage,
                    Modifier.weight(1f), style = Theme.typography.label)
                if (section == StickerPanelSection.STORE) PanelIcon(Icons.gearSix, labels.manage, StickerPanelTestTags.Manage, false) {
                    section = StickerPanelSection.MANAGE
                }
            }
        }
        if (error != null) {
            Text(UserFacingError.message(error, strings.networkError),
                Modifier.fillMaxWidth().clickable { scope.launch {
                    state.dismissActionError()
                    when (section) {
                        StickerPanelSection.FAVORITES -> state.refreshFavorites()
                        StickerPanelSection.SEARCH -> state.search(query)
                        StickerPanelSection.STORE -> state.refreshStore()
                        else -> state.refreshInstalled()
                    }
                } }.padding(horizontal = 12.dp), style = Theme.typography.label)
        }
        Box(Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
            when (section) {
                StickerPanelSection.DEFAULT, StickerPanelSection.PACK -> {
                    val pack = if (section == StickerPanelSection.DEFAULT) state.defaultPack else state.selectedPack
                    if (pack != null) {
                        Column(Modifier.fillMaxSize()) {
                            Text(pack.displayName(language), Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                                .testTag(StickerPanelTestTags.SelectedPack), style = Theme.typography.label,
                                color = Theme.colors.mutedForeground, maxLines = 1)
                            StickerGrid(pack.stickers, StickerPanelTestTags.grid(pack.packId), Modifier.weight(1f),
                                compact = section == StickerPanelSection.DEFAULT, enabled = state.canSend(pack)) { send(it, pack) }
                        }
                    } else if (state.installedLoading) PanelEmpty(strings.loading)
                    else PanelEmpty(labels.addPacks) { section = StickerPanelSection.STORE }
                }
                StickerPanelSection.FAVORITES -> {
                    if (state.favorites.isEmpty()) PanelEmpty(if (state.favoritesLoading) strings.loading else labels.emptyFavorites)
                    else StickerGrid(state.favorites, "sticker-favorites-grid", Modifier.fillMaxSize(), enabled = !state.busy) { send(it) }
                }
                StickerPanelSection.SEARCH -> {
                    val result = state.searchResult
                    if (state.searchLoading) PanelEmpty(strings.loading)
                    else if (query.isBlank()) PanelEmpty(labels.searchHint)
                    else if (result.packs.isEmpty() && result.stickers.isEmpty()) PanelEmpty(labels.emptySearch)
                    else Column(Modifier.fillMaxSize()) {
                        if (result.packs.isNotEmpty()) LazyRow(Modifier.fillMaxWidth().height(60.dp)) {
                            items(result.packs.size, key = { result.packs[it].packId }) { index ->
                                val pack = result.packs[index]
                                Row(Modifier.width(170.dp).fillMaxHeight().clickable {
                                    state.selectPack(pack.packId)
                                    storePackId = pack.packId
                                    section = if (state.installed.any { it.packId == pack.packId }) StickerPanelSection.PACK else StickerPanelSection.STORE
                                }.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    PackCover(pack, language, Modifier.size(38.dp))
                                    Text(pack.displayName(language), Modifier.padding(start = 6.dp).weight(1f),
                                        style = Theme.typography.label, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        StickerGrid(result.stickers, "sticker-search-grid", Modifier.weight(1f), enabled = !state.busy) { send(it) }
                    }
                }
                StickerPanelSection.STORE, StickerPanelSection.MANAGE -> {
                    val store = section == StickerPanelSection.STORE
                    val packs = if (store) state.store.filter { storePackId == null || it.packId == storePackId } else state.installed
                    if (packs.isEmpty()) PanelEmpty(if (if (store) state.storeLoading else state.installedLoading) strings.loading else strings.noData)
                    else GearLazyColumn(Modifier.fillMaxSize(), avoidKeyboard = false) {
                        items(packs.size, key = { packs[it].packId }) { index ->
                            val pack = packs[index]
                            PackCard(pack, state, language, manage = !store,
                                onInstall = { scope.launch { state.install(pack.packId) } },
                                onUninstall = { scope.launch { state.uninstall(pack.packId) } },
                                onMove = { delta -> scope.launch { state.move(pack.packId, delta) } })
                        }
                    }
                }
            }
        }
        // The primary panel is navigation, not a second management toolbar.
        Row(Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            PanelIcon(Icons.magnifyingGlass, labels.search, StickerPanelTestTags.Search, section == StickerPanelSection.SEARCH) { section = StickerPanelSection.SEARCH }
            PanelIcon(Icons.smiley, labels.default, StickerPanelTestTags.Installed, section == StickerPanelSection.DEFAULT) { section = StickerPanelSection.DEFAULT }
            PanelIcon(Icons.heart, labels.favorites, StickerPanelTestTags.Favorites, section == StickerPanelSection.FAVORITES) { section = StickerPanelSection.FAVORITES }
            LazyRow(Modifier.weight(1f).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                val packs = state.installed.filter { it.packId != state.defaultPack?.packId }
                items(packs.size, key = { packs[it].packId }) { index ->
                    val pack = packs[index]
                    Box(Modifier.size(44.dp).testTag(StickerPanelTestTags.pack(pack.packId))
                        .semantics { contentDescription = pack.displayName(language); selected = section == StickerPanelSection.PACK && state.selectedPackId == pack.packId }
                        .clip(Theme.shapes.md).background(if (section == StickerPanelSection.PACK && state.selectedPackId == pack.packId) Theme.colors.muted else Theme.colors.background)
                        .clickable { state.selectPack(pack.packId); section = StickerPanelSection.PACK }.padding(7.dp)) {
                        PackCover(pack, language, Modifier.fillMaxSize())
                    }
                }
            }
            PanelIcon(Icons.plus, labels.addPacks, StickerPanelTestTags.Store, browsing) {
                storePackId = null
                section = StickerPanelSection.STORE
                scope.launch { state.refreshStore() }
            }
        }
    }
}

@Composable
private fun PanelEmpty(text: String, onClick: (() -> Unit)? = null) {
    Box(Modifier.fillMaxSize().then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)).padding(20.dp), contentAlignment = Alignment.Center) {
        Text(text, style = Theme.typography.label, color = Theme.colors.mutedForeground)
    }
}

@Composable
private fun PanelIcon(icon: IconSource, label: String, tag: String, active: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).testTag(tag).semantics { contentDescription = label; selected = active }
        .clip(Theme.shapes.md).background(if (active) Theme.colors.muted else Theme.colors.background)
        .clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, size = 23.dp, tint = Theme.colors.foreground)
    }
}

@Composable
private fun PackCover(pack: StickerPack, language: String, modifier: Modifier) {
    val url = pack.coverImageUrl?.takeIf { it.isNotBlank() } ?: pack.stickers.firstOrNull()?.imageUrl
    Box(modifier.testTag(StickerPanelTestTags.cover(pack.packId)).clip(Theme.shapes.md), contentAlignment = Alignment.Center) {
        if (url != null) PublishedStickerImage(url, pack.displayName(language), Modifier.fillMaxSize())
        else Text(pack.displayName(language).take(1), style = Theme.typography.titleMedium)
    }
}

@Composable
private fun PackCard(pack: StickerPack, state: StickerPanelState, language: String, manage: Boolean,
    onInstall: () -> Unit, onUninstall: () -> Unit, onMove: (Int) -> Unit) {
    val strings = PrivChatI18n.strings
    val labels = stickerLibraryStrings(language)
    val panelStrings = stickerPanelStrings(language)
    val index = state.installed.indexOfFirst { it.packId == pack.packId }
    val installed = index >= 0
    val disabled = state.busy || !state.installedReady
    Column(Modifier.fillMaxWidth().testTag(StickerPanelTestTags.card(pack.packId)).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PackCover(pack, language, Modifier.size(44.dp))
            Column(Modifier.weight(1f)) {
                Text(pack.displayName(language), Modifier.testTag(StickerPanelTestTags.name(pack.packId)),
                    style = Theme.typography.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(panelStrings.count(pack.stickers.size), Modifier.testTag(StickerPanelTestTags.count(pack.packId)),
                    style = Theme.typography.label, color = Theme.colors.mutedForeground)
            }
            if (!manage) {
                if (installed) Text(labels.installed, Modifier.testTag("sticker-already-installed-${pack.packId}"),
                    style = Theme.typography.label, color = Theme.colors.mutedForeground)
                else Button(text = strings.stickerInstall, modifier = Modifier.testTag(StickerPanelTestTags.install(pack.packId)),
                    size = ButtonSize.SMALL, disabled = disabled || pack.publicationState != 2, onClick = onInstall)
            } else Button(text = strings.stickerUninstall, modifier = Modifier.testTag(StickerPanelTestTags.uninstall(pack.packId)),
                size = ButtonSize.SMALL, disabled = disabled, onClick = onUninstall)
        }
        if (manage) Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
            Button(text = strings.stickerMoveUp, modifier = Modifier.testTag(StickerPanelTestTags.up(pack.packId)),
                size = ButtonSize.SMALL, disabled = disabled || index <= 0, onClick = { onMove(-1) })
            Button(text = panelStrings.moveDown, modifier = Modifier.testTag(StickerPanelTestTags.down(pack.packId)),
                size = ButtonSize.SMALL, disabled = disabled || index == state.installed.lastIndex, onClick = { onMove(1) })
        }
    }
}

@Composable
private fun StickerGrid(stickers: List<CatalogSticker>, tag: String, modifier: Modifier, compact: Boolean = false,
    enabled: Boolean = true, onSend: (CatalogSticker) -> Unit) {
    BoxWithConstraints(modifier.fillMaxWidth().testTag(tag).clipToBounds()) {
        val cell = if (compact) 44f else 76f
        val columns = (maxWidth.value / cell).toInt().coerceIn(1, 10)
        val rows = remember(stickers, columns) { stickers.chunked(columns) }
        GearLazyColumn(Modifier.fillMaxSize(), avoidKeyboard = false) {
            items(rows.size, key = { rows[it].first().stickerId }) { index ->
                Row(Modifier.fillMaxWidth()) {
                    for (sticker in rows[index]) PublishedSticker(sticker,
                        Modifier.weight(1f).height(cell.dp).padding(if (compact) 8.dp else 6.dp)
                            .testTag(StickerPanelTestTags.send(sticker.stickerId)).clickable(enabled = enabled) { onSend(sticker) })
                    repeat(columns - rows[index].size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
