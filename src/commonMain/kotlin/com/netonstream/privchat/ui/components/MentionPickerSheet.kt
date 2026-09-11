package com.netonstream.privchat.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import com.gearui.components.bottomsheet.BottomSheet
import com.gearui.components.checkbox.Checkbox
import com.gearui.components.icon.Icons
import com.gearui.components.searchbar.SearchBar
import com.gearui.components.searchbar.SearchBarCancel
import com.gearui.foundation.avatar.AvatarSizeTokens
import com.gearui.foundation.layout.Spacing
import com.gearui.foundation.primitives.GearLazyColumn
import com.gearui.foundation.primitives.Icon
import com.gearui.foundation.primitives.Text
import com.gearui.theme.Theme
import com.netonstream.privchat.sdk.dto.GroupMemberEntry
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.netonstream.privchat.ui.i18n.withArgs
import com.netonstream.privchat.ui.models.displayName
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.CircleShape
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 群聊 @ 选人面板（微信「选择联系人」形态）。
 *
 * 之前这里是输入栏上方一块 220dp 的内联列表：它盖在会话上、跟着键盘挤来挤去，
 * 名字多了只能在一小条里滚，而且键盘一直占着半屏——所以"弹窗效果不对"。
 * 现在改成底部面板：自带标题栏与搜索框，打开时收起键盘，与微信一致。
 *
 * 分组用**显示名首字符**，与联系人页同一套口径（[com.netonstream.privchat.ui.pages.ContactPage]）。
 * 没有引入拼音：中文名会各自成组，看起来不像微信那样归到 A–Z，但这是全 app 一致的
 * 现状，在这里单独发明一套排序只会让两个页面的索引对不上。真要做拼音，是一次独立的改动。
 */
@Composable
fun MentionPickerSheet(
    visible: Boolean,
    members: List<GroupMemberEntry>,
    /** 初始搜索词：用户在输入框里 `@` 后面已经敲了的那几个字。 */
    initialQuery: String,
    onDismiss: () -> Unit,
    /** 单选直接回调一个；多选按勾选顺序回调多个。 */
    onPick: (List<GroupMemberEntry>) -> Unit,
) {
    val strings = PrivChatI18n.strings
    var query by remember(visible) { mutableStateOf(initialQuery) }
    var multiSelect by remember(visible) { mutableStateOf(false) }
    val selected = remember(visible) { mutableStateListOf<ULong>() }

    val sections = remember(members, query) {
        val filtered = if (query.isBlank()) {
            members
        } else {
            members.filter { it.displayName.contains(query, ignoreCase = true) }
        }
        filtered
            .groupBy { it.displayName.firstOrNull()?.uppercaseChar() ?: '#' }
            .entries
            .sortedBy { it.key }
            .map { it.key to it.value }
    }

    BottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        // 标题栏自绘：微信那一行是「关闭 / 标题 / 多选」，不是 gearui 默认的居中标题 + 取消。
        showCancel = false,
    ) {
        Column(modifier = Modifier.fillMaxWidth().height(SHEET_HEIGHT)) {
            Header(
                multiSelect = multiSelect,
                selectedCount = selected.size,
                onClose = onDismiss,
                onToggleMulti = {
                    multiSelect = !multiSelect
                    selected.clear()
                },
                onConfirm = {
                    // 按勾选顺序回调：插进输入框的 @ 顺序就是用户勾的顺序。
                    val picked = selected.mapNotNull { id -> members.find { it.userId == id } }
                    if (picked.isNotEmpty()) onPick(picked)
                },
            )

            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg)) {
                SearchBar(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = strings.search,
                    // 面板自己就有关闭按钮，再给搜索框挂一个"取消"会有两个退出口。
                    cancel = SearchBarCancel.Never,
                )
            }
            Spacer(modifier = Modifier.height(Spacing.sm))

            if (sections.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = strings.mentionPickerNoResult,
                        style = Theme.typography.bodyMedium,
                        color = Theme.colors.mutedForeground,
                    )
                }
            } else {
                GearLazyColumn(modifier = Modifier.fillMaxWidth()) {
                    sections.forEach { (letter, list) ->
                        item { LetterHeader(letter.toString()) }
                        items(list.size) { index ->
                            val member = list[index]
                            MemberRow(
                                member = member,
                                multiSelect = multiSelect,
                                checked = selected.contains(member.userId),
                                onClick = {
                                    if (multiSelect) {
                                        if (!selected.remove(member.userId)) selected.add(member.userId)
                                    } else {
                                        onPick(listOf(member))
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 面板高度：占屏幕下方大部分，但露出上面的会话——微信也是这个比例。 */
private val SHEET_HEIGHT = 520.dp

@Composable
private fun Header(
    multiSelect: Boolean,
    selectedCount: Int,
    onClose: () -> Unit,
    onToggleMulti: () -> Unit,
    onConfirm: () -> Unit,
) {
    val strings = PrivChatI18n.strings
    val colors = Theme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.muted)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(name = Icons.caret_down, size = 18.dp, tint = colors.foreground)
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(
                text = strings.mentionPickerTitle,
                style = Theme.typography.titleMedium,
                color = colors.foreground,
            )
        }

        // 多选态下右侧变成「完成(n)」；没勾人时点它没有意义，置灰但仍可退回单选。
        val label = when {
            !multiSelect -> strings.mentionPickerMultiple
            selectedCount > 0 -> strings.mentionPickerDone.withArgs(selectedCount)
            else -> strings.cancel
        }
        Text(
            text = label,
            style = Theme.typography.bodyMedium,
            color = if (multiSelect && selectedCount > 0) colors.primary else colors.mutedForeground,
            modifier = Modifier.clickable {
                when {
                    !multiSelect -> onToggleMulti()
                    selectedCount > 0 -> onConfirm()
                    else -> onToggleMulti()
                }
            },
        )
    }
}

@Composable
private fun LetterHeader(letter: String) {
    Text(
        text = letter,
        style = Theme.typography.label,
        color = Theme.colors.mutedForeground,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
    )
}

@Composable
private fun MemberRow(
    member: GroupMemberEntry,
    multiSelect: Boolean,
    checked: Boolean,
    onClick: () -> Unit,
) {
    val name = member.displayName
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (multiSelect) {
            // 勾选框只读：整行才是点击目标，两处都可点会让"点名字算不算勾选"变得含糊。
            Checkbox(checked = checked, onCheckedChange = { onClick() })
            Spacer(modifier = Modifier.width(Spacing.md))
        }
        ChatAvatar(
            url = member.avatar.takeIf { it.isNotBlank() },
            name = name,
            size = AvatarSizeTokens.Medium.size,
            userId = member.userId.toLong(),
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Text(
            text = name,
            style = Theme.typography.bodyMedium,
            color = Theme.colors.foreground,
        )
    }
}
