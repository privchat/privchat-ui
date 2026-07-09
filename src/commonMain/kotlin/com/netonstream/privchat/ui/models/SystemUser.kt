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

    /** 系统用户 → 本地化「系统消息」;否则原样返回 [fallback]。 */
    fun displayNameOr(username: String?, fallback: String): String =
        if (isSystem(username)) PrivChatI18n.current.systemMessagesName else fallback
}
