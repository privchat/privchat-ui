package com.netonstream.privchat.ui.avatar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gearui.foundation.avatar.AvatarSizeTokens
import com.gearui.components.avatar.Avatar
import com.netonstream.privchat.ui.common.base.PrivChatThemeExtension.onlineStatus
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.gearui.theme.Theme

/**
 * PrivChat 业务**唯一**头像入口。所有业务页面（消息、联系人、个人资料、群、二维码…）只用这个。
 *
 * 直接调 [com.gearui.primitives.Avatar] 被视为分裂规则——initials / 配色 / radius 三件套必须经过
 * [rememberAvatarResolved] 统一解析，bitmap 渲染（QR 中心头像）通过 [AvatarText] +
 * [AvatarPalette] 共享同一来源。
 *
 * @param name 用户/群的显示名（nickname / groupName 最常用）
 * @param username 兜底来源；name 为空时使用
 * @param avatarUrl 远程头像图 URL（http(s)/file/assets）。非空时异步加载并按 [radius] 裁剪
 *   覆盖在 initials 色块之上；加载中 / 加载失败自然露出 initials 色块兜底
 * @param userId 最末兜底来源（取末 2 位）
 * @param size 头像尺寸；默认 [AvatarSizeTokens.Medium]
 * @param radius 圆角（默认 6dp 方圆角，跟既有 [ChatAvatar] 视觉一致）；群头像传同值不区分
 * @param isGroup 元信息标记；视觉差异留给下阶段
 * @param unreadCount 未读 badge 数量（旧 [ChatAvatar] 用法保留）
 * @param isMuted 免打扰时不显示数字、只显示小红点（旧 [ChatAvatar] 用法保留）
 * @param isOnline 在线小绿点（旧 [ChatAvatar] 用法保留）
 * @param seed hash 色种子（`"u:<uid>"` / `"g:<channelId>"`）；不传时由 resolver 按 userId/名字兜底
 * @param preferLocalCache 已缓存头像（如自己头像，SDK 已下载到本地，见 [AvatarCacheLayout]）优先
 *   直接读本地文件、跳过远程网络加载，消除 initials→网络图的闪烁；本地无缓存时自动回落远程/initials
 */
/**
 * 头像入口的 [AvatarModel] 重载（CLIENT_GLOBAL_STATE §8）：所有页面应逐步迁移到此形态，
 * 由 AvatarStore 产出 model，UI 不再自己拼 `avatarUrl` / `preferLocalCache`。
 * `localPath` 非空（AvatarStore 已判本地缓存可用）→ 直接 `file://` 渲染，near-instant 无闪烁；
 * 否则远程；都无则 initials 兜底（由基础组件按 name/username/userId 解析）。
 */
@Composable
fun PrivChatAvatar(
    model: AvatarModel,
    size: Dp = AvatarSizeTokens.Medium.size,
    radius: Dp = 6.dp,
    unreadCount: Int = 0,
    isMuted: Boolean = false,
    isOnline: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val url = model.localPath?.let { "file://$it" } ?: model.remoteUrl
    PrivChatAvatar(
        name = model.displayName,
        username = model.username,
        avatarUrl = url,
        userId = model.userId,
        size = size,
        radius = radius,
        isGroup = model.isGroup,
        unreadCount = unreadCount,
        isMuted = isMuted,
        isOnline = isOnline,
        seed = model.seed,
        modifier = modifier,
    )
}

@Composable
fun PrivChatAvatar(
    name: String?,
    username: String? = null,
    avatarUrl: String? = null,
    userId: Long? = null,
    size: Dp = AvatarSizeTokens.Medium.size,
    radius: Dp = 6.dp,
    isGroup: Boolean = false,
    unreadCount: Int = 0,
    isMuted: Boolean = false,
    isOnline: Boolean = false,
    seed: String? = null,
    preferLocalCache: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val resolved = rememberAvatarResolved(
        name = name,
        username = username,
        avatarUrl = avatarUrl,
        userId = userId,
        isGroup = isGroup,
        seed = seed,
    )
    val colors = Theme.colors
    // 强制把 fallback 配色固定到 [AvatarPalette]，不让 gearui Avatar 回退到 Theme.colors.muted——
    // 否则 dark theme 下「我」tab 头像会变暗色，而 QR bitmap 头像永远是 light 色（保存到相册的
    // 图片跟 app theme 解耦），两条管道视觉就割裂了。
    // 头像渲染统一交给 gearui Avatar：首字色块始终在底层，图片（裁切填满）加载中 / 失败自然
    // 露出首字；未读 badge 与在线点都画在图片之上。本组件只负责「选哪张图」。
    // 本地优先（CLIENT_GLOBAL_STATE §4）：任意用户头像都先读本地缓存文件，near-instant，
    // 不会先首字后网络图闪一下；本地无则远程；无远程头像的用户用生成的首字 PNG（P2）。
    var localCacheUrl by remember(userId) { mutableStateOf<String?>(null) }
    if (userId != null && !isGroup) {
        LaunchedEffect(userId, resolved.avatarUrl) {
            val root = AvatarLocalCache.userRoot
            val given = resolved.avatarUrl
            localCacheUrl = when {
                // 🔴 调用方已经给了本地文件（model.localPath → "file://…"）就直接用，
                // 不要再自己盲探 `{uid}.img`。
                //
                // 盲探那条路推导出的文件名只由 uid 决定，而换头像是**原地覆盖同一个
                // 文件**；加载器按 URL 缓存，URL 不变就永远给旧图——换完头像页面纹丝
                // 不动，要杀进程重进才看得到。数据里的 localPath 带内容指纹（SDK 侧
                // 按远端 URL 命名），换头像它就变，重新加载是自然发生的。
                //
                // 别想用 `?v=` 或 `#v=` 去骗缓存：两者都会被当成文件路径的一部分，
                // 文件打不开，头像直接掉回字母占位（实测过）。
                given?.startsWith("file://") == true -> given
                root != null && given != null ->
                    AvatarCacheLayout.userAvatarFile(root, userId)?.let { "file://$it" }
                else -> null
            }
        }
    }
    val remoteUrl = resolved.avatarUrl?.trim()?.takeIf { it.isNotEmpty() }
    // P2(AVATAR_CACHE_SPEC §5.2)：无远程头像的用户，生成 initials 色块 PNG 落盘，
    // 与运行时色块视觉一致；生成中 / 失败时 Avatar 自然露出首字，不阻塞。
    var generatedUrl by remember(userId) { mutableStateOf<String?>(null) }
    if (remoteUrl == null && userId != null && !isGroup) {
        LaunchedEffect(userId, name, username) {
            generatedUrl = GeneratedAvatarCache
                .ensureInitials(userId.toString(), name, username)
                ?.let { "file://$it" }
        }
    }
    Avatar(
        fallback = resolved.initials,
        url = localCacheUrl ?: remoteUrl ?: generatedUrl,
        size = size,
        shape = RoundedCornerShape(radius),
        backgroundColor = resolved.backgroundColor,
        contentColor = resolved.foregroundColor,
        // 免打扰 → 红点，否则数字
        badgeCount = if (isMuted || unreadCount <= 0) null else unreadCount,
        badgeDot = isMuted && unreadCount > 0,
        online = isOnline,
        onlineColor = colors.onlineStatus,
        contentDescription = name ?: username,
        modifier = modifier,
    )
}
