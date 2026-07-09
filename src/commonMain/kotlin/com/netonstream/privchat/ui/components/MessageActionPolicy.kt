package com.netonstream.privchat.ui.components

import com.netonstream.privchat.sdk.dto.ContentMessageType
import com.netonstream.privchat.sdk.dto.MessageEntry
import com.netonstream.privchat.sdk.dto.MessageStatus
import com.netonstream.privchat.sdk.dto.contentType

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
    SaveImage,
    Recall,
    Forward,
    Pin,
    Unpin,
    DeleteLocal,
    Select,
    Report,
}

/**
 * 消息动作可用性策略（纯函数，无副作用）。
 *
 * 规则定稿见产品确认：
 * - 系统消息 → 完全无菜单
 * - 撤回消息 → 仅 DeleteLocal
 * - pending / sending → 仅 DeleteLocal（文案层面显示"取消发送"）
 * - failed → 允许 Recall（语义等价本地删除，不调 revoke RPC）+ Copy/Forward/DeleteLocal/Select；
 *   不给 React（未到达服务端）；重试走气泡旁边的状态图标，非菜单内项。
 * - 正常（Sent / Read）→ 按类型矩阵，Recall 调服务端 revoke RPC
 *
 * 类型矩阵：
 * - Reply：所有类型（失败消息不能被引用）
 * - Copy：TEXT / LINK（链接仅复制 URL）
 * - SaveImage：IMAGE
 * - Recall：isSelf，且满足以下其一：
 *     Sent/Read（调 revokeMessage RPC；撤回无时效，不做客户端时间窗判断）
 *     Failed（UI 层改为本地删除，不调 RPC）
 * - Forward：除 VOICE 外所有类型（VOICE 强绑说话人身份，禁止原样转发）
 * - DeleteLocal / Select：所有类型
 *
 * 反应面板可见性：仅对 Sent / Read 状态的非撤回消息显示。
 */
object MessageActionPolicy {
    data class Context(
        val message: MessageEntry,
        val isSelf: Boolean,
        val nowMs: Long,
        /**
         * 当前用户是否可在该群置顶/取消置顶消息（群主/管理员）。
         * 仅群聊有效；DM 与普通成员恒为 false。服务端仍是权威，本地仅做菜单 gate。
         */
        val canPin: Boolean = false,
        /** 该消息当前是否已被置顶（决定显示「置顶」还是「取消置顶」）。 */
        val isPinned: Boolean = false,
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

        // Copy：文本 / 链接
        if (type == ContentMessageType.TEXT || type == ContentMessageType.LINK) {
            result += MessageActionKind.Copy
        }

        // SaveImage：图片
        if (type == ContentMessageType.IMAGE) {
            result += MessageActionKind.SaveImage
        }

        // Recall：自己的消息；或群主/管理员撤回他人消息（canPin 即
        // 「群主/管理员」权限位，DM/普通成员为 false）。server 端同样鉴权
        // （管理员撤回不受时限），本地不预过滤时间。
        // Failed → UI 层改为本地删除（不调 RPC）；Sent/Read → 调 revokeMessage RPC。
        if (ctx.isSelf || ctx.canPin) {
            result += MessageActionKind.Recall
        }

        // Forward：除语音外都可以；失败消息允许转发（等价于再发一次）
        if (type != ContentMessageType.VOICE) {
            result += MessageActionKind.Forward
        }

        // Pin / Unpin：仅群主/管理员（canPin），且消息已在服务端（非 failed）。
        // 普通成员不显示；server 仍做最终鉴权。
        if (ctx.canPin && !isFailed) {
            result += if (ctx.isPinned) MessageActionKind.Unpin else MessageActionKind.Pin
        }

        // Delete：本端永远允许
        result += MessageActionKind.DeleteLocal

        // Select：批量操作，所有类型都支持
        result += MessageActionKind.Select

        // Report：仅他人消息（不举报自己），失败消息（未到服务端）不给。App Store UGC 1.2。
        if (!ctx.isSelf && !isFailed) result += MessageActionKind.Report

        return result
    }
}
