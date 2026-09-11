package com.netonstream.privchat.ui

import com.netonstream.privchat.ui.common.base.currentTimeMillis

/**
 * 自己撤回掉的文本，留着给「重新编辑」用。
 *
 * 撤回之后消息在本地就只剩一条「你撤回了一条消息」的提示，正文没了。而撤回最常见的原因
 * 是打错一个字——没有这份留存，用户只能把整句重打一遍。
 *
 * 只存在内存里：它是一次会话期间的便利，不是需要跨重启保留的数据；而且撤回的往往正是
 * 用户不想留下的内容，落盘反而不合适。
 *
 * 只对**自己主动撤回的文本消息**留存——别人撤回的内容本来就不该给我看，媒体消息也没有
 * 可回填进输入框的东西。
 */
object RecallDrafts {

    /**
     * 可以重新编辑的时长，与撤回本身的时限同量级（微信是 2 分钟）。
     *
     * 过了就当作用户已经放弃这段话：一直挂着一个「重新编辑」，翻历史时会一直看见。
     */
    const val WINDOW_MILLIS: Long = 2 * 60 * 1000L

    private val texts = mutableMapOf<ULong, Pair<String, Long>>()

    /** 撤回**之前**调用：此刻正文还在。 */
    fun remember(messageId: ULong, text: String) {
        if (text.isBlank()) return
        texts[messageId] = text to currentTimeMillis()
        prune()
    }

    /** 仍在时限内的原文；过期或没有则返回 null。 */
    fun peek(messageId: ULong): String? {
        val (text, at) = texts[messageId] ?: return null
        if (currentTimeMillis() - at > WINDOW_MILLIS) {
            texts.remove(messageId)
            return null
        }
        return text
    }

    /** 已经回填进输入框，就不该再留着第二次。 */
    fun consume(messageId: ULong): String? = peek(messageId)?.also { texts.remove(messageId) }

    private fun prune() {
        val now = currentTimeMillis()
        texts.entries.removeAll { now - it.value.second > WINDOW_MILLIS }
    }
}
