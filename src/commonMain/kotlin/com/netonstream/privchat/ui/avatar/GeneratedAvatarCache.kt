package com.netonstream.privchat.ui.avatar

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * App 登录后注入当前账号的 userRoot（`{dataDir}/users/{selfUid}`），生成头像落盘用。
 * 未设置时 [GeneratedAvatarCache] 全部返回 null（调用方回退运行时绘制）。
 */
object AvatarLocalCache {
    /** 登录时（主线程）赋值、生成协程读取；String? 的读写在此处竞争无害（最坏读到
     *  null → 该次回退运行时绘制），故不加 JVM-only 的 @Volatile。 */
    var userRoot: String? = null
}

/**
 * 无头像用户 initials PNG + 群九宫格 PNG 的「生成一次落盘」缓存（AVATAR_CACHE_SPEC §5.2/§5.3）。
 *
 * 纯客户端：按确定性路径 `avatars/users|groups/{id}.img` 落盘（与 SDK 下载头像同槽位/同布局），
 * UI 命中即 `file://` 加载，不再每次运行时用 Compose 逐格绘制。
 */
object GeneratedAvatarCache {
    /** 落盘分辨率：覆盖到 xl(64dp)*3x 以上，initials/九宫格都够清晰，文件也不大。 */
    private const val SIZE_PX = 256

    private val mutex = Mutex()
    private val readyUsers = mutableSetOf<String>() // 已确认落盘的 user 路径
    private val collageFp = mutableMapOf<String, String>() // gid -> 上次合成的成员指纹

    /** 群九宫格取前 9 成员的最小信息。 */
    data class CollageMember(val uid: String, val name: String?, val username: String?)

    /** 确保无头像用户的 initials PNG 已落盘；返回文件绝对路径，不可用返回 null。 */
    suspend fun ensureInitials(uid: String, displayName: String?, username: String?): String? {
        val root = AvatarLocalCache.userRoot ?: return null
        if (uid.isBlank()) return null
        val path = "$root/avatars/users/$uid.img"
        if (path in readyUsers) return path
        if (AvatarBitmapRenderer.fileExists(path)) {
            readyUsers += path
            return path
        }
        return mutex.withLock {
            if (path in readyUsers || AvatarBitmapRenderer.fileExists(path)) {
                readyUsers += path
                return@withLock path
            }
            val initials = AvatarText.initialsOf(displayName, username, uid.toLongOrNull())
            val ok = AvatarBitmapRenderer.renderInitials(
                initials = initials,
                bgArgb = AvatarPalette.hashBackgroundArgb("u:$uid"),
                fgArgb = AvatarPalette.HASH_FOREGROUND_ARGB,
                sizePx = SIZE_PX,
                outPath = path,
            )
            if (ok) {
                readyUsers += path
                path
            } else {
                null
            }
        }
    }

    /** 确保群九宫格 PNG 已落盘（成员或其头像下载态变化才重合成）；返回路径或 null。 */
    suspend fun ensureCollage(gid: String, members: List<CollageMember>): String? {
        val root = AvatarLocalCache.userRoot ?: return null
        if (gid.isBlank() || members.isEmpty()) return null
        val path = "$root/avatars/groups/$gid.img"
        val cells: List<CollageCell> = members.take(9).map { m ->
            val memberImg = "$root/avatars/users/${m.uid}.img"
            if (AvatarBitmapRenderer.fileExists(memberImg)) {
                CollageCell.Image(memberImg)
            } else {
                CollageCell.Initials(
                    text = AvatarText.initialsOf(m.name, m.username, m.uid.toLongOrNull()),
                    bgArgb = AvatarPalette.hashBackgroundArgb("u:${m.uid}"),
                    fgArgb = AvatarPalette.HASH_FOREGROUND_ARGB,
                )
            }
        }
        // 指纹 = 每格类型+内容：成员增删 / 某成员头像下载完成（Initials→Image）都会变。
        val fp = cells.joinToString("|") { c ->
            when (c) {
                is CollageCell.Image -> "i:${c.filePath}"
                is CollageCell.Initials -> "t:${c.text}:${c.bgArgb}"
                CollageCell.Empty -> "e"
            }
        }
        return mutex.withLock {
            if (collageFp[gid] == fp && AvatarBitmapRenderer.fileExists(path)) {
                return@withLock path
            }
            val ok = AvatarBitmapRenderer.renderCollage(cells, SIZE_PX, path)
            if (ok) {
                collageFp[gid] = fp
                path
            } else {
                null
            }
        }
    }

    /** 登出/切号：清进程内记忆（文件随 `users/{selfUid}` 目录回收）。 */
    fun clear() {
        readyUsers.clear()
        collageFp.clear()
    }
}
