package com.netonstream.privchat.ui.avatar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import com.gearui.foundation.avatar.AvatarSizeTokens
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.typography.Typography
import com.netonstream.privchat.sdk.dto.GroupMemberEntry
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.models.displayName
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp

/**
 * 群成员预览缓存：channelId → 排序后前 9 名成员（进程内一次，无 TTL）。
 *
 * 排序规则（三端统一，App 无 joined_at 以 userId 近似）：
 * role 权重 owner < admin < member 升序 → userId 升序。
 */
private object GroupMemberPreviewCache {
    // mutableStateMapOf：写入后引用它的 Composable 自动重组
    private val cache = mutableStateMapOf<ULong, List<GroupMemberEntry>>()
    private val totals = mutableStateMapOf<ULong, Int>()
    private val inFlight = mutableSetOf<ULong>()

    fun peek(channelId: ULong): List<GroupMemberEntry>? = cache[channelId]

    /** 拉取并缓存前 9 名成员；重复调用 / 并发进入直接跳过（in-flight 去重）。 */
    suspend fun load(channelId: ULong) {
        if (cache.containsKey(channelId) || !inFlight.add(channelId)) return
        try {
            var members = PrivChat.client.getGroupMembers(channelId, null, null)
                .getOrNull().orEmpty()
            // 本地为空（新登录还没同步）或 entity sync 只写了 uid 没带昵称
            // （displayName 退化成纯数字 uid）→ 拉一次远端补全再读
            if (members.isEmpty() || members.any { it.displayName.toULongOrNull() == it.userId }) {
                PrivChat.client.syncGroupMembers(channelId)
                members = PrivChat.client.getGroupMembers(channelId, null, null)
                    .getOrNull().orEmpty()
            }
            if (members.isNotEmpty()) {
                cache[channelId] = topNine(members)
                totals[channelId] = members.size
            }
        } catch (_: Throwable) {
            // 失败静默：留给下次进入会话列表时重试（inFlight 已释放）
        } finally {
            inFlight.remove(channelId)
        }
    }

    /** 已缓存成员总数（0 = 未知）；供群标题人数兜底（ChannelListEntry.memberCount 尚无数据源）。 */
    fun memberCount(channelId: ULong): Int = totals[channelId] ?: 0

    private fun topNine(members: List<GroupMemberEntry>): List<GroupMemberEntry> =
        members
            .sortedWith(compareBy({ roleWeight(it.role) }, { it.userId }))
            .take(9)

    // owner(role=2) → 0 / admin(role=1) → 1 / member(role=0) → 2
    private fun roleWeight(role: Int): Int = when (role) {
        2 -> 0
        1 -> 1
        else -> 2
    }
}

/** 群成员总数(来自九宫格成员预览缓存;0=尚未拉到)。群标题「名称 (人数)」的数据源。 */
fun groupMemberPreviewCount(channelId: ULong): Int = GroupMemberPreviewCache.memberCount(channelId)

/**
 * 群头像九宫格拼贴（三端统一规格）。
 *
 * - 成员格 = per-identity hash 色块（seed `"u:<uid>"`）+ 白色首字
 * - 布局：n=1 单列；2–4 两列；5–9 三列；行块垂直居中、首行水平居中
 * - 成员未拉到 / 空群 → 回退 [PrivChatAvatar]（群名首字 + `"g:<channelId>"` hash 色）
 *
 * Phase 1 不接远程图加载，成员 avatar url 一律不渲染，只走色块 + 首字。
 * 布局用 Column/Row/Box 拼（仓库无 Canvas 先例，刻意不引入）。
 */
@Composable
fun GroupCollageAvatar(
    channelId: ULong,
    name: String,
    size: Dp = AvatarSizeTokens.Medium.size,
    radius: Dp = 6.dp,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(channelId) { GroupMemberPreviewCache.load(channelId) }
    val members = GroupMemberPreviewCache.peek(channelId).orEmpty()

    if (members.isEmpty()) {
        PrivChatAvatar(
            name = name,
            size = size,
            radius = radius,
            isGroup = true,
            seed = "g:$channelId",
            modifier = modifier,
        )
        return
    }

    // 布局参数（全用 Float 算好再转 Dp，避免 Dp 运算符跨平台差异）。
    // 固定 3x3 九格：无论成员多少格子恒 9 个，从左上按行填,空位画浅色空格块。
    val padValue = size.value * 0.04f
    val gapValue = size.value * 0.04f
    val cellValue = (size.value - padValue * 2 - gapValue * 2) / 3
    val cell = cellValue.dp
    val gap = gapValue.dp

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(Color(0xFFD9DCE0)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            for (row in 0 until 3) {
                if (row > 0) Spacer(modifier = Modifier.height(gap))
                Row {
                    for (col in 0 until 3) {
                        if (col > 0) Spacer(modifier = Modifier.width(gap))
                        val index = row * 3 + col
                        val member = members.getOrNull(index)
                        if (member != null) {
                            CollageCell(member = member, cell = cell)
                        } else {
                            EmptyCell(cell = cell)
                        }
                    }
                }
            }
        }
    }
}

/** 空格子：比容器底色略浅的占位块，保证九宫格视觉恒为 9 格。 */
@Composable
private fun EmptyCell(cell: Dp) {
    Box(
        modifier = Modifier
            .size(cell)
            .clip(RoundedCornerShape((cell.value * 0.12f).dp))
            .background(Color(0xFFEDEFF2)),
    )
}

/** 单个成员格：hash 色块 + 白色首字（字号约 cell*0.5，双字母 initials 略缩）。 */
@Composable
private fun CollageCell(member: GroupMemberEntry, cell: Dp) {
    val initials = AvatarText.initialsOf(
        name = member.displayName,
        fallbackId = member.userId.toLong(),
    )
    val fontScale = if (initials.length > 1) 0.36f else 0.5f
    Box(
        modifier = Modifier
            .size(cell)
            .clip(RoundedCornerShape((cell.value * 0.12f).dp))
            .background(Color(AvatarPalette.hashBackgroundArgb("u:${member.userId}"))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = Typography.BodyMedium.copy(fontSize = (cell.value * fontScale).sp),
            color = Color(AvatarPalette.HASH_FOREGROUND_ARGB),
        )
    }
}
