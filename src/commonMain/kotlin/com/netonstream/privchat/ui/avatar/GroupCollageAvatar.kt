package com.netonstream.privchat.ui.avatar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.netonstream.privchat.ui.state.GroupStore
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
// removed: mutableStateMapOf / PrivChat imports — private member cache collapsed into GroupStore (P6-1)
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.gearui.foundation.avatar.AvatarSizeTokens
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.typography.Typography
import com.netonstream.privchat.sdk.dto.GroupMemberEntry
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
 * 群成员总数（P6-1：来自 [GroupStore]，channel-keyed + 系统用户已过滤；0=尚未拉到）。
 * 群标题「名称 (人数)」的数据源——不再含系统账号。
 */
fun groupMemberPreviewCount(channelId: ULong): Int = GroupStore.memberCount(channelId)

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
    LaunchedEffect(channelId) { GroupStore.ensureMembers(channelId) }
    // P6-1：九宫格改读 GroupStore（channel-keyed + 系统用户已过滤），不再用私有未过滤缓存。
    val members = GroupStore.preview(channelId)

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

    // P2(AVATAR_CACHE_SPEC §5.3)：九宫格合成一次 PNG 落盘，命中即 file:// 加载
    // （与 PrivChatAvatar 同一图片管道）；合成中/失败回退下方运行时逐格绘制。
    var collageUrl by remember(channelId) { mutableStateOf<String?>(null) }
    LaunchedEffect(channelId, members) {
        val cm = members.take(9).map {
            GeneratedAvatarCache.CollageMember(
                uid = it.userId.toString(),
                name = it.displayName,
                username = null,
                hasAvatar = it.avatar.isNotBlank(),
            )
        }
        collageUrl = GeneratedAvatarCache
            .ensureCollage(channelId.toString(), cm)
            ?.let { "file://$it" }
    }
    val cu = collageUrl
    if (cu != null) {
        Image(
            painter = rememberAsyncImagePainter(model = cu),
            contentDescription = "",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(radius)),
        )
        return
    }

    // 布局参数（全用 Float 算好再转 Dp，避免 Dp 运算符跨平台差异）。
    // 固定 3x3 九格：无论成员多少格子恒 9 个，从左上按行填,空位画浅色空格块。
    // 微信风:外边距偏大、格间距很小、格子直角(无圆角,省渲染)。
    val padValue = size.value * 0.09f
    val gapValue = size.value * 0.02f
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
            .background(Color(0xFFEDEFF2)),
    )
}

/**
 * 单个成员格：initials/配色一律走 [rememberAvatarResolved]——与 [PrivChatAvatar]
 * 完全同源(seed=u:<uid>),九宫格只负责组装。方形无圆角(微信风,省渲染)。
 */
@Composable
private fun CollageCell(member: GroupMemberEntry, cell: Dp) {
    val resolved = rememberAvatarResolved(
        name = member.displayName,
        userId = member.userId.toLong(),
    )
    val fontScale = if (resolved.initials.length > 1) 0.36f else 0.5f
    val fontSize = (cell.value * fontScale).sp
    Box(
        modifier = Modifier
            .size(cell)
            .background(resolved.backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        // lineHeight = fontSize:小格子里默认行高会把字挤偏,压平后才真正居中
        Text(
            text = resolved.initials,
            style = Typography.BodyMedium.copy(fontSize = fontSize, lineHeight = fontSize),
            color = resolved.foregroundColor,
        )
    }
}
