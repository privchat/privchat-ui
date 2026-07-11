package com.netonstream.privchat.ui.models

import com.netonstream.privchat.ui.i18n.PrivChatI18n

/**
 * 系统用户识别(三端统一规则,GPT 红线):判定依据是 username=="system"
 * (兼容历史 "__system_1__" 过渡),**绝不使用 uid==1 作为逻辑依据**——
 * uid=1 只是当前部署事实,不同环境不保证。显示名按客户端语言本地化,
 * 语言包缺失时回落 server 下发昵称。
 */
object SystemUser {
    fun isSystem(username: String?): Boolean =
        username == "system" || username == "__system_1__"

    /**
     * 按「用户类型」判定系统账号(user_type==1,与服务端 USER_TYPE_SYSTEM 一致)。
     * 这是判定系统账号的权威依据——uid 只是部署事实,username 是兼容通道;
     * 有 user_type 时优先按类型判(GPT 红线:禁用 uid 作逻辑依据)。null=未知→非系统。
     */
    fun isSystemType(userType: Int?): Boolean = userType == 1

    /** 系统用户 → 本地化「系统消息」;否则原样返回 [fallback]。 */
    fun displayNameOr(username: String?, fallback: String): String =
        if (isSystem(username)) PrivChatI18n.current.systemMessagesName else fallback

    // ── 会话列表场景:条目只有 peerUserId,按 profile 查 username 后缓存判定 ──
    // (依据仍是 username/userType,uid 集合只是查询结果缓存,不违反「禁 uid 判定」红线)

    private val systemUidsState = androidx.compose.runtime.mutableStateOf<Set<ULong>>(emptySet())
    private val checkedUids = mutableSetOf<ULong>()

    fun isSystemUid(uid: ULong?): Boolean =
        uid != null && systemUidsState.value.contains(uid)

    /**
     * 会话标题统一入口:DM 对端为系统用户 → 本地化「系统消息」;
     * 群且 name 为空(SDK 全回退落空)→ 本地化「群聊」——标题任何情况不出现裸 id。
     */
    fun channelTitle(entry: com.netonstream.privchat.sdk.dto.ChannelListEntry): String = when {
        entry.isDm && isSystemUid(entry.peerUserId) -> PrivChatI18n.current.systemMessagesName
        !entry.isDm && entry.name.isBlank() -> PrivChatI18n.current.groupChatFallback
        else -> entry.name
    }

    /** 后台解析 peer 的 username/userType(local-first,一次一 uid,失败允许重试)。 */
    suspend fun resolveUid(uid: ULong) {
        if (!checkedUids.add(uid)) return
        runCatching { com.netonstream.privchat.ui.PrivChat.client.getUserProfileLocalFirst(uid) }
            .getOrNull()
            ?.fold(
                onSuccess = { profile ->
                    if (isSystem(profile.username) || profile.userType.toInt() == 1) {
                        systemUidsState.value = systemUidsState.value + uid
                    }
                },
                onFailure = { checkedUids.remove(uid) },
            ) ?: checkedUids.remove(uid)
    }
}
