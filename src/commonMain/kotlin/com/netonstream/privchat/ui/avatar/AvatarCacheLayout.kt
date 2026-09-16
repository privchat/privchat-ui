package com.netonstream.privchat.ui.avatar

/**
 * SDK 头像缓存目录的文件名约定（AVATAR_CACHE_SPEC §3）。
 *
 * 真实头像文件名是 `{uid}-{指纹}.img`，指纹由 SDK 按远端 URL 算；换头像 ⇒ 换文件名。
 *
 * 🔴 **UI 不能按 `{uid}.img` 盲探**。那是旧布局：换头像原地覆盖同一个文件，图片加载器按
 * URL 缓存，URL 不变就一直给旧位图——换完头像界面纹丝不动，要杀进程重进才看得到新图。
 * 也别试图用 `?v=` / `#v=` 给路径加版本号骗缓存：两者都会被当成文件名的一部分，文件直接
 * 打不开，头像掉回字母占位（实测）。唯一正确的做法就是让文件名本身随内容变。
 *
 * 生成式头像（initials / 九宫格）走 `{uid}.gen-*.img`，与真实头像物理分离，见
 * [GeneratedAvatarCache]。
 */
object AvatarCacheLayout {

    /**
     * 找出 [uid] 当前的真实头像缓存文件绝对路径；没有返回 null。
     *
     * 正常情况下同一 uid 只会有一个文件（SDK 写入新版本后会删掉旧版本），但清理失败时可能
     * 残留多个，所以按修改时间取最新的那个。同时兼容旧布局 `{uid}.img`——老装机升级上来
     * 磁盘上就是这个名字，直到下次换头像才会变成带指纹的。
     */
    fun userAvatarFile(userRoot: String, uid: String): String? {
        if (uid.isBlank()) return null
        val dir = "$userRoot/avatars/users"
        val prefix = "$uid-"
        val newest = AvatarBitmapRenderer
            .listFilesNewestFirst(dir)
            .firstOrNull { it.startsWith(prefix) && it.endsWith(".img") }
        if (newest != null) return "$dir/$newest"
        val legacy = "$dir/$uid.img"
        return if (AvatarBitmapRenderer.fileExists(legacy)) legacy else null
    }

    fun userAvatarFile(userRoot: String, uid: Long): String? =
        userAvatarFile(userRoot, uid.toString())
}
