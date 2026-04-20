package com.netonstream.privchat.ui.components

import om.netonstream.privchat.sdk.dto.ContentMessageType
import om.netonstream.privchat.sdk.dto.MessageEntry
import om.netonstream.privchat.sdk.dto.MessageStatus
import om.netonstream.privchat.sdk.dto.contentType

/**
 * 长按动作菜单的枚举集合。UI 层把 kind 映射到 label / icon / callback。
 *
 * DeleteLocal 同时承担两层语义：
 * - 正常消息 → "本地删除"
 * - pending/sending → "取消发送（本地删除）"
 * 文案差异由 UI 层根据 [MessageActionPolicy.Context.message.status] 决定。
 */
enum class MessageActionKind {
    Reply,
    Copy,
    Recall,
    Forward,
    DeleteLocal,
    DeleteForAll,
    Select,
}

/**
 * 消息动作可用性策略（纯函数，无副作用）。
 *
 * 规则定稿见产品确认：
 * - 系统消息 → 完全无菜单
 * - 撤回消息 → 仅 DeleteLocal
 * - pending / sending → 仅 DeleteLocal（文案层面显示"取消发送"）
 * - failed → 允许 Recall（语义等价本地删除，不调 revoke RPC）+ Copy/Forward/DeleteLocal/Select；
 *   不给 DeleteForAll（从未到达服务端）；不给 React（未到达服务端）；
 *   重试走气泡旁边的状态图标，非菜单内项。
 * - 正常（Sent / Read）→ 按类型矩阵，Recall 调服务端 revoke RPC
 *
 * 类型矩阵：
 * - Reply：所有类型（失败消息不能被引用）
 * - Copy：TEXT / IMAGE / LINK（图片复制字节，链接仅复制 URL）
 * - Recall：isSelf，且满足以下其一：
 *     Sent/Read 且发送时间 ≤ 5min（调 revokeMessage RPC）
 *     Failed（UI 层改为本地删除，不调 RPC）
 * - Forward：除 VOICE 外所有类型（VOICE 强绑说话人身份，禁止原样转发）
 * - DeleteLocal / Select：所有类型
 * - DeleteForAll：所有类型，但失败消息无意义（从未送达）
 *
 * 反应面板可见性：仅对 Sent / Read 状态的非撤回消息显示。
 */
object MessageActionPolicy {
    /** 撤回时间窗（毫秒），参考微信 5 分钟。服务端仍可能拒绝，本地仅做 UI 预过滤。 */
    private const val RecallWindowMs: Long = 5L * 60L * 1000L

    data class Context(
        val message: MessageEntry,
        val isSelf: Boolean,
        val nowMs: Long,
    )

    /**
     * 该消息是否应显示长按菜单。
     * 系统消息（SYSTEM content type）永远返回 false；撤回消息仍然返回 true（只给本地删除）。
     */
    fun isMenuAvailable(ctx: Context): Boolean {
        val type = ctx.message.contentType()
        if (type == ContentMessageType.SYSTEM) return false
        return true
    }

    /** 反应面板是否可见（撤回 / 未到达服务端的消息都不允许反应）。 */
    fun canReact(ctx: Context): Boolean {
        val msg = ctx.message
        if (msg.isRevoked) return false
        val type = msg.contentType()
        if (type == ContentMessageType.SYSTEM) return false
        return when (msg.status) {
            MessageStatus.Sent, MessageStatus.Read -> true
            else -> false
        }
    }

    /** 按显示顺序返回菜单项集合（调用方已经决定要显示菜单）。 */
    fun menuActions(ctx: Context): List<MessageActionKind> {
        val msg = ctx.message

        // 撤回消息：仅本地删除
        if (msg.isRevoked) return listOf(MessageActionKind.DeleteLocal)

        // pending / sending：仅本地删除（UI 层文案改为"取消发送"）
        if (msg.status == MessageStatus.Pending || msg.status == MessageStatus.Sending) {
            return listOf(MessageActionKind.DeleteLocal)
        }

        val type = msg.contentType()
        val isFailed = msg.status == MessageStatus.Failed
        val result = mutableListOf<MessageActionKind>()

        // Reply：失败消息不能被引用回复（还没到服务端）
        if (!isFailed) result += MessageActionKind.Reply

        // Copy：文本 / 图片 / 链接
        if (type == ContentMessageType.TEXT ||
            type == ContentMessageType.IMAGE ||
            type == ContentMessageType.LINK
        ) {
            result += MessageActionKind.Copy
        }

        // Recall：isSelf 必须。Failed 任意时间都允许（UI 层改为本地删除，不调 RPC）；
        // Sent/Read 仅 5min 内允许（调 revokeMessage RPC）。
        if (ctx.isSelf) {
            val canRecall = if (isFailed) {
                true
            } else {
                val ageMs = ctx.nowMs - msg.timestamp.toLong()
                ageMs in 0L..RecallWindowMs
            }
            if (canRecall) result += MessageActionKind.Recall
        }

        // Forward：除语音外都可以；失败消息允许转发（等价于再发一次）
        if (type != ContentMessageType.VOICE) {
            result += MessageActionKind.Forward
        }

        // Delete：本端永远允许；所有人删除对失败消息无意义（从未送达）
        result += MessageActionKind.DeleteLocal
        if (!isFailed) result += MessageActionKind.DeleteForAll

        // Select：批量操作，所有类型都支持
        result += MessageActionKind.Select

        return result
    }
}
