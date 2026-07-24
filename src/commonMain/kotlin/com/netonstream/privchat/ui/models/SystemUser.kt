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

    /**
     * 显示名**单点规则**(用户拍板,2026-07-24):账号是系统类型(userType==1)时,
     * 按其 username 查语言包——**有对应词条才替换,没有就显示原名**;非系统账号
     * 一律原名。所有取显示名的地方收敛到这里,不允许调用点二次处理。
     *
     * 语言包映射按 username 扩展(未来更多系统账号在此登记)。
     */
    fun localizedNameFor(name: String, username: String?, userType: Int?): String {
        if (!isSystemType(userType) && !isSystem(username)) return name
        return when (username) {
            "system", "__system_1__" -> PrivChatI18n.current.systemMessagesName
            else -> name
        }
    }

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
        // 数据层已带出 DM 对端 userType/username(本地 user 实体在场时),
        // 单点规则同步判定,零网络零二次处理;peerUserType 缺席时退回
        // resolveUid 异步缓存(isSystemUid)与 username 兼容通道。
        entry.isDm -> localizedNameFor(
            name = when {
                entry.name.isNotBlank() -> entry.name
                else -> entry.peerUserId?.toString() ?: ""
            },
            username = entry.peerUsername ?: entry.name.takeIf { isSystem(it) },
            userType = entry.peerUserType
                ?: if (isSystemUid(entry.peerUserId)) 1 else null,
        )
        entry.name.isBlank() -> PrivChatI18n.current.groupChatFallback
        else -> entry.name
    }

    /**
     * 后台解析 peer 的 username/userType(local-first,一次一 uid,失败允许重试)。
     * [sourceChannelId]:远程拉取的资料可见性来源(会话场景传共同会话 id,
     * 服务端按会话成员放行;不传则退化为好友来源,对非好友对端会被闸口拒绝)。
     */
    suspend fun resolveUid(uid: ULong, sourceChannelId: ULong? = null) {
        if (!checkedUids.add(uid)) return
        runCatching {
            com.netonstream.privchat.ui.PrivChat.client.getUserProfileLocalFirst(uid, sourceChannelId)
        }
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
