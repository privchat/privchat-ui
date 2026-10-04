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
     *
     * 读文件系统，**不要在 UI 线程上调**：会话列表滑动时每行都会走到这里。
     */
    fun userAvatarFile(userRoot: String, uid: String): String? =
        userAvatarFiles(userRoot, listOf(uid))[uid]

    fun userAvatarFile(userRoot: String, uid: Long): String? =
        userAvatarFile(userRoot, uid.toString())

    /**
     * 一批用户的头像文件（uid → 路径，没有的不在结果里），只列一次目录。
     *
     * 只读目录项、按文件名匹配；只有同一 uid 残留多个版本时才对这几个候选取修改时间。
     * 以前每查一个 uid 都把整个目录按修改时间排一次序，比较器里逐次取文件属性：群九宫格
     * 9 个成员 × 两轮就是上千次系统调用，压在 UI 线程上，会话列表惯性滑过群聊行时卡住再跳。
     */
    fun userAvatarFiles(userRoot: String, uids: Collection<String>): Map<String, String> {
        val wanted = uids.filter { it.isNotBlank() }.toSet()
        if (wanted.isEmpty()) return emptyMap()
        val dir = "$userRoot/avatars/users"
        return matchUserAvatarFiles(AvatarBitmapRenderer.listFileNames(dir), wanted) { name ->
            AvatarBitmapRenderer.lastModifiedMillis("$dir/$name")
        }.mapValues { (_, name) -> "$dir/$name" }
    }

    /**
     * [names]（目录项）里每个 [wanted] uid 的头像文件名：`{uid}-{指纹}.img`，多个取 [lastModified]
     * 最新的；没有就退回旧布局 `{uid}.img`。生成的字母头像 `{uid}.gen-*.img` 不算。
     */
    internal fun matchUserAvatarFiles(
        names: List<String>,
        wanted: Set<String>,
        lastModified: (String) -> Long,
    ): Map<String, String> {
        val candidates = HashMap<String, MutableList<String>>()
        for (name in names) {
            if (!name.endsWith(".img")) continue
            val dash = name.indexOf('-')
            if (dash <= 0) continue
            val uid = name.substring(0, dash)
            if (uid in wanted) candidates.getOrPut(uid) { mutableListOf() } += name
        }
        val result = HashMap<String, String>()
        for (uid in wanted) {
            val found = candidates[uid]
            val newest = when {
                found == null -> null
                found.size == 1 -> found[0]
                else -> found.maxBy(lastModified)
            }
            if (newest != null) {
                result[uid] = newest
            } else if ("$uid.img" in names) {
                result[uid] = "$uid.img"
            }
        }
        return result
    }
}
