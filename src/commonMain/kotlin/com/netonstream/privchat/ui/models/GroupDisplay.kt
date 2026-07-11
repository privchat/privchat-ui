package com.netonstream.privchat.ui.models

import com.netonstream.privchat.ui.i18n.PrivChatI18n

/**
 * 群标题统一规则（P6-1，CLIENT_GLOBAL_STATE §22）。
 *
 * 收口此前 3 套不一致的群名 fallback：
 * - `SystemUser.channelTitle`（判 blank → groupChatFallback，会话列表/chat 头/成员页复用）
 * - `GroupEntry.displayName`（判 null → 硬编码「群聊」，仅 GroupList）
 * - `ChatSettingsPage` 群名 Cell（裸 `channel.name`，无 fallback）
 *
 * 唯一入口：自定义群名非空 → 群名；否则本地化 groupChatFallback（任何情况不出现裸 id/空串）。
 *
 * 备注：微信式「张三、李四等 N 人」成员派生标题需新增参数化 i18n key（4 语言包），
 * 作为 follow-up；本版先保证「同一群各入口群名一致」这一硬验收。
 */
object GroupDisplay {
    fun titleOf(name: String?): String =
        name?.trim()?.takeIf { it.isNotEmpty() } ?: PrivChatI18n.current.groupChatFallback
}
