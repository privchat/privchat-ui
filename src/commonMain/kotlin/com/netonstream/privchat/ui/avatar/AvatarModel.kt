package com.netonstream.privchat.ui.avatar

/**
 * 头像新鲜度（CLIENT_GLOBAL_STATE_AND_IDENTITY_STORE_SPEC §4.2，判据沿用 AVATAR_CACHE_SPEC）。
 * P1 只用 FRESH_LOCAL / REMOTE_ONLY / FALLBACK；STALE_LOCAL（avatar_cached_url 比对）留 P2。
 */
enum class AvatarFreshness { FRESH_LOCAL, STALE_LOCAL, REMOTE_ONLY, FALLBACK }

/**
 * 头像统一模型（CLIENT_GLOBAL_STATE_AND_IDENTITY_STORE_SPEC §4.1）。
 *
 * 由 AvatarStore 产出：`localPath` 是已判存在/新鲜的本地缓存文件（AvatarStore 唯一产出，
 * UI 不得自己推导 `avatars/users/{uid}.img`）。UI 只 `PrivChatAvatar(model = ...)`，不再各自
 * 拼 `remoteUrl` / `preferLocalCache` / initials。
 */
data class AvatarModel(
    val userId: Long? = null,
    val displayName: String? = null,
    val username: String? = null,
    /** AvatarStore 解析出的本地缓存文件绝对路径（已判存在）；优先渲染，near-instant 无闪烁。 */
    val localPath: String? = null,
    val remoteUrl: String? = null,
    /** = SDK avatar_cached_url；P2 起用于 freshness 比对。 */
    val version: String? = null,
    val freshness: AvatarFreshness = AvatarFreshness.FALLBACK,
    val isGroup: Boolean = false,
    /** hash 色种子（`"u:<uid>"` / `"g:<channelId>"`）；不传由 resolver 兜底。 */
    val seed: String? = null,
)
