// 转发的重发计划。原本住在 privchat-app 里，下沉到这里是因为 live-chat 也要转发，
// 而 SDK 已经把 forwardMessage 拿掉了（91afdfb），转发从此是「读原消息 + 按类型重发」
// 这套上层组合。两个产品各抄一份的话，媒体重发那几条分支迟早只在其中一份里被修。
//
// 放 privchat-ui 而不是 sdk-kotlin：SDK 移除 forwardMessage 就是要把这层组合留在上面；
// 而它零 UI 依赖（只用 PrivchatClient / MessageEntry），放这里不会把 UI 拖进来。
package com.netonstream.privchat.ui.forward

import com.netonstream.privchat.sdk.PrivchatClient
import com.netonstream.privchat.sdk.dto.DownloadedAttachment
import com.netonstream.privchat.sdk.dto.MessageContentKind
import com.netonstream.privchat.sdk.dto.MessageEntry

/**
 * 「转发」在这套架构里不是一种消息，也不是一条 RPC——它就是**把同一份内容，以自己的身份，重新发一次**。
 *
 * 所以这里只做一件事：看一眼源消息，决定重新发的时候调哪个普通发送接口。没有 forward 类型、没有
 * 来源关系、没有服务端转发路由。附件之所以不重传字节，是上传预检那层按内容摘要命中的，跟这里无关。
 */
sealed interface ResendPlan {

    /** 文本原样重发。 */
    data class Text(val text: String) : ResendPlan

    /**
     * 附件重发。
     *
     * [localPath] 有值就直接发那个文件——它已经在受管目录里，**不做二次压缩/转码**（原图/原视频）。
     * 没有值说明本地还没下载过，得先按 [fileId]/[fileUrl] 拉到缓存再发。
     * [voiceDurationMs] 非空表示按语音发（语音时长是消息的一部分，走普通附件会丢）。
     */
    data class Attachment(
        val localPath: String?,
        val fileId: String?,
        val fileUrl: String?,
        val voiceDurationMs: Long?,
        /** 源文件名/MIME：下载缓存要靠它保住扩展名，不然图片会被重发成「文件」。 */
        val fileName: String?,
        val mimeType: String?,
        /** 源消息的说明文字。丢了它，重发出去的就不是同一条消息。 */
        val caption: String?,
    ) : ResendPlan

    /** 这条消息重新发一次没有意义（红包/转账/系统消息），或者已经撤回了。 */
    data object Unsupported : ResendPlan
}

/** 决定一条消息该怎么重新发。 */
fun planResend(message: MessageEntry): ResendPlan {
    if (message.isRevoked) return ResendPlan.Unsupported
    val body = message.body
    return when (body.kind) {
        MessageContentKind.Text,
        MessageContentKind.Link,
        -> body.text.takeIf { it.isNotBlank() }?.let(ResendPlan::Text) ?: ResendPlan.Unsupported

        MessageContentKind.Image,
        MessageContentKind.Video,
        MessageContentKind.File,
        MessageContentKind.Voice,
        -> {
            val localPath = message.localMediaPath?.takeIf { it.isNotBlank() }
            val fileUrl = body.attachmentUrl?.takeIf { it.isNotBlank() }
            val fileId = body.attachmentFileId?.toString()
            // 本地没有、又没有任何取回内容的办法（id 和 URL 都没有），就没有东西可发。
            if (localPath == null && fileUrl == null && fileId == null) {
                ResendPlan.Unsupported
            } else {
                ResendPlan.Attachment(
                    localPath = localPath,
                    fileId = fileId,
                    fileUrl = fileUrl,
                    voiceDurationMs = if (body.kind == MessageContentKind.Voice) {
                        (body.duration?.toLong() ?: 0L).coerceAtLeast(1L) * 1000L
                    } else {
                        null
                    },
                    fileName = body.fileName?.takeIf { it.isNotBlank() },
                    mimeType = message.mimeType?.takeIf { it.isNotBlank() },
                    caption = body.text.takeIf { it.isNotBlank() },
                )
            }
        }

        // 表情包重新发一次只能退化成图片/文件——普通发送没有 sticker 这个类型可传。
        // 与其把消息种类悄悄换掉，不如先不支持。
        MessageContentKind.Sticker,
        MessageContentKind.System,
        MessageContentKind.Contact,
        MessageContentKind.Location,
        MessageContentKind.Forward,
        MessageContentKind.RedPacket,
        MessageContentKind.MoneyTransfer,
        MessageContentKind.Unknown,
        -> ResendPlan.Unsupported
    }
}

/**
 * 执行重发。发送动作以函数形参注入，方便按次序断言（先下载后发、本地已有就别下载）。
 */
suspend fun executeResendPlan(
    plan: ResendPlan,
    channelId: ULong,
    channelType: Int,
    sendText: suspend (ULong, Int, String) -> Result<*>,
    sendMedia: suspend (channelId: ULong, path: String, displayFileName: String?, caption: String?) -> Result<*>,
    sendVoice: suspend (ULong, String, Long) -> Result<*>,
    downloadToCache: suspend (fileId: String, fileUrl: String, fileName: String?, mimeType: String?) -> Result<DownloadedAttachment>,
): Result<Unit> = when (plan) {
    is ResendPlan.Text -> sendText(channelId, channelType, plan.text).map { }

    is ResendPlan.Attachment -> {
        // 本地已有就用本地那份；否则下载，**并且用 SDK 给回来的路径和名字**。
        // 曾经这里只拿路径、名字沿用本地消息上的值：对方客户端没写文件名时就成了空，
        // 一张图于是被当成「文件」重发出去。
        var downloadedName: String? = null
        var downloadedMime: String? = null
        val path = plan.localPath ?: run {
            // 🔴 id 和 URL 有一个就够：老消息只有 URL，新消息只有 file_id。
            // 两个都要的话，这两类消息都会重发不出去。
            val fileId = plan.fileId.orEmpty()
            val fileUrl = plan.fileUrl.orEmpty()
            if (fileId.isEmpty() && fileUrl.isEmpty()) {
                null
            } else {
                downloadToCache(fileId, fileUrl, plan.fileName, plan.mimeType).getOrNull()?.also {
                    downloadedName = it.displayFileName.takeIf { name -> name.isNotBlank() }
                    downloadedMime = it.mimeType.takeIf { mime -> mime.isNotBlank() }
                }?.localPath
            }
        }
        // 展示名优先用服务端那个：本地消息上的名字可能压根没有。
        val displayName = downloadedName ?: plan.fileName
        when {
            path == null -> Result.failure(IllegalStateException("attachment unavailable"))
            plan.voiceDurationMs != null -> sendVoice(channelId, path, plan.voiceDurationMs).map { }
            else -> sendMedia(channelId, path, displayName, plan.caption).map { }
        }
    }

    ResendPlan.Unsupported -> Result.failure(IllegalStateException("message cannot be resent"))
}

/**
 * 把一条消息重新发到某个会话——**长按「转发」最终走的就是这个函数**。
 *
 * 接线（planResend + 四个发送接口）只此一处：UI 和运行时验收测试调的是同一段代码。
 * 各自再接一遍的话，测试绿了也只能证明「测试那份接线是对的」。
 */
suspend fun resendMessageToChannel(
    client: PrivchatClient,
    message: MessageEntry,
    channelId: ULong,
    channelType: Int,
): Result<Unit> = executeResendPlan(
    plan = planResend(message),
    channelId = channelId,
    channelType = channelType,
    sendText = { cid, ctype, text -> client.sendText(cid, ctype, text) },
    sendMedia = { cid, path, name, caption -> client.sendMedia(cid, path, null, name, caption) },
    sendVoice = { cid, path, ms -> client.sendVoiceFromPath(cid, path, ms, null, null) },
    downloadToCache = { fileId, fileUrl, name, mime ->
        client.downloadAttachmentDetailed(fileId, fileUrl, null, name, mime)
    },
)
