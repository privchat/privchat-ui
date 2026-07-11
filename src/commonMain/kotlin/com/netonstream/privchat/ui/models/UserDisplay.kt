package com.netonstream.privchat.ui.models

import com.netonstream.privchat.ui.i18n.PrivChatI18n

/**
 * 用户展示名的**唯一规则入口**（CLIENT_GLOBAL_STATE_AND_IDENTITY_STORE_SPEC §5.2 P3.5）。
 *
 * 所有页面 / SDK 类型扩展的 displayName 都收口到这里，保证**同一 userId 在会话列表、聊天页顶部、
 * 好友列表、群成员页、个人资料页、搜索结果里显示名完全一致**。
 *
 * 规则：
 *  - 系统用户（`user_type == 1` 或 `username == "system"`）→ 本地化「系统消息」（按 user_type 优先，禁用 uid）。
 *  - 否则：备注名 > 昵称 > username > userId。
 */
object UserDisplay {
    fun of(
        username: String?,
        nickname: String?,
        remark: String? = null,
        userId: Long? = null,
        userType: Int? = null,
    ): String {
        if (SystemUser.isSystem(username) || SystemUser.isSystemType(userType)) {
            return PrivChatI18n.current.systemMessagesName
        }
        return remark?.takeIf { it.isNotBlank() }
            ?: nickname?.takeIf { it.isNotBlank() }
            ?: username?.takeIf { it.isNotBlank() }
            ?: userId?.toString()
            ?: ""
    }
}
