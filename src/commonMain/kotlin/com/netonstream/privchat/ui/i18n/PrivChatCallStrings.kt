package com.netonstream.privchat.ui.i18n

import com.netonstream.privchat.sdk.dto.call.CallHistory
import com.netonstream.privchat.sdk.dto.call.CallMediaKind
import com.netonstream.privchat.sdk.dto.call.CallEndReason

data class PrivChatCallStrings(
    val audio: String,
    val video: String,
    val incoming: String,
    val ringing: String,
    val active: String,
    val accept: String,
    val reject: String,
    val hangUp: String,
    val mute: String,
    val unmute: String,
    val cameraOn: String,
    val cameraOff: String,
    val speakerOn: String,
    val speakerOff: String,
    val showVideo: String,
    val unavailable: String,
    val disabled: String,
    val participantLimit: String,
    val busy: String,
    val mediaError: String,
    val ended: String,
    val rejected: String,
    val unanswered: String,
    val peerBusy: String,
    val cancelled: String,
    val failed: String,
    val historyFormat: String,
    val channelFallback: String,
    val userFallback: String,
    val initiatedBy: String,
    val routingError: String,
)

fun PrivChatCallStrings.channelLabel(id: ULong, name: String?): String =
    name?.takeIf { it.isNotBlank() && it != id.toString() }
        ?: channelFallback.replace("{0}", id.toString())

fun PrivChatCallStrings.initiatorLabel(id: ULong, name: String?): String = initiatedBy.replace("{0}",
    name?.takeIf { it.isNotBlank() && it != id.toString() && it != id.toLong().toString() }
        ?: userFallback.replace("{0}", id.toString()))

fun PrivChatCallStrings.historySummary(history: CallHistory): String {
    val seconds = history.durationMs.coerceAtLeast(0) / 1_000
    val duration = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    val reason = when (history.endReason) {
        CallEndReason.REJECTED -> rejected
        CallEndReason.TIMEOUT -> unanswered
        CallEndReason.BUSY -> peerBusy
        CallEndReason.CANCELLED -> cancelled
        CallEndReason.FAILED -> failed
        CallEndReason.NORMAL, null -> ended
    }
    return historyFormat.replace("{0}", if (history.mediaKind == CallMediaKind.VIDEO) video else audio)
        .replace("{1}", reason).replace("{2}", duration)
}

internal val enCall = PrivChatCallStrings(
    "Voice call", "Video call", "Incoming call", "Calling...", "In call", "Answer", "Decline", "Hang up",
    "Mute", "Unmute", "Camera on", "Camera off", "Speaker on", "Speaker off", "Show video",
    "Calling is not available on this device", "Calling is disabled by the server",
    "Too many participants for this call", "Finish the current call first",
    "Could not use the microphone or camera. Check permissions and retry.",
    "Ended", "Declined", "Unanswered", "Busy", "Cancelled", "Failed", "{0} · {1} · {2}",
    "Channel #{0}", "User #{0}", "Caller: {0}",
    "Could not change the audio output. The call is still connected.",
)
internal val zhCall = PrivChatCallStrings(
    "语音通话", "视频通话", "来电", "正在呼叫…", "通话中", "接听", "拒绝", "挂断",
    "静音", "取消静音", "打开摄像头", "关闭摄像头", "打开扬声器", "关闭扬声器", "显示视频",
    "此设备暂不支持通话", "服务器未启用通话", "通话人数超过限制", "请先结束当前通话",
    "无法使用麦克风或摄像头，请检查权限后重试。",
    "已结束", "已拒绝", "未接听", "对方忙", "已取消", "通话失败", "{0} · {1} · {2}",
    "频道 #{0}", "用户 #{0}", "发起人：{0}",
    "无法切换音频输出，通话仍保持连接。",
)
internal val hantCall = PrivChatCallStrings(
    "語音通話", "視訊通話", "來電", "正在呼叫…", "通話中", "接聽", "拒絕", "掛斷",
    "靜音", "取消靜音", "開啟攝影機", "關閉攝影機", "開啟擴音", "關閉擴音", "顯示視訊",
    "此裝置暫不支援通話", "伺服器未啟用通話", "通話人數超過限制", "請先結束目前通話",
    "無法使用麥克風或攝影機，請檢查權限後重試。",
    "已結束", "已拒絕", "未接聽", "對方忙碌", "已取消", "通話失敗", "{0} · {1} · {2}",
    "頻道 #{0}", "使用者 #{0}", "發起人：{0}",
    "無法切換音訊輸出，通話仍保持連線。",
)
internal val viCall = PrivChatCallStrings(
    "Gọi thoại", "Gọi video", "Cuộc gọi đến", "Đang gọi...", "Đang trong cuộc gọi", "Trả lời", "Từ chối", "Kết thúc",
    "Tắt mic", "Bật mic", "Bật camera", "Tắt camera", "Bật loa", "Tắt loa", "Hiện video",
    "Thiết bị này chưa hỗ trợ cuộc gọi", "Máy chủ chưa bật cuộc gọi", "Quá nhiều người tham gia", "Hãy kết thúc cuộc gọi hiện tại",
    "Không thể dùng mic hoặc camera. Hãy kiểm tra quyền và thử lại.",
    "Đã kết thúc", "Đã từ chối", "Không trả lời", "Máy bận", "Đã hủy", "Thất bại", "{0} · {1} · {2}",
    "Kênh #{0}", "Người dùng #{0}", "Người gọi: {0}",
    "Không thể đổi đầu ra âm thanh. Cuộc gọi vẫn được kết nối.",
)
