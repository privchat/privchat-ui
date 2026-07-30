package com.netonstream.privchat.ui.models

/**
 * SDK 本地 user 表的 UI 投影——显示名与头像所需的最小集。
 *
 * 由 `user` 实体增量同步维护，覆盖当前账号所有会话的全部成员
 * （服务端 `entity/sync_entities("user")` 的 related_user_ids =
 * 好友 ∪ 所有频道成员）。
 *
 * [username] 可空是**权限语义**，不是「没有」：PROFILE_VISIBILITY §2 把 username
 * 定为 L1 联络凭证，仅好友可见，陌生人拿到的公开投影里它就是缺的。显示名链走到
 * 这一级时若为空，就继续往下兜底，不要把它当成数据缺陷去补拉。
 */
data class UserProfileSnapshot(
    val userId: ULong,
    val nickname: String,
    val username: String?,
    val avatar: String,
    val userType: Int,
)
