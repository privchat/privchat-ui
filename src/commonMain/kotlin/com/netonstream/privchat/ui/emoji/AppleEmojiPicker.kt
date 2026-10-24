package com.netonstream.privchat.ui.emoji

import androidx.compose.runtime.*
import com.gearui.foundation.primitives.GearLazyColumn
import com.gearui.foundation.primitives.Text
import com.gearui.theme.Theme
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.semantics.contentDescription
import com.tencent.kuikly.compose.ui.semantics.semantics
import com.tencent.kuikly.compose.ui.semantics.testTag
import com.tencent.kuikly.compose.ui.semantics.selected
import com.tencent.kuikly.compose.ui.unit.dp

/** Every generated category is reachable; only the selected category's visible rows are composed. */
@Composable
fun AppleEmojiPicker(
    onSelect: (String) -> Unit,
    onDelete: () -> Unit,
    deleteLabel: String,
    modifier: Modifier = Modifier,
    catalog: AppleEmojiCatalog = AppleEmojiUiBridge.catalog,
) {
    var selectedId by remember(catalog) { mutableStateOf(catalog.groups.firstOrNull()?.id) }
    val colors = Theme.colors
    val language = PrivChatI18n.languageTag
    Column(modifier.testTag(AppleEmojiTestTags.Picker)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LazyRow(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) {
                items(catalog.groups.size, key = { catalog.groups[it].id }) { index ->
                    val group = catalog.groups[index]
                    val category = appleEmojiCategoryUi(group.id, language)
                    Text(category.icon,
                        modifier = Modifier.appleEmoji()
                            .testTag(AppleEmojiTestTags.category(index))
                            .semantics { contentDescription = category.accessibilityLabel; selected = selectedId == group.id }
                            .background(if (selectedId == group.id) colors.muted else colors.background)
                            .clickable { selectedId = group.id }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        style = Theme.typography.titleLarge,
                        color = if (selectedId == group.id) colors.primary else colors.mutedForeground,
                        maxLines = 1,
                    )
                }
            }
            Text(deleteLabel, modifier = Modifier.testTag(AppleEmojiTestTags.Delete).clickable(onClick = onDelete).padding(10.dp), style = Theme.typography.label)
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val columns = (maxWidth.value / 48f).toInt().coerceIn(1, 12)
            val entries = remember(catalog, selectedId) { catalog.entriesIn(selectedId.orEmpty()) }
            val rows = remember(entries, columns) { entries.chunked(columns) }
            key(catalog, selectedId, columns) {
                GearLazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(3.dp), avoidKeyboard = false) {
                    items(rows.size, key = { rows[it].first().unicode }) { index ->
                        Row(Modifier.fillMaxWidth()) {
                            for (entry in rows[index]) {
                                Box(Modifier.weight(1f).height(48.dp).testTag(AppleEmojiTestTags.entry(entry.unicode))
                                    .semantics { contentDescription = entry.label.ifEmpty { entry.unicode } }
                                    .clickable { onSelect(entry.unicode) }, contentAlignment = Alignment.Center) {
                                    Text(entry.unicode, modifier = Modifier.appleEmoji(), style = Theme.typography.headlineSmall)
                                }
                            }
                            repeat(columns - rows[index].size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}
