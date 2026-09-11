package com.netonstream.privchat.ui.i18n

// 生成/维护说明：i18n 文案按语义域拆分（gearui 标准：分域小 data class，避免
// 单类字段爆炸触发 DEX 方法字节码 / 255 参数上限）。门面 PrivChatStrings 用委托
// getter 暴露扁平 `strings.xxx` API 保持调用点稳定。新增文案加到对应域即可。

import androidx.compose.runtime.Immutable

@Immutable
data class PrivChatCommonStrings(
    val appName: String,
    val confirm: String,
    val cancel: String,
    val save: String,
    val edit: String,
    val delete: String,
    val search: String,
    val loading: String,
    val retry: String,
    val noData: String,
    val networkError: String,
    /** 会话尚未就绪（连接中/重连中）时的可重试提示。 */
    val connectionNotReady: String,
    /**
     * 远程推送的兜底标题/正文。
     *
     * 推送 payload 里没有会话名——服务端不知道这台设备用哪种语言，客户端此刻也
     * 未必已经同步到这个会话（可能是被这条推送刚拉起来的冷进程）。进 App 之后
     * 看到的才是真实会话名。
     */
    val pushDefaultTitle: String,
    val pushDefaultBody: String,
)

data class PrivChatCommonStringsPatch(
    val appName: String? = null,
    val confirm: String? = null,
    val cancel: String? = null,
    val save: String? = null,
    val edit: String? = null,
    val delete: String? = null,
    val search: String? = null,
    val loading: String? = null,
    val retry: String? = null,
    val noData: String? = null,
    val networkError: String? = null,
    val connectionNotReady: String? = null,
    val pushDefaultTitle: String? = null,
    val pushDefaultBody: String? = null,
)

val PrivChatCommonStringsPatch.isEmpty: Boolean
    get() = appName == null &&
        confirm == null &&
        cancel == null &&
        save == null &&
        edit == null &&
        delete == null &&
        search == null &&
        loading == null &&
        retry == null &&
        noData == null &&
        networkError == null &&
        connectionNotReady == null &&
        pushDefaultTitle == null &&
        pushDefaultBody == null

fun PrivChatCommonStrings.merge(patch: PrivChatCommonStringsPatch?): PrivChatCommonStrings {
    if (patch == null || patch.isEmpty) return this
    return copy(
        appName = patch.appName ?: appName,
        confirm = patch.confirm ?: confirm,
        cancel = patch.cancel ?: cancel,
        save = patch.save ?: save,
        edit = patch.edit ?: edit,
        delete = patch.delete ?: delete,
        search = patch.search ?: search,
        loading = patch.loading ?: loading,
        retry = patch.retry ?: retry,
        noData = patch.noData ?: noData,
        networkError = patch.networkError ?: networkError,
        connectionNotReady = patch.connectionNotReady ?: connectionNotReady,
        pushDefaultTitle = patch.pushDefaultTitle ?: pushDefaultTitle,
        pushDefaultBody = patch.pushDefaultBody ?: pushDefaultBody,
    )
}

@Immutable
data class PrivChatConversationStrings(
    val tabConversation: String,
    val tabContact: String,
    val tabMe: String,
    val conversationTitle: String,
    val conversationEmpty: String,
    val conversationMuted: String,
    val conversationPinned: String,
    val conversationPin: String,
    val conversationUnpin: String,
    val conversationHide: String,
    val conversationDelete: String,
    val conversationDraft: String,
    val conversationAtMe: String,
    val conversationAtAll: String,
    val conversationUnread: String,
)

data class PrivChatConversationStringsPatch(
    val tabConversation: String? = null,
    val tabContact: String? = null,
    val tabMe: String? = null,
    val conversationTitle: String? = null,
    val conversationEmpty: String? = null,
    val conversationMuted: String? = null,
    val conversationPinned: String? = null,
    val conversationPin: String? = null,
    val conversationUnpin: String? = null,
    val conversationHide: String? = null,
    val conversationDelete: String? = null,
    val conversationDraft: String? = null,
    val conversationAtMe: String? = null,
    val conversationAtAll: String? = null,
    val conversationUnread: String? = null,
)

val PrivChatConversationStringsPatch.isEmpty: Boolean
    get() = tabConversation == null &&
        tabContact == null &&
        tabMe == null &&
        conversationTitle == null &&
        conversationEmpty == null &&
        conversationMuted == null &&
        conversationPinned == null &&
        conversationPin == null &&
        conversationUnpin == null &&
        conversationHide == null &&
        conversationDelete == null &&
        conversationDraft == null &&
        conversationAtMe == null &&
        conversationAtAll == null &&
        conversationUnread == null

fun PrivChatConversationStrings.merge(patch: PrivChatConversationStringsPatch?): PrivChatConversationStrings {
    if (patch == null || patch.isEmpty) return this
    return copy(
        tabConversation = patch.tabConversation ?: tabConversation,
        tabContact = patch.tabContact ?: tabContact,
        tabMe = patch.tabMe ?: tabMe,
        conversationTitle = patch.conversationTitle ?: conversationTitle,
        conversationEmpty = patch.conversationEmpty ?: conversationEmpty,
        conversationMuted = patch.conversationMuted ?: conversationMuted,
        conversationPinned = patch.conversationPinned ?: conversationPinned,
        conversationPin = patch.conversationPin ?: conversationPin,
        conversationUnpin = patch.conversationUnpin ?: conversationUnpin,
        conversationHide = patch.conversationHide ?: conversationHide,
        conversationDelete = patch.conversationDelete ?: conversationDelete,
        conversationDraft = patch.conversationDraft ?: conversationDraft,
        conversationAtMe = patch.conversationAtMe ?: conversationAtMe,
        conversationAtAll = patch.conversationAtAll ?: conversationAtAll,
        conversationUnread = patch.conversationUnread ?: conversationUnread,
    )
}

@Immutable
data class PrivChatMessageStrings(
    val messageImage: String,
    val messageVideo: String,
    val messageVoice: String,
    val messageFile: String,
    val messageLocation: String,
    val messageSticker: String,
    val messageRevoked: String,
    val messageSystem: String,
    val messageUnknown: String,
    val messageSending: String,
    val messageSendFailed: String,
    val messageAttachmentSourceMissing: String,
    val messageAttachmentTooLarge: String,
    val messageInputHint: String,
    val messageVoiceHint: String,
    val presenceOnline: String,
    val presenceOffline: String,
    val presenceLastSeenPrefix: String,
    val forwardTitle: String,
    val forwardSend: String,
    val forwardSearchPlaceholder: String,
    val forwardEmpty: String,
    val forwardNoMatch: String,
    val forwardSectionRecent: String,
    val forwardSectionFriends: String,
    val forwardSectionGroups: String,
    val forwardCommentPlaceholder: String,
    /** 占位符按序替换：已选数量 / 上限。 */
    val forwardSelectedCount: String,
    val forwardMaxReached: String,
    val chatEmpty: String,
    /** 引用/回复摘要里指代自己的称呼。 */
    val messageSenderSelf: String,
    val messageSend: String,
    val messageCopied: String,
    val messageSaving: String,
    val messageSavedToAlbum: String,
    val messageSaveFailedNotDownloaded: String,
    val messageOriginalNotSentYet: String,
    val messageProcessing: String,
    /** 未读分隔线，占位符=未读条数。 */
    val unreadDividerLabel: String,
    val unreadJumpLabel: String,
    val plusAlbum: String,
    val plusCamera: String,
    val plusLocation: String,
    val plusRedPacket: String,
    val plusMoneyTransfer: String,
    val plusFile: String,
    val plusContact: String,
    val voiceReleaseToCancel: String,
    val voiceReleaseToSend: String,
    val voiceHoldToTalk: String,
    val actionReply: String,
    val actionCopyText: String,
    val actionSaveImage: String,
    val actionRecall: String,
    val actionForward: String,
    val actionCancelSend: String,
    val actionDeleteLocal: String,
    val actionSelect: String,
    val actionReport: String,
    val resendTitle: String,
    val resendConfirm: String,
    /** 输入栏引用条的第一行，占位符=被回复的人。 */
    val replyToPrefix: String,
    val replyOriginalUnavailable: String,
    val featureLocationComingSoon: String,
    val featureContactComingSoon: String,
    val featureForwardComingSoon: String,
    val featureReplyComingSoon: String,
    val featureSelectComingSoon: String,
    val featureReportUnavailable: String,
    val menuLoading: String,
    val menuLoadFailed: String,
    val menuEmpty: String,
    val menuInvokeFailed: String,
    val menuActionDone: String,
    val menuPreparing: String,
    val menuSignFailed: String,
    val menuPrepareLinkFailed: String,
    /** 菜单/转账路由不合法，占位符=原始 route。 */
    val menuInvalidRoute: String,
    /** 占位符=消息 id。 */
    val menuEmptyContent: String,
    val linkHttpsOnly: String,
    val linkOpenFailed: String,
    val linkOpen: String,
    val linkCopy: String,
    val phoneDial: String,
    val phoneSms: String,
    val phoneCopy: String,
    val phoneDialFailed: String,
    val phoneSmsFailed: String,
    val emailSend: String,
    val emailCopy: String,
    val emailOpenFailed: String,
    val a11yImage: String,
    val a11yVideo: String,
    val a11yFile: String,
    val a11ySticker: String,
    val downloadPaused: String,
    /** 占位符=已完成百分比。 */
    val downloadPausedAt: String,
    val downloadFailed: String,
    val locationFallbackTitle: String,
    /** 名片没有昵称时的兜底，占位符=uid。 */
    val contactCardUnnamed: String,
    val contactCardFallback: String,
    val contactCardLabel: String,
    val unsupportedContent: String,
    val redPacketDefaultTitle: String,
    val redPacketLucky: String,
    val redPacketNormal: String,
    val redPacketDrained: String,
    val redPacketClaimed: String,
    val redPacketExpired: String,
    val redPacketClaim: String,
    val redPacketUnsupportedVersion: String,
    val transferTitle: String,
    /** 占位符=收款人。 */
    val transferToPeer: String,
    /** 占位符=付款人。 */
    val transferFromPeer: String,
    val transferRefunded: String,
    val transferReceived: String,
    val transferCredited: String,
    val statusSendFailedRetry: String,
    /** 占位符=进度百分比。 */
    val statusUploading: String,
    val statusSendingShort: String,
    val statusRead: String,
    val statusDelivered: String,
    val statusSent: String,
    /** 长按菜单里的已读入口，占位符=已读人数。 */
    val readByMenuEntry: String,
    /** 已读名单弹层标题。 */
    val readBySheetTitle: String,
    /** 名单副标题：占位符依次是已读人数、发送时的收件人数。 */
    val readBySheetSubtitle: String,
    /** 明细窗口过期后的说明，占位符=保留天数。 */
    val readByExpired: String,
    /** 资料没取到时的占位名，不显示 uid。 */
    val readByUnknownUser: String,
)

data class PrivChatMessageStringsPatch(
    val messageImage: String? = null,
    val messageVideo: String? = null,
    val messageVoice: String? = null,
    val messageFile: String? = null,
    val messageLocation: String? = null,
    val messageSticker: String? = null,
    val messageRevoked: String? = null,
    val messageSystem: String? = null,
    val messageUnknown: String? = null,
    val messageSending: String? = null,
    val messageSendFailed: String? = null,
    val messageAttachmentSourceMissing: String? = null,
    val messageAttachmentTooLarge: String? = null,
    val messageInputHint: String? = null,
    val messageVoiceHint: String? = null,
    val presenceOnline: String? = null,
    val presenceOffline: String? = null,
    val presenceLastSeenPrefix: String? = null,
    val forwardTitle: String? = null,
    val forwardSend: String? = null,
    val forwardSearchPlaceholder: String? = null,
    val forwardEmpty: String? = null,
    val forwardNoMatch: String? = null,
    val forwardSectionRecent: String? = null,
    val forwardSectionFriends: String? = null,
    val forwardSectionGroups: String? = null,
    val forwardCommentPlaceholder: String? = null,
    val forwardSelectedCount: String? = null,
    val forwardMaxReached: String? = null,
    val chatEmpty: String? = null,
    val messageSenderSelf: String? = null,
    val messageSend: String? = null,
    val messageCopied: String? = null,
    val messageSaving: String? = null,
    val messageSavedToAlbum: String? = null,
    val messageSaveFailedNotDownloaded: String? = null,
    val messageOriginalNotSentYet: String? = null,
    val messageProcessing: String? = null,
    val unreadDividerLabel: String? = null,
    val unreadJumpLabel: String? = null,
    val plusAlbum: String? = null,
    val plusCamera: String? = null,
    val plusLocation: String? = null,
    val plusRedPacket: String? = null,
    val plusMoneyTransfer: String? = null,
    val plusFile: String? = null,
    val plusContact: String? = null,
    val voiceReleaseToCancel: String? = null,
    val voiceReleaseToSend: String? = null,
    val voiceHoldToTalk: String? = null,
    val actionReply: String? = null,
    val actionCopyText: String? = null,
    val actionSaveImage: String? = null,
    val actionRecall: String? = null,
    val actionForward: String? = null,
    val actionCancelSend: String? = null,
    val actionDeleteLocal: String? = null,
    val actionSelect: String? = null,
    val actionReport: String? = null,
    val resendTitle: String? = null,
    val resendConfirm: String? = null,
    val replyToPrefix: String? = null,
    val replyOriginalUnavailable: String? = null,
    val featureLocationComingSoon: String? = null,
    val featureContactComingSoon: String? = null,
    val featureForwardComingSoon: String? = null,
    val featureReplyComingSoon: String? = null,
    val featureSelectComingSoon: String? = null,
    val featureReportUnavailable: String? = null,
    val menuLoading: String? = null,
    val menuLoadFailed: String? = null,
    val menuEmpty: String? = null,
    val menuInvokeFailed: String? = null,
    val menuActionDone: String? = null,
    val menuPreparing: String? = null,
    val menuSignFailed: String? = null,
    val menuPrepareLinkFailed: String? = null,
    val menuInvalidRoute: String? = null,
    val menuEmptyContent: String? = null,
    val linkHttpsOnly: String? = null,
    val linkOpenFailed: String? = null,
    val linkOpen: String? = null,
    val linkCopy: String? = null,
    val phoneDial: String? = null,
    val phoneSms: String? = null,
    val phoneCopy: String? = null,
    val phoneDialFailed: String? = null,
    val phoneSmsFailed: String? = null,
    val emailSend: String? = null,
    val emailCopy: String? = null,
    val emailOpenFailed: String? = null,
    val a11yImage: String? = null,
    val a11yVideo: String? = null,
    val a11yFile: String? = null,
    val a11ySticker: String? = null,
    val downloadPaused: String? = null,
    val downloadPausedAt: String? = null,
    val downloadFailed: String? = null,
    val locationFallbackTitle: String? = null,
    val contactCardUnnamed: String? = null,
    val contactCardFallback: String? = null,
    val contactCardLabel: String? = null,
    val unsupportedContent: String? = null,
    val redPacketDefaultTitle: String? = null,
    val redPacketLucky: String? = null,
    val redPacketNormal: String? = null,
    val redPacketDrained: String? = null,
    val redPacketClaimed: String? = null,
    val redPacketExpired: String? = null,
    val redPacketClaim: String? = null,
    val redPacketUnsupportedVersion: String? = null,
    val transferTitle: String? = null,
    val transferToPeer: String? = null,
    val transferFromPeer: String? = null,
    val transferRefunded: String? = null,
    val transferReceived: String? = null,
    val transferCredited: String? = null,
    val statusSendFailedRetry: String? = null,
    val statusUploading: String? = null,
    val statusSendingShort: String? = null,
    val statusRead: String? = null,
    val statusDelivered: String? = null,
    val statusSent: String? = null,
    val readByMenuEntry: String? = null,
    val readBySheetTitle: String? = null,
    val readBySheetSubtitle: String? = null,
    val readByExpired: String? = null,
    val readByUnknownUser: String? = null,
)

val PrivChatMessageStringsPatch.isEmpty: Boolean
    get() = messageImage == null &&
        messageVideo == null &&
        messageVoice == null &&
        messageFile == null &&
        messageLocation == null &&
        messageSticker == null &&
        messageRevoked == null &&
        messageSystem == null &&
        messageUnknown == null &&
        messageSending == null &&
        messageSendFailed == null &&
        messageAttachmentSourceMissing == null &&
        messageAttachmentTooLarge == null &&
        messageInputHint == null &&
        messageVoiceHint == null &&
        presenceOnline == null &&
        presenceOffline == null &&
        presenceLastSeenPrefix == null &&
        forwardTitle == null &&
        forwardSend == null &&
        forwardSearchPlaceholder == null &&
        forwardEmpty == null &&
        forwardNoMatch == null &&
        forwardSectionRecent == null &&
        forwardSectionFriends == null &&
        forwardSectionGroups == null &&
        forwardCommentPlaceholder == null &&
        forwardSelectedCount == null &&
        forwardMaxReached == null &&
        chatEmpty == null &&
        messageSenderSelf == null &&
        messageSend == null &&
        messageCopied == null &&
        messageSaving == null &&
        messageSavedToAlbum == null &&
        messageSaveFailedNotDownloaded == null &&
        messageOriginalNotSentYet == null &&
        messageProcessing == null &&
        unreadDividerLabel == null &&
        unreadJumpLabel == null &&
        plusAlbum == null &&
        plusCamera == null &&
        plusLocation == null &&
        plusRedPacket == null &&
        plusMoneyTransfer == null &&
        plusFile == null &&
        plusContact == null &&
        voiceReleaseToCancel == null &&
        voiceReleaseToSend == null &&
        voiceHoldToTalk == null &&
        actionReply == null &&
        actionCopyText == null &&
        actionSaveImage == null &&
        actionRecall == null &&
        actionForward == null &&
        actionCancelSend == null &&
        actionDeleteLocal == null &&
        actionSelect == null &&
        actionReport == null &&
        resendTitle == null &&
        resendConfirm == null &&
        replyToPrefix == null &&
        replyOriginalUnavailable == null &&
        featureLocationComingSoon == null &&
        featureContactComingSoon == null &&
        featureForwardComingSoon == null &&
        featureReplyComingSoon == null &&
        featureSelectComingSoon == null &&
        featureReportUnavailable == null &&
        menuLoading == null &&
        menuLoadFailed == null &&
        menuEmpty == null &&
        menuInvokeFailed == null &&
        menuActionDone == null &&
        menuPreparing == null &&
        menuSignFailed == null &&
        menuPrepareLinkFailed == null &&
        menuInvalidRoute == null &&
        menuEmptyContent == null &&
        linkHttpsOnly == null &&
        linkOpenFailed == null &&
        linkOpen == null &&
        linkCopy == null &&
        phoneDial == null &&
        phoneSms == null &&
        phoneCopy == null &&
        phoneDialFailed == null &&
        phoneSmsFailed == null &&
        emailSend == null &&
        emailCopy == null &&
        emailOpenFailed == null &&
        a11yImage == null &&
        a11yVideo == null &&
        a11yFile == null &&
        a11ySticker == null &&
        downloadPaused == null &&
        downloadPausedAt == null &&
        downloadFailed == null &&
        locationFallbackTitle == null &&
        contactCardUnnamed == null &&
        contactCardFallback == null &&
        contactCardLabel == null &&
        unsupportedContent == null &&
        redPacketDefaultTitle == null &&
        redPacketLucky == null &&
        redPacketNormal == null &&
        redPacketDrained == null &&
        redPacketClaimed == null &&
        redPacketExpired == null &&
        redPacketClaim == null &&
        redPacketUnsupportedVersion == null &&
        transferTitle == null &&
        transferToPeer == null &&
        transferFromPeer == null &&
        transferRefunded == null &&
        transferReceived == null &&
        transferCredited == null &&
        statusSendFailedRetry == null &&
        statusUploading == null &&
        statusSendingShort == null &&
        statusRead == null &&
        statusDelivered == null &&
        statusSent == null &&
        readByMenuEntry == null &&
        readBySheetTitle == null &&
        readBySheetSubtitle == null &&
        readByExpired == null &&
        readByUnknownUser == null

fun PrivChatMessageStrings.merge(patch: PrivChatMessageStringsPatch?): PrivChatMessageStrings {
    if (patch == null || patch.isEmpty) return this
    return copy(
        messageImage = patch.messageImage ?: messageImage,
        messageVideo = patch.messageVideo ?: messageVideo,
        messageVoice = patch.messageVoice ?: messageVoice,
        messageFile = patch.messageFile ?: messageFile,
        messageLocation = patch.messageLocation ?: messageLocation,
        messageSticker = patch.messageSticker ?: messageSticker,
        messageRevoked = patch.messageRevoked ?: messageRevoked,
        messageSystem = patch.messageSystem ?: messageSystem,
        messageUnknown = patch.messageUnknown ?: messageUnknown,
        messageSending = patch.messageSending ?: messageSending,
        messageSendFailed = patch.messageSendFailed ?: messageSendFailed,
        messageAttachmentSourceMissing = patch.messageAttachmentSourceMissing ?: messageAttachmentSourceMissing,
        messageAttachmentTooLarge = patch.messageAttachmentTooLarge ?: messageAttachmentTooLarge,
        messageInputHint = patch.messageInputHint ?: messageInputHint,
        messageVoiceHint = patch.messageVoiceHint ?: messageVoiceHint,
        presenceOnline = patch.presenceOnline ?: presenceOnline,
        presenceOffline = patch.presenceOffline ?: presenceOffline,
        presenceLastSeenPrefix = patch.presenceLastSeenPrefix ?: presenceLastSeenPrefix,
        forwardTitle = patch.forwardTitle ?: forwardTitle,
        forwardSend = patch.forwardSend ?: forwardSend,
        forwardSearchPlaceholder = patch.forwardSearchPlaceholder ?: forwardSearchPlaceholder,
        forwardEmpty = patch.forwardEmpty ?: forwardEmpty,
        forwardNoMatch = patch.forwardNoMatch ?: forwardNoMatch,
        forwardSectionRecent = patch.forwardSectionRecent ?: forwardSectionRecent,
        forwardSectionFriends = patch.forwardSectionFriends ?: forwardSectionFriends,
        forwardSectionGroups = patch.forwardSectionGroups ?: forwardSectionGroups,
        forwardCommentPlaceholder = patch.forwardCommentPlaceholder ?: forwardCommentPlaceholder,
        forwardSelectedCount = patch.forwardSelectedCount ?: forwardSelectedCount,
        forwardMaxReached = patch.forwardMaxReached ?: forwardMaxReached,
        chatEmpty = patch.chatEmpty ?: chatEmpty,
        messageSenderSelf = patch.messageSenderSelf ?: messageSenderSelf,
        messageSend = patch.messageSend ?: messageSend,
        messageCopied = patch.messageCopied ?: messageCopied,
        messageSaving = patch.messageSaving ?: messageSaving,
        messageSavedToAlbum = patch.messageSavedToAlbum ?: messageSavedToAlbum,
        messageSaveFailedNotDownloaded = patch.messageSaveFailedNotDownloaded ?: messageSaveFailedNotDownloaded,
        messageOriginalNotSentYet = patch.messageOriginalNotSentYet ?: messageOriginalNotSentYet,
        messageProcessing = patch.messageProcessing ?: messageProcessing,
        unreadDividerLabel = patch.unreadDividerLabel ?: unreadDividerLabel,
        unreadJumpLabel = patch.unreadJumpLabel ?: unreadJumpLabel,
        plusAlbum = patch.plusAlbum ?: plusAlbum,
        plusCamera = patch.plusCamera ?: plusCamera,
        plusLocation = patch.plusLocation ?: plusLocation,
        plusRedPacket = patch.plusRedPacket ?: plusRedPacket,
        plusMoneyTransfer = patch.plusMoneyTransfer ?: plusMoneyTransfer,
        plusFile = patch.plusFile ?: plusFile,
        plusContact = patch.plusContact ?: plusContact,
        voiceReleaseToCancel = patch.voiceReleaseToCancel ?: voiceReleaseToCancel,
        voiceReleaseToSend = patch.voiceReleaseToSend ?: voiceReleaseToSend,
        voiceHoldToTalk = patch.voiceHoldToTalk ?: voiceHoldToTalk,
        actionReply = patch.actionReply ?: actionReply,
        actionCopyText = patch.actionCopyText ?: actionCopyText,
        actionSaveImage = patch.actionSaveImage ?: actionSaveImage,
        actionRecall = patch.actionRecall ?: actionRecall,
        actionForward = patch.actionForward ?: actionForward,
        actionCancelSend = patch.actionCancelSend ?: actionCancelSend,
        actionDeleteLocal = patch.actionDeleteLocal ?: actionDeleteLocal,
        actionSelect = patch.actionSelect ?: actionSelect,
        actionReport = patch.actionReport ?: actionReport,
        resendTitle = patch.resendTitle ?: resendTitle,
        resendConfirm = patch.resendConfirm ?: resendConfirm,
        replyToPrefix = patch.replyToPrefix ?: replyToPrefix,
        replyOriginalUnavailable = patch.replyOriginalUnavailable ?: replyOriginalUnavailable,
        featureLocationComingSoon = patch.featureLocationComingSoon ?: featureLocationComingSoon,
        featureContactComingSoon = patch.featureContactComingSoon ?: featureContactComingSoon,
        featureForwardComingSoon = patch.featureForwardComingSoon ?: featureForwardComingSoon,
        featureReplyComingSoon = patch.featureReplyComingSoon ?: featureReplyComingSoon,
        featureSelectComingSoon = patch.featureSelectComingSoon ?: featureSelectComingSoon,
        featureReportUnavailable = patch.featureReportUnavailable ?: featureReportUnavailable,
        menuLoading = patch.menuLoading ?: menuLoading,
        menuLoadFailed = patch.menuLoadFailed ?: menuLoadFailed,
        menuEmpty = patch.menuEmpty ?: menuEmpty,
        menuInvokeFailed = patch.menuInvokeFailed ?: menuInvokeFailed,
        menuActionDone = patch.menuActionDone ?: menuActionDone,
        menuPreparing = patch.menuPreparing ?: menuPreparing,
        menuSignFailed = patch.menuSignFailed ?: menuSignFailed,
        menuPrepareLinkFailed = patch.menuPrepareLinkFailed ?: menuPrepareLinkFailed,
        menuInvalidRoute = patch.menuInvalidRoute ?: menuInvalidRoute,
        menuEmptyContent = patch.menuEmptyContent ?: menuEmptyContent,
        linkHttpsOnly = patch.linkHttpsOnly ?: linkHttpsOnly,
        linkOpenFailed = patch.linkOpenFailed ?: linkOpenFailed,
        linkOpen = patch.linkOpen ?: linkOpen,
        linkCopy = patch.linkCopy ?: linkCopy,
        phoneDial = patch.phoneDial ?: phoneDial,
        phoneSms = patch.phoneSms ?: phoneSms,
        phoneCopy = patch.phoneCopy ?: phoneCopy,
        phoneDialFailed = patch.phoneDialFailed ?: phoneDialFailed,
        phoneSmsFailed = patch.phoneSmsFailed ?: phoneSmsFailed,
        emailSend = patch.emailSend ?: emailSend,
        emailCopy = patch.emailCopy ?: emailCopy,
        emailOpenFailed = patch.emailOpenFailed ?: emailOpenFailed,
        a11yImage = patch.a11yImage ?: a11yImage,
        a11yVideo = patch.a11yVideo ?: a11yVideo,
        a11yFile = patch.a11yFile ?: a11yFile,
        a11ySticker = patch.a11ySticker ?: a11ySticker,
        downloadPaused = patch.downloadPaused ?: downloadPaused,
        downloadPausedAt = patch.downloadPausedAt ?: downloadPausedAt,
        downloadFailed = patch.downloadFailed ?: downloadFailed,
        locationFallbackTitle = patch.locationFallbackTitle ?: locationFallbackTitle,
        contactCardUnnamed = patch.contactCardUnnamed ?: contactCardUnnamed,
        contactCardFallback = patch.contactCardFallback ?: contactCardFallback,
        contactCardLabel = patch.contactCardLabel ?: contactCardLabel,
        unsupportedContent = patch.unsupportedContent ?: unsupportedContent,
        redPacketDefaultTitle = patch.redPacketDefaultTitle ?: redPacketDefaultTitle,
        redPacketLucky = patch.redPacketLucky ?: redPacketLucky,
        redPacketNormal = patch.redPacketNormal ?: redPacketNormal,
        redPacketDrained = patch.redPacketDrained ?: redPacketDrained,
        redPacketClaimed = patch.redPacketClaimed ?: redPacketClaimed,
        redPacketExpired = patch.redPacketExpired ?: redPacketExpired,
        redPacketClaim = patch.redPacketClaim ?: redPacketClaim,
        redPacketUnsupportedVersion = patch.redPacketUnsupportedVersion ?: redPacketUnsupportedVersion,
        transferTitle = patch.transferTitle ?: transferTitle,
        transferToPeer = patch.transferToPeer ?: transferToPeer,
        transferFromPeer = patch.transferFromPeer ?: transferFromPeer,
        transferRefunded = patch.transferRefunded ?: transferRefunded,
        transferReceived = patch.transferReceived ?: transferReceived,
        transferCredited = patch.transferCredited ?: transferCredited,
        statusSendFailedRetry = patch.statusSendFailedRetry ?: statusSendFailedRetry,
        statusUploading = patch.statusUploading ?: statusUploading,
        statusSendingShort = patch.statusSendingShort ?: statusSendingShort,
        statusRead = patch.statusRead ?: statusRead,
        statusDelivered = patch.statusDelivered ?: statusDelivered,
        statusSent = patch.statusSent ?: statusSent,
        readByMenuEntry = patch.readByMenuEntry ?: readByMenuEntry,
        readBySheetTitle = patch.readBySheetTitle ?: readBySheetTitle,
        readBySheetSubtitle = patch.readBySheetSubtitle ?: readBySheetSubtitle,
        readByExpired = patch.readByExpired ?: readByExpired,
        readByUnknownUser = patch.readByUnknownUser ?: readByUnknownUser,
    )
}

@Immutable
data class PrivChatContactStrings(
    val contactTitle: String,
    val contactFriends: String,
    val contactGroups: String,
    val contactEmpty: String,
    val contactNewFriend: String,
    val contactFriendRequest: String,
    val contactAddFriend: String,
    val contactDeleteFriend: String,
    val contactMyGroups: String,
    val contactGroupsEmpty: String,
    val contactSearchUser: String,
    val myQrCodeTitle: String,
    val menuCreateGroup: String,
    val menuAddFriend: String,
    val menuScan: String,
    val menuMyQrCode: String,
    val friendRequestTitle: String,
    val friendRequestEmpty: String,
    val friendRequestAccept: String,
    val friendRequestReject: String,
    val friendRequestMessage: String,
    val friendRequestTime: String,
    val friendRequestAccepted: String,
    val friendRequestRejected: String,
    val friendRequestView: String,
    val friendRequestAdded: String,
    val friendRequestTabReceived: String,
    val friendRequestTabSent: String,
    val friendRequestDecline: String,
    val friendRequestRecall: String,
    val friendRequestRecallSoon: String,
    val friendRequestSectionOlder: String,
    val friendRequestSourceUnknown: String,
    val friendRequestSentEmpty: String,
    val friendRequestSourceLabels: Map<String, String>,
    val friendRequestStatusLabels: Map<Int, String>,
)

data class PrivChatContactStringsPatch(
    val contactTitle: String? = null,
    val contactFriends: String? = null,
    val contactGroups: String? = null,
    val contactEmpty: String? = null,
    val contactNewFriend: String? = null,
    val contactFriendRequest: String? = null,
    val contactAddFriend: String? = null,
    val contactDeleteFriend: String? = null,
    val contactMyGroups: String? = null,
    val contactGroupsEmpty: String? = null,
    val contactSearchUser: String? = null,
    val myQrCodeTitle: String? = null,
    val menuCreateGroup: String? = null,
    val menuAddFriend: String? = null,
    val menuScan: String? = null,
    val menuMyQrCode: String? = null,
    val friendRequestTitle: String? = null,
    val friendRequestEmpty: String? = null,
    val friendRequestAccept: String? = null,
    val friendRequestReject: String? = null,
    val friendRequestMessage: String? = null,
    val friendRequestTime: String? = null,
    val friendRequestAccepted: String? = null,
    val friendRequestRejected: String? = null,
    val friendRequestView: String? = null,
    val friendRequestAdded: String? = null,
    val friendRequestTabReceived: String? = null,
    val friendRequestTabSent: String? = null,
    val friendRequestDecline: String? = null,
    val friendRequestRecall: String? = null,
    val friendRequestRecallSoon: String? = null,
    val friendRequestSectionOlder: String? = null,
    val friendRequestSourceUnknown: String? = null,
    val friendRequestSentEmpty: String? = null,
    val friendRequestSourceLabels: Map<String, String>? = null,
    val friendRequestStatusLabels: Map<Int, String>? = null,
)

val PrivChatContactStringsPatch.isEmpty: Boolean
    get() = contactTitle == null &&
        contactFriends == null &&
        contactGroups == null &&
        contactEmpty == null &&
        contactNewFriend == null &&
        contactFriendRequest == null &&
        contactAddFriend == null &&
        contactDeleteFriend == null &&
        contactMyGroups == null &&
        contactGroupsEmpty == null &&
        contactSearchUser == null &&
        myQrCodeTitle == null &&
        menuCreateGroup == null &&
        menuAddFriend == null &&
        menuScan == null &&
        menuMyQrCode == null &&
        friendRequestTitle == null &&
        friendRequestEmpty == null &&
        friendRequestAccept == null &&
        friendRequestReject == null &&
        friendRequestMessage == null &&
        friendRequestTime == null &&
        friendRequestAccepted == null &&
        friendRequestRejected == null &&
        friendRequestView == null &&
        friendRequestAdded == null &&
        friendRequestTabReceived == null &&
        friendRequestTabSent == null &&
        friendRequestDecline == null &&
        friendRequestRecall == null &&
        friendRequestRecallSoon == null &&
        friendRequestSectionOlder == null &&
        friendRequestSourceUnknown == null &&
        friendRequestSentEmpty == null &&
        friendRequestSourceLabels == null &&
        friendRequestStatusLabels == null

fun PrivChatContactStrings.merge(patch: PrivChatContactStringsPatch?): PrivChatContactStrings {
    if (patch == null || patch.isEmpty) return this
    return copy(
        contactTitle = patch.contactTitle ?: contactTitle,
        contactFriends = patch.contactFriends ?: contactFriends,
        contactGroups = patch.contactGroups ?: contactGroups,
        contactEmpty = patch.contactEmpty ?: contactEmpty,
        contactNewFriend = patch.contactNewFriend ?: contactNewFriend,
        contactFriendRequest = patch.contactFriendRequest ?: contactFriendRequest,
        contactAddFriend = patch.contactAddFriend ?: contactAddFriend,
        contactDeleteFriend = patch.contactDeleteFriend ?: contactDeleteFriend,
        contactMyGroups = patch.contactMyGroups ?: contactMyGroups,
        contactGroupsEmpty = patch.contactGroupsEmpty ?: contactGroupsEmpty,
        contactSearchUser = patch.contactSearchUser ?: contactSearchUser,
        myQrCodeTitle = patch.myQrCodeTitle ?: myQrCodeTitle,
        menuCreateGroup = patch.menuCreateGroup ?: menuCreateGroup,
        menuAddFriend = patch.menuAddFriend ?: menuAddFriend,
        menuScan = patch.menuScan ?: menuScan,
        menuMyQrCode = patch.menuMyQrCode ?: menuMyQrCode,
        friendRequestTitle = patch.friendRequestTitle ?: friendRequestTitle,
        friendRequestEmpty = patch.friendRequestEmpty ?: friendRequestEmpty,
        friendRequestAccept = patch.friendRequestAccept ?: friendRequestAccept,
        friendRequestReject = patch.friendRequestReject ?: friendRequestReject,
        friendRequestMessage = patch.friendRequestMessage ?: friendRequestMessage,
        friendRequestTime = patch.friendRequestTime ?: friendRequestTime,
        friendRequestAccepted = patch.friendRequestAccepted ?: friendRequestAccepted,
        friendRequestRejected = patch.friendRequestRejected ?: friendRequestRejected,
        friendRequestView = patch.friendRequestView ?: friendRequestView,
        friendRequestAdded = patch.friendRequestAdded ?: friendRequestAdded,
        friendRequestTabReceived = patch.friendRequestTabReceived ?: friendRequestTabReceived,
        friendRequestTabSent = patch.friendRequestTabSent ?: friendRequestTabSent,
        friendRequestDecline = patch.friendRequestDecline ?: friendRequestDecline,
        friendRequestRecall = patch.friendRequestRecall ?: friendRequestRecall,
        friendRequestRecallSoon = patch.friendRequestRecallSoon ?: friendRequestRecallSoon,
        friendRequestSectionOlder = patch.friendRequestSectionOlder ?: friendRequestSectionOlder,
        friendRequestSourceUnknown = patch.friendRequestSourceUnknown ?: friendRequestSourceUnknown,
        friendRequestSentEmpty = patch.friendRequestSentEmpty ?: friendRequestSentEmpty,
        friendRequestSourceLabels = patch.friendRequestSourceLabels ?: friendRequestSourceLabels,
        friendRequestStatusLabels = patch.friendRequestStatusLabels ?: friendRequestStatusLabels,
    )
}

@Immutable
data class PrivChatSearchStrings(
    val searchUserTitle: String,
    val searchUserPlaceholder: String,
    val searchUserSearching: String,
    val searchUserNoResult: String,
    val searchUserTryAgain: String,
    val searchUserHint: String,
    val searchUserError: String,
    val searchUserScan: String,
    val searchUserPhoneContacts: String,
    val searchUserMyQrCode: String,
    val searchUserMyAccount: String,
    val globalSearchTitle: String,
    val globalSearchPlaceholder: String,
    val globalSearchSectionChannels: String,
    val globalSearchSectionContacts: String,
    val globalSearchRelatedPrefix: String,
    val globalSearchRelatedSuffix: String,
    val globalSearchSectionGroups: String,
    val globalSearchMoreContacts: String,
    val globalSearchMoreGroups: String,
    val globalSearchMoreMessages: String,
    val globalSearchSectionMessages: String,
    val globalSearchNoResult: String,
    val globalSearchLoadMore: String,
    val globalSearchAnchorMissing: String,
)

data class PrivChatSearchStringsPatch(
    val searchUserTitle: String? = null,
    val searchUserPlaceholder: String? = null,
    val searchUserSearching: String? = null,
    val searchUserNoResult: String? = null,
    val searchUserTryAgain: String? = null,
    val searchUserHint: String? = null,
    val searchUserError: String? = null,
    val searchUserScan: String? = null,
    val searchUserPhoneContacts: String? = null,
    val searchUserMyQrCode: String? = null,
    val searchUserMyAccount: String? = null,
    val globalSearchTitle: String? = null,
    val globalSearchPlaceholder: String? = null,
    val globalSearchSectionChannels: String? = null,
    val globalSearchSectionContacts: String? = null,
    val globalSearchRelatedPrefix: String? = null,
    val globalSearchRelatedSuffix: String? = null,
    val globalSearchSectionGroups: String? = null,
    val globalSearchMoreContacts: String? = null,
    val globalSearchMoreGroups: String? = null,
    val globalSearchMoreMessages: String? = null,
    val globalSearchSectionMessages: String? = null,
    val globalSearchNoResult: String? = null,
    val globalSearchLoadMore: String? = null,
    val globalSearchAnchorMissing: String? = null,
)

val PrivChatSearchStringsPatch.isEmpty: Boolean
    get() = searchUserTitle == null &&
        searchUserPlaceholder == null &&
        searchUserSearching == null &&
        searchUserNoResult == null &&
        searchUserTryAgain == null &&
        searchUserHint == null &&
        searchUserError == null &&
        searchUserScan == null &&
        searchUserPhoneContacts == null &&
        searchUserMyQrCode == null &&
        searchUserMyAccount == null &&
        globalSearchTitle == null &&
        globalSearchPlaceholder == null &&
        globalSearchSectionChannels == null &&
        globalSearchSectionContacts == null &&
        globalSearchRelatedPrefix == null &&
        globalSearchRelatedSuffix == null &&
        globalSearchSectionGroups == null &&
        globalSearchMoreContacts == null &&
        globalSearchMoreGroups == null &&
        globalSearchMoreMessages == null &&
        globalSearchSectionMessages == null &&
        globalSearchNoResult == null &&
        globalSearchLoadMore == null &&
        globalSearchAnchorMissing == null

fun PrivChatSearchStrings.merge(patch: PrivChatSearchStringsPatch?): PrivChatSearchStrings {
    if (patch == null || patch.isEmpty) return this
    return copy(
        searchUserTitle = patch.searchUserTitle ?: searchUserTitle,
        searchUserPlaceholder = patch.searchUserPlaceholder ?: searchUserPlaceholder,
        searchUserSearching = patch.searchUserSearching ?: searchUserSearching,
        searchUserNoResult = patch.searchUserNoResult ?: searchUserNoResult,
        searchUserTryAgain = patch.searchUserTryAgain ?: searchUserTryAgain,
        searchUserHint = patch.searchUserHint ?: searchUserHint,
        searchUserError = patch.searchUserError ?: searchUserError,
        searchUserScan = patch.searchUserScan ?: searchUserScan,
        searchUserPhoneContacts = patch.searchUserPhoneContacts ?: searchUserPhoneContacts,
        searchUserMyQrCode = patch.searchUserMyQrCode ?: searchUserMyQrCode,
        searchUserMyAccount = patch.searchUserMyAccount ?: searchUserMyAccount,
        globalSearchTitle = patch.globalSearchTitle ?: globalSearchTitle,
        globalSearchPlaceholder = patch.globalSearchPlaceholder ?: globalSearchPlaceholder,
        globalSearchSectionChannels = patch.globalSearchSectionChannels ?: globalSearchSectionChannels,
        globalSearchSectionContacts = patch.globalSearchSectionContacts ?: globalSearchSectionContacts,
        globalSearchRelatedPrefix = patch.globalSearchRelatedPrefix ?: globalSearchRelatedPrefix,
        globalSearchRelatedSuffix = patch.globalSearchRelatedSuffix ?: globalSearchRelatedSuffix,
        globalSearchSectionGroups = patch.globalSearchSectionGroups ?: globalSearchSectionGroups,
        globalSearchMoreContacts = patch.globalSearchMoreContacts ?: globalSearchMoreContacts,
        globalSearchMoreGroups = patch.globalSearchMoreGroups ?: globalSearchMoreGroups,
        globalSearchMoreMessages = patch.globalSearchMoreMessages ?: globalSearchMoreMessages,
        globalSearchSectionMessages = patch.globalSearchSectionMessages ?: globalSearchSectionMessages,
        globalSearchNoResult = patch.globalSearchNoResult ?: globalSearchNoResult,
        globalSearchLoadMore = patch.globalSearchLoadMore ?: globalSearchLoadMore,
        globalSearchAnchorMissing = patch.globalSearchAnchorMissing ?: globalSearchAnchorMissing,
    )
}

@Immutable
data class PrivChatUserStrings(
    val userProfileTitle: String,
    val userProfileUserId: String,
    val userProfileSystemAccount: String,
    val userBadgeSystem: String,
    val userBadgeBot: String,
    val userProfileBio: String,
    val userProfileRemark: String,
    val userProfileRemarkPlaceholder: String,
    val userProfileNickname: String,
    val userProfileSendMessage: String,
    val userProfileAddFriend: String,
    val userProfileFollowBot: String,
    val userProfileFollowingBot: String,
    val userProfileFollowedBotToast: String,
    val userProfileCannotAddSelf: String,
    val userProfileAcceptFriendRequest: String,
    val userProfileAdding: String,
    val userProfileRequestSent: String,
    val userProfileGroupAddFriendDisabled: String,
    val userProfileDeleteFriend: String,
    val userProfileDeleting: String,
    val userProfileBlockUser: String,
    val userProfileBlocking: String,
    val userProfileDeleteFriendConfirmTitle: String,
    val userProfileDeleteFriendConfirmMessage: String,
    val userProfileBlockUserConfirmTitle: String,
    val userProfileBlockUserConfirmMessage: String,
    val friendRequestInputTitle: String,
    val friendRequestInputPlaceholder: String,
    val friendRequestSentTitle: String,
    val genderUnknown: String,
    val genderMale: String,
    val genderFemale: String,
    val profileEditSave: String,
    val profileEditAvatarHint: String,
)

data class PrivChatUserStringsPatch(
    val userProfileTitle: String? = null,
    val userProfileUserId: String? = null,
    val userProfileSystemAccount: String? = null,
    val userBadgeSystem: String? = null,
    val userBadgeBot: String? = null,
    val userProfileBio: String? = null,
    val userProfileRemark: String? = null,
    val userProfileRemarkPlaceholder: String? = null,
    val userProfileNickname: String? = null,
    val userProfileSendMessage: String? = null,
    val userProfileAddFriend: String? = null,
    val userProfileFollowBot: String? = null,
    val userProfileFollowingBot: String? = null,
    val userProfileFollowedBotToast: String? = null,
    val userProfileCannotAddSelf: String? = null,
    val userProfileAcceptFriendRequest: String? = null,
    val userProfileAdding: String? = null,
    val userProfileRequestSent: String? = null,
    val userProfileGroupAddFriendDisabled: String? = null,
    val userProfileDeleteFriend: String? = null,
    val userProfileDeleting: String? = null,
    val userProfileBlockUser: String? = null,
    val userProfileBlocking: String? = null,
    val userProfileDeleteFriendConfirmTitle: String? = null,
    val userProfileDeleteFriendConfirmMessage: String? = null,
    val userProfileBlockUserConfirmTitle: String? = null,
    val userProfileBlockUserConfirmMessage: String? = null,
    val friendRequestInputTitle: String? = null,
    val friendRequestInputPlaceholder: String? = null,
    val friendRequestSentTitle: String? = null,
    val genderUnknown: String? = null,
    val genderMale: String? = null,
    val genderFemale: String? = null,
    val profileEditSave: String? = null,
    val profileEditAvatarHint: String? = null,
)

val PrivChatUserStringsPatch.isEmpty: Boolean
    get() = userProfileTitle == null &&
        userProfileUserId == null &&
        userProfileSystemAccount == null &&
        userBadgeSystem == null &&
        userBadgeBot == null &&
        userProfileBio == null &&
        userProfileRemark == null &&
        userProfileRemarkPlaceholder == null &&
        userProfileNickname == null &&
        userProfileSendMessage == null &&
        userProfileAddFriend == null &&
        userProfileFollowBot == null &&
        userProfileFollowingBot == null &&
        userProfileFollowedBotToast == null &&
        userProfileCannotAddSelf == null &&
        userProfileAcceptFriendRequest == null &&
        userProfileAdding == null &&
        userProfileRequestSent == null &&
        userProfileGroupAddFriendDisabled == null &&
        userProfileDeleteFriend == null &&
        userProfileDeleting == null &&
        userProfileBlockUser == null &&
        userProfileBlocking == null &&
        userProfileDeleteFriendConfirmTitle == null &&
        userProfileDeleteFriendConfirmMessage == null &&
        userProfileBlockUserConfirmTitle == null &&
        userProfileBlockUserConfirmMessage == null &&
        friendRequestInputTitle == null &&
        friendRequestInputPlaceholder == null &&
        friendRequestSentTitle == null &&
        genderUnknown == null &&
        genderMale == null &&
        genderFemale == null &&
        profileEditSave == null &&
        profileEditAvatarHint == null

fun PrivChatUserStrings.merge(patch: PrivChatUserStringsPatch?): PrivChatUserStrings {
    if (patch == null || patch.isEmpty) return this
    return copy(
        userProfileTitle = patch.userProfileTitle ?: userProfileTitle,
        userProfileUserId = patch.userProfileUserId ?: userProfileUserId,
        userProfileSystemAccount = patch.userProfileSystemAccount ?: userProfileSystemAccount,
        userBadgeSystem = patch.userBadgeSystem ?: userBadgeSystem,
        userBadgeBot = patch.userBadgeBot ?: userBadgeBot,
        userProfileBio = patch.userProfileBio ?: userProfileBio,
        userProfileRemark = patch.userProfileRemark ?: userProfileRemark,
        userProfileRemarkPlaceholder = patch.userProfileRemarkPlaceholder ?: userProfileRemarkPlaceholder,
        userProfileNickname = patch.userProfileNickname ?: userProfileNickname,
        userProfileSendMessage = patch.userProfileSendMessage ?: userProfileSendMessage,
        userProfileAddFriend = patch.userProfileAddFriend ?: userProfileAddFriend,
        userProfileFollowBot = patch.userProfileFollowBot ?: userProfileFollowBot,
        userProfileFollowingBot = patch.userProfileFollowingBot ?: userProfileFollowingBot,
        userProfileFollowedBotToast = patch.userProfileFollowedBotToast ?: userProfileFollowedBotToast,
        userProfileCannotAddSelf = patch.userProfileCannotAddSelf ?: userProfileCannotAddSelf,
        userProfileAcceptFriendRequest = patch.userProfileAcceptFriendRequest ?: userProfileAcceptFriendRequest,
        userProfileAdding = patch.userProfileAdding ?: userProfileAdding,
        userProfileRequestSent = patch.userProfileRequestSent ?: userProfileRequestSent,
        userProfileGroupAddFriendDisabled = patch.userProfileGroupAddFriendDisabled ?: userProfileGroupAddFriendDisabled,
        userProfileDeleteFriend = patch.userProfileDeleteFriend ?: userProfileDeleteFriend,
        userProfileDeleting = patch.userProfileDeleting ?: userProfileDeleting,
        userProfileBlockUser = patch.userProfileBlockUser ?: userProfileBlockUser,
        userProfileBlocking = patch.userProfileBlocking ?: userProfileBlocking,
        userProfileDeleteFriendConfirmTitle = patch.userProfileDeleteFriendConfirmTitle ?: userProfileDeleteFriendConfirmTitle,
        userProfileDeleteFriendConfirmMessage = patch.userProfileDeleteFriendConfirmMessage ?: userProfileDeleteFriendConfirmMessage,
        userProfileBlockUserConfirmTitle = patch.userProfileBlockUserConfirmTitle ?: userProfileBlockUserConfirmTitle,
        userProfileBlockUserConfirmMessage = patch.userProfileBlockUserConfirmMessage ?: userProfileBlockUserConfirmMessage,
        friendRequestInputTitle = patch.friendRequestInputTitle ?: friendRequestInputTitle,
        friendRequestInputPlaceholder = patch.friendRequestInputPlaceholder ?: friendRequestInputPlaceholder,
        friendRequestSentTitle = patch.friendRequestSentTitle ?: friendRequestSentTitle,
        genderUnknown = patch.genderUnknown ?: genderUnknown,
        genderMale = patch.genderMale ?: genderMale,
        genderFemale = patch.genderFemale ?: genderFemale,
        profileEditSave = patch.profileEditSave ?: profileEditSave,
        profileEditAvatarHint = patch.profileEditAvatarHint ?: profileEditAvatarHint,
    )
}

@Immutable
data class PrivChatFriendStrings(
    val friendSettingsTitle: String,
    val addFriendGroupPolicyDenied: String,
    val addFriendPersonalDenied: String,
    val addFriendAlreadyFriends: String,

    val friendSettingsShare: String,
    val friendSettingsSpecialFollow: String,
    val friendSettingsDelete: String,
    val permissionAllowAny: String,
    val permissionDenyAny: String,
    val permissionRequireRequest: String,
    val permissionNone: String,
    val friendSettingsReport: String,
)

data class PrivChatFriendStringsPatch(
    val friendSettingsTitle: String? = null,
    val addFriendGroupPolicyDenied: String? = null,
    val addFriendPersonalDenied: String? = null,
    val addFriendAlreadyFriends: String? = null,

    val friendSettingsShare: String? = null,
    val friendSettingsSpecialFollow: String? = null,
    val friendSettingsDelete: String? = null,
    val permissionAllowAny: String? = null,
    val permissionDenyAny: String? = null,
    val permissionRequireRequest: String? = null,
    val permissionNone: String? = null,
    val friendSettingsReport: String? = null,
)

val PrivChatFriendStringsPatch.isEmpty: Boolean
    get() = friendSettingsTitle == null &&
        addFriendGroupPolicyDenied == null &&
        addFriendPersonalDenied == null &&
        addFriendAlreadyFriends == null &&
        friendSettingsShare == null &&
        friendSettingsSpecialFollow == null &&
        friendSettingsDelete == null &&
        permissionAllowAny == null &&
        permissionDenyAny == null &&
        permissionRequireRequest == null &&
        permissionNone == null &&
        friendSettingsReport == null

fun PrivChatFriendStrings.merge(patch: PrivChatFriendStringsPatch?): PrivChatFriendStrings {
    if (patch == null || patch.isEmpty) return this
    return copy(
        friendSettingsTitle = patch.friendSettingsTitle ?: friendSettingsTitle,
        addFriendGroupPolicyDenied = patch.addFriendGroupPolicyDenied ?: addFriendGroupPolicyDenied,
        addFriendPersonalDenied = patch.addFriendPersonalDenied ?: addFriendPersonalDenied,
        addFriendAlreadyFriends = patch.addFriendAlreadyFriends ?: addFriendAlreadyFriends,
        friendSettingsShare = patch.friendSettingsShare ?: friendSettingsShare,
        friendSettingsSpecialFollow = patch.friendSettingsSpecialFollow ?: friendSettingsSpecialFollow,
        friendSettingsDelete = patch.friendSettingsDelete ?: friendSettingsDelete,
        permissionAllowAny = patch.permissionAllowAny ?: permissionAllowAny,
        permissionDenyAny = patch.permissionDenyAny ?: permissionDenyAny,
        permissionRequireRequest = patch.permissionRequireRequest ?: permissionRequireRequest,
        permissionNone = patch.permissionNone ?: permissionNone,
        friendSettingsReport = patch.friendSettingsReport ?: friendSettingsReport,
    )
}

@Immutable
data class PrivChatChatSettingsStrings(
    val chatSettingsTitle: String,
    val chatSettingsGroupName: String,
    val groupNameEditTitle: String,
    val chatSettingsGroupQrCode: String,
    val chatSettingsGroupMembers: String,
    val chatSettingsGroupManage: String,
    val chatSettingsMute: String,
    val chatSettingsPin: String,
    val chatSettingsLeaveGroup: String,
    val chatSettingsLeaveGroupConfirmTitle: String,
    val chatSettingsLeaveGroupConfirmMessage: String,
)

data class PrivChatChatSettingsStringsPatch(
    val chatSettingsTitle: String? = null,
    val chatSettingsGroupName: String? = null,
    val groupNameEditTitle: String? = null,
    val chatSettingsGroupQrCode: String? = null,
    val chatSettingsGroupMembers: String? = null,
    val chatSettingsGroupManage: String? = null,
    val chatSettingsMute: String? = null,
    val chatSettingsPin: String? = null,
    val chatSettingsLeaveGroup: String? = null,
    val chatSettingsLeaveGroupConfirmTitle: String? = null,
    val chatSettingsLeaveGroupConfirmMessage: String? = null,
)

val PrivChatChatSettingsStringsPatch.isEmpty: Boolean
    get() = chatSettingsTitle == null &&
        chatSettingsGroupName == null &&
        groupNameEditTitle == null &&
        chatSettingsGroupQrCode == null &&
        chatSettingsGroupMembers == null &&
        chatSettingsGroupManage == null &&
        chatSettingsMute == null &&
        chatSettingsPin == null &&
        chatSettingsLeaveGroup == null &&
        chatSettingsLeaveGroupConfirmTitle == null &&
        chatSettingsLeaveGroupConfirmMessage == null

fun PrivChatChatSettingsStrings.merge(patch: PrivChatChatSettingsStringsPatch?): PrivChatChatSettingsStrings {
    if (patch == null || patch.isEmpty) return this
    return copy(
        chatSettingsTitle = patch.chatSettingsTitle ?: chatSettingsTitle,
        chatSettingsGroupName = patch.chatSettingsGroupName ?: chatSettingsGroupName,
        groupNameEditTitle = patch.groupNameEditTitle ?: groupNameEditTitle,
        chatSettingsGroupQrCode = patch.chatSettingsGroupQrCode ?: chatSettingsGroupQrCode,
        chatSettingsGroupMembers = patch.chatSettingsGroupMembers ?: chatSettingsGroupMembers,
        chatSettingsGroupManage = patch.chatSettingsGroupManage ?: chatSettingsGroupManage,
        chatSettingsMute = patch.chatSettingsMute ?: chatSettingsMute,
        chatSettingsPin = patch.chatSettingsPin ?: chatSettingsPin,
        chatSettingsLeaveGroup = patch.chatSettingsLeaveGroup ?: chatSettingsLeaveGroup,
        chatSettingsLeaveGroupConfirmTitle = patch.chatSettingsLeaveGroupConfirmTitle ?: chatSettingsLeaveGroupConfirmTitle,
        chatSettingsLeaveGroupConfirmMessage = patch.chatSettingsLeaveGroupConfirmMessage ?: chatSettingsLeaveGroupConfirmMessage,
    )
}

@Immutable
data class PrivChatGroupStrings(
    val groupSettingsSectionTitle: String,
    val groupSettingsAllowSearch: String,
    val groupSettingsMemberCanInvite: String,
    val groupSettingsAllowMemberAddFriend: String,
    val groupSettingsAllMuted: String,
    val groupSettingsJoinPolicy: String,
    val groupSettingsJoinPolicyNone: String,
    val groupSettingsJoinPolicyApproval: String,
    val groupApprovalTitle: String,
    val groupApprovalEmpty: String,
    val groupSettingsJoinPolicyOpen: String,
    val groupSettingsUpdateFailed: String,
    val groupMemberMute: String,
    val groupMemberRemove: String,
    val groupMemberUnmute: String,
    val groupMuteDurationTitle: String,
    val groupRoleSetAdmin: String,
    val groupRoleRemoveAdmin: String,
    val groupTransferOwner: String,
    val groupTransferOwnerConfirm: String,
    val groupMuteDuration10m: String,
    val groupMuteDuration1h: String,
    val groupMuteDuration1d: String,
    val groupMuteDurationForever: String,
    val groupMuteSuccess: String,
    val groupUnmuteSuccess: String,
    val messagePin: String,
    val messageUnpin: String,
    val pinnedMessagesTitle: String,
    val messagePinSuccess: String,
    val messageUnpinSuccess: String,
    val presenceOfflineMinutesAgo: String,
    val presenceOfflineHoursAgo: String,
    val presenceOfflineDaysAgo: String,
    val groupMembers: String,
    val groupOwner: String,
    val groupAdmin: String,
    // 聊天气泡昵称旁的短标签(「管理」;群主标签直接复用 groupOwner)。
    val groupAdminTag: String,
    val groupMember: String,
    val groupCreate: String,
    val groupLeave: String,
    val groupDissolve: String,
    val groupCreateTitle: String,
    val groupCreateAction: String,
    val groupCreateFailed: String,
    val groupCreateNamePlaceholder: String,
    val groupCreateNoFriends: String,
    val groupInviteTitle: String,
    val groupInviteAction: String,
    val groupInviteFailed: String,
    val groupInviteNoFriends: String,
    val groupInviteMembers: String,
    val groupPickerSearchPlaceholder: String,
    val groupPickerNoMatch: String,
    /** 占位符按序替换：已选数量 / 上限。 */
    val groupPickerSelectedCount: String,
    val groupPickerMaxReached: String,
    val groupOwnerCannotLeave: String,
    /** 未填群名时用成员昵称自动拼名字的分隔符。 */
    val groupCreateNameSeparator: String,
)

data class PrivChatGroupStringsPatch(
    val groupSettingsSectionTitle: String? = null,
    val groupSettingsAllowSearch: String? = null,
    val groupSettingsMemberCanInvite: String? = null,
    val groupSettingsAllowMemberAddFriend: String? = null,
    val groupSettingsAllMuted: String? = null,
    val groupSettingsJoinPolicy: String? = null,
    val groupSettingsJoinPolicyNone: String? = null,
    val groupSettingsJoinPolicyApproval: String? = null,
    val groupApprovalTitle: String? = null,
    val groupApprovalEmpty: String? = null,
    val groupSettingsJoinPolicyOpen: String? = null,
    val groupSettingsUpdateFailed: String? = null,
    val groupMemberMute: String? = null,
    val groupMemberRemove: String? = null,
    val groupMemberUnmute: String? = null,
    val groupMuteDurationTitle: String? = null,
    val groupRoleSetAdmin: String? = null,
    val groupRoleRemoveAdmin: String? = null,
    val groupTransferOwner: String? = null,
    val groupTransferOwnerConfirm: String? = null,
    val groupMuteDuration10m: String? = null,
    val groupMuteDuration1h: String? = null,
    val groupMuteDuration1d: String? = null,
    val groupMuteDurationForever: String? = null,
    val groupMuteSuccess: String? = null,
    val groupUnmuteSuccess: String? = null,
    val messagePin: String? = null,
    val messageUnpin: String? = null,
    val pinnedMessagesTitle: String? = null,
    val messagePinSuccess: String? = null,
    val messageUnpinSuccess: String? = null,
    val presenceOfflineMinutesAgo: String? = null,
    val presenceOfflineHoursAgo: String? = null,
    val presenceOfflineDaysAgo: String? = null,
    val groupMembers: String? = null,
    val groupOwner: String? = null,
    val groupAdmin: String? = null,
    val groupAdminTag: String? = null,
    val groupMember: String? = null,
    val groupCreate: String? = null,
    val groupLeave: String? = null,
    val groupDissolve: String? = null,
    val groupCreateTitle: String? = null,
    val groupCreateAction: String? = null,
    val groupCreateFailed: String? = null,
    val groupCreateNamePlaceholder: String? = null,
    val groupCreateNoFriends: String? = null,
    val groupInviteTitle: String? = null,
    val groupInviteAction: String? = null,
    val groupInviteFailed: String? = null,
    val groupInviteNoFriends: String? = null,
    val groupInviteMembers: String? = null,
    val groupPickerSearchPlaceholder: String? = null,
    val groupPickerNoMatch: String? = null,
    val groupPickerSelectedCount: String? = null,
    val groupPickerMaxReached: String? = null,
    val groupOwnerCannotLeave: String? = null,
    val groupCreateNameSeparator: String? = null,
)

val PrivChatGroupStringsPatch.isEmpty: Boolean
    get() = groupSettingsSectionTitle == null &&
        groupSettingsAllowSearch == null &&
        groupSettingsMemberCanInvite == null &&
        groupSettingsAllowMemberAddFriend == null &&
        groupSettingsAllMuted == null &&
        groupSettingsJoinPolicy == null &&
        groupSettingsJoinPolicyNone == null &&
        groupSettingsJoinPolicyApproval == null &&
        groupApprovalTitle == null &&
        groupApprovalEmpty == null &&
        groupSettingsJoinPolicyOpen == null &&
        groupSettingsUpdateFailed == null &&
        groupMemberMute == null &&
        groupMemberRemove == null &&
        groupMemberUnmute == null &&
        groupMuteDurationTitle == null &&
        groupRoleSetAdmin == null &&
        groupRoleRemoveAdmin == null &&
        groupTransferOwner == null &&
        groupTransferOwnerConfirm == null &&
        groupMuteDuration10m == null &&
        groupMuteDuration1h == null &&
        groupMuteDuration1d == null &&
        groupMuteDurationForever == null &&
        groupMuteSuccess == null &&
        groupUnmuteSuccess == null &&
        messagePin == null &&
        messageUnpin == null &&
        pinnedMessagesTitle == null &&
        messagePinSuccess == null &&
        messageUnpinSuccess == null &&
        presenceOfflineMinutesAgo == null &&
        presenceOfflineHoursAgo == null &&
        presenceOfflineDaysAgo == null &&
        groupMembers == null &&
        groupOwner == null &&
        groupAdmin == null &&
        groupAdminTag == null &&
        groupMember == null &&
        groupCreate == null &&
        groupLeave == null &&
        groupDissolve == null &&
        groupCreateTitle == null &&
        groupCreateAction == null &&
        groupCreateFailed == null &&
        groupCreateNamePlaceholder == null &&
        groupCreateNoFriends == null &&
        groupInviteTitle == null &&
        groupInviteAction == null &&
        groupInviteFailed == null &&
        groupInviteNoFriends == null &&
        groupInviteMembers == null &&
        groupPickerSearchPlaceholder == null &&
        groupPickerNoMatch == null &&
        groupPickerSelectedCount == null &&
        groupPickerMaxReached == null &&
        groupOwnerCannotLeave == null &&
        groupCreateNameSeparator == null

fun PrivChatGroupStrings.merge(patch: PrivChatGroupStringsPatch?): PrivChatGroupStrings {
    if (patch == null || patch.isEmpty) return this
    return copy(
        groupSettingsSectionTitle = patch.groupSettingsSectionTitle ?: groupSettingsSectionTitle,
        groupSettingsAllowSearch = patch.groupSettingsAllowSearch ?: groupSettingsAllowSearch,
        groupSettingsMemberCanInvite = patch.groupSettingsMemberCanInvite ?: groupSettingsMemberCanInvite,
        groupSettingsAllowMemberAddFriend = patch.groupSettingsAllowMemberAddFriend ?: groupSettingsAllowMemberAddFriend,
        groupSettingsAllMuted = patch.groupSettingsAllMuted ?: groupSettingsAllMuted,
        groupSettingsJoinPolicy = patch.groupSettingsJoinPolicy ?: groupSettingsJoinPolicy,
        groupSettingsJoinPolicyNone = patch.groupSettingsJoinPolicyNone ?: groupSettingsJoinPolicyNone,
        groupSettingsJoinPolicyApproval = patch.groupSettingsJoinPolicyApproval ?: groupSettingsJoinPolicyApproval,
        groupApprovalTitle = patch.groupApprovalTitle ?: groupApprovalTitle,
        groupApprovalEmpty = patch.groupApprovalEmpty ?: groupApprovalEmpty,
        groupSettingsJoinPolicyOpen = patch.groupSettingsJoinPolicyOpen ?: groupSettingsJoinPolicyOpen,
        groupSettingsUpdateFailed = patch.groupSettingsUpdateFailed ?: groupSettingsUpdateFailed,
        groupMemberMute = patch.groupMemberMute ?: groupMemberMute,
        groupMemberRemove = patch.groupMemberRemove ?: groupMemberRemove,
        groupMemberUnmute = patch.groupMemberUnmute ?: groupMemberUnmute,
        groupMuteDurationTitle = patch.groupMuteDurationTitle ?: groupMuteDurationTitle,
        groupRoleSetAdmin = patch.groupRoleSetAdmin ?: groupRoleSetAdmin,
        groupRoleRemoveAdmin = patch.groupRoleRemoveAdmin ?: groupRoleRemoveAdmin,
        groupTransferOwner = patch.groupTransferOwner ?: groupTransferOwner,
        groupTransferOwnerConfirm = patch.groupTransferOwnerConfirm ?: groupTransferOwnerConfirm,
        groupMuteDuration10m = patch.groupMuteDuration10m ?: groupMuteDuration10m,
        groupMuteDuration1h = patch.groupMuteDuration1h ?: groupMuteDuration1h,
        groupMuteDuration1d = patch.groupMuteDuration1d ?: groupMuteDuration1d,
        groupMuteDurationForever = patch.groupMuteDurationForever ?: groupMuteDurationForever,
        groupMuteSuccess = patch.groupMuteSuccess ?: groupMuteSuccess,
        groupUnmuteSuccess = patch.groupUnmuteSuccess ?: groupUnmuteSuccess,
        messagePin = patch.messagePin ?: messagePin,
        messageUnpin = patch.messageUnpin ?: messageUnpin,
        pinnedMessagesTitle = patch.pinnedMessagesTitle ?: pinnedMessagesTitle,
        messagePinSuccess = patch.messagePinSuccess ?: messagePinSuccess,
        messageUnpinSuccess = patch.messageUnpinSuccess ?: messageUnpinSuccess,
        presenceOfflineMinutesAgo = patch.presenceOfflineMinutesAgo ?: presenceOfflineMinutesAgo,
        presenceOfflineHoursAgo = patch.presenceOfflineHoursAgo ?: presenceOfflineHoursAgo,
        presenceOfflineDaysAgo = patch.presenceOfflineDaysAgo ?: presenceOfflineDaysAgo,
        groupMembers = patch.groupMembers ?: groupMembers,
        groupOwner = patch.groupOwner ?: groupOwner,
        groupAdmin = patch.groupAdmin ?: groupAdmin,
        groupAdminTag = patch.groupAdminTag ?: groupAdminTag,
        groupMember = patch.groupMember ?: groupMember,
        groupCreate = patch.groupCreate ?: groupCreate,
        groupLeave = patch.groupLeave ?: groupLeave,
        groupDissolve = patch.groupDissolve ?: groupDissolve,
        groupCreateTitle = patch.groupCreateTitle ?: groupCreateTitle,
        groupCreateAction = patch.groupCreateAction ?: groupCreateAction,
        groupCreateFailed = patch.groupCreateFailed ?: groupCreateFailed,
        groupCreateNamePlaceholder = patch.groupCreateNamePlaceholder ?: groupCreateNamePlaceholder,
        groupCreateNoFriends = patch.groupCreateNoFriends ?: groupCreateNoFriends,
        groupInviteTitle = patch.groupInviteTitle ?: groupInviteTitle,
        groupInviteAction = patch.groupInviteAction ?: groupInviteAction,
        groupInviteFailed = patch.groupInviteFailed ?: groupInviteFailed,
        groupInviteNoFriends = patch.groupInviteNoFriends ?: groupInviteNoFriends,
        groupInviteMembers = patch.groupInviteMembers ?: groupInviteMembers,
        groupPickerSearchPlaceholder = patch.groupPickerSearchPlaceholder ?: groupPickerSearchPlaceholder,
        groupPickerNoMatch = patch.groupPickerNoMatch ?: groupPickerNoMatch,
        groupPickerSelectedCount = patch.groupPickerSelectedCount ?: groupPickerSelectedCount,
        groupPickerMaxReached = patch.groupPickerMaxReached ?: groupPickerMaxReached,
        groupOwnerCannotLeave = patch.groupOwnerCannotLeave ?: groupOwnerCannotLeave,
        groupCreateNameSeparator = patch.groupCreateNameSeparator ?: groupCreateNameSeparator,
    )
}

@Immutable
data class PrivChatSettingsStrings(
    val settingsTitle: String,
    val settingsProfile: String,
    val settingsWallet: String,
    val settingsUserId: String,
    /** 「我」页展示自己账号名的标签（PROFILE_VISIBILITY §D1：username 对 Self 可见）。 */
    val settingsUsername: String,
    val settingsMobile: String,
    val settingsNickname: String,
    val settingsSignature: String,
    val settingsGender: String,
    val settingsBirthday: String,
    val settingsNotSet: String,
    val profileLoadFailed: String,
    val profileUpdateFailed: String,
    val profileSessionExpired: String,
    val profileBuiltinSaveUnsupported: String,
    val settingsUsernameRule: String,
    val settingsUsernameLockedDaysPrefix: String,
    val settingsUsernameLockedDaysSuffix: String,
    val settingsUsernameErrorInvalidFormat: String,
    val settingsUsernameErrorReserved: String,
    val settingsUsernameErrorTaken: String,
    val settingsUsernameErrorRateLimited: String,
    val settingsMobileNotEditableTip: String,
    val profileAvatarUploading: String,
    val profileAvatarPendingSave: String,
    val profileAvatarChange: String,
    val profileAvatarTitle: String,
    val profileAvatarPickFromAlbum: String,
    val profileAvatarSaveImage: String,
    val profileAvatarSaved: String,
    val settingsFriendPermission: String,
    val settingsAppearance: String,
    val settingsLanguage: String,
    val settingsLightTheme: String,
    val settingsDarkTheme: String,
    val settingsSystemTheme: String,
    val settingsAbout: String,
    val settingsMore: String,
    val settingsLogout: String,
    val settingsDeleteAccount: String,
    val settingsDeleteAccountTitle: String,
    val settingsDeleteAccountHint: String,
    val settingsDeleteAccountConfirm: String,
    val deleteAccountFailed: String,
    val settingsSectionAccount: String,
    val settingsAccountSecurity: String,
    val settingsPrivacy: String,
    val privacySectionAddMe: String,
    val privacySectionFindMe: String,
    val privacySectionMisc: String,
    val privacyAddByGroup: String,
    val privacyAddByCard: String,
    val privacySearchByUsername: String,
    val privacySearchByPhone: String,
    val privacySearchByQrcode: String,
    val privacyNonFriendMessage: String,
    val privacyNonFriendView: String,
    val privacyHint: String,
    val settingsSectionGeneral: String,
    val settingsNotification: String,
    val notificationsSectionAlert: String,
    val notificationsSound: String,
    /**
     * 振动开关的标题。文案里写明"应用内"是刻意的：远程通知的振动由系统决定，
     * APNs / FCM 的 payload 里没有这个旋钮，这个开关管不到锁屏。
     */
    val notificationsVibration: String,
    val notificationsSectionMute: String,
    val notificationsGlobalMute: String,
    val notificationsSectionPrivacy: String,
    val notificationsHidePreview: String,
    val notificationsHidePreviewHint: String,
    val notificationsSyncFailed: String,
    val settingsSectionHelp: String,
    val settingsHelp: String,
    val settingsFeedback: String,
    val settingsSwitchAccount: String,
    val settingsAddAccount: String,
)

data class PrivChatSettingsStringsPatch(
    val settingsTitle: String? = null,
    val settingsProfile: String? = null,
    val settingsUserId: String? = null,
    val settingsUsername: String? = null,
    val settingsMobile: String? = null,
    val settingsNickname: String? = null,
    val settingsSignature: String? = null,
    val settingsGender: String? = null,
    val settingsBirthday: String? = null,
    val settingsNotSet: String? = null,
    val profileLoadFailed: String? = null,
    val profileUpdateFailed: String? = null,
    val profileSessionExpired: String? = null,
    val profileBuiltinSaveUnsupported: String? = null,
    val settingsUsernameRule: String? = null,
    val settingsUsernameLockedDaysPrefix: String? = null,
    val settingsUsernameLockedDaysSuffix: String? = null,
    val settingsUsernameErrorInvalidFormat: String? = null,
    val settingsUsernameErrorReserved: String? = null,
    val settingsUsernameErrorTaken: String? = null,
    val settingsUsernameErrorRateLimited: String? = null,
    val settingsMobileNotEditableTip: String? = null,
    val profileAvatarUploading: String? = null,
    val profileAvatarPendingSave: String? = null,
    val profileAvatarChange: String? = null,
    val profileAvatarTitle: String? = null,
    val profileAvatarPickFromAlbum: String? = null,
    val profileAvatarSaveImage: String? = null,
    val profileAvatarSaved: String? = null,
    val settingsFriendPermission: String? = null,
    val settingsAppearance: String? = null,
    val settingsLanguage: String? = null,
    val settingsLightTheme: String? = null,
    val settingsDarkTheme: String? = null,
    val settingsSystemTheme: String? = null,
    val settingsAbout: String? = null,
    val settingsMore: String? = null,
    val settingsLogout: String? = null,
    val settingsDeleteAccount: String? = null,
    val settingsDeleteAccountTitle: String? = null,
    val settingsDeleteAccountHint: String? = null,
    val settingsDeleteAccountConfirm: String? = null,
    val deleteAccountFailed: String? = null,
    val settingsSectionAccount: String? = null,
    val settingsAccountSecurity: String? = null,
    val settingsPrivacy: String? = null,
    val privacySectionAddMe: String? = null,
    val privacySectionFindMe: String? = null,
    val privacySectionMisc: String? = null,
    val privacyAddByGroup: String? = null,
    val privacyAddByCard: String? = null,
    val privacySearchByUsername: String? = null,
    val privacySearchByPhone: String? = null,
    val privacySearchByQrcode: String? = null,
    val privacyNonFriendMessage: String? = null,
    val privacyNonFriendView: String? = null,
    val privacyHint: String? = null,
    val settingsSectionGeneral: String? = null,
    val settingsNotification: String? = null,
    val notificationsSectionAlert: String? = null,
    val notificationsSound: String? = null,
    val notificationsVibration: String? = null,
    val notificationsSectionMute: String? = null,
    val notificationsGlobalMute: String? = null,
    val notificationsSectionPrivacy: String? = null,
    val notificationsHidePreview: String? = null,
    val notificationsHidePreviewHint: String? = null,
    val notificationsSyncFailed: String? = null,
    val settingsSectionHelp: String? = null,
    val settingsHelp: String? = null,
    val settingsFeedback: String? = null,
    val settingsSwitchAccount: String? = null,
    val settingsAddAccount: String? = null,
)

val PrivChatSettingsStringsPatch.isEmpty: Boolean
    get() = settingsTitle == null &&
        settingsProfile == null &&
        settingsUserId == null &&
        settingsUsername == null &&
        settingsMobile == null &&
        settingsNickname == null &&
        settingsSignature == null &&
        settingsGender == null &&
        settingsBirthday == null &&
        settingsNotSet == null &&
        profileLoadFailed == null &&
        profileUpdateFailed == null &&
        profileSessionExpired == null &&
        profileBuiltinSaveUnsupported == null &&
        settingsUsernameRule == null &&
        settingsUsernameLockedDaysPrefix == null &&
        settingsUsernameLockedDaysSuffix == null &&
        settingsUsernameErrorInvalidFormat == null &&
        settingsUsernameErrorReserved == null &&
        settingsUsernameErrorTaken == null &&
        settingsUsernameErrorRateLimited == null &&
        settingsMobileNotEditableTip == null &&
        profileAvatarUploading == null &&
        profileAvatarPendingSave == null &&
        profileAvatarChange == null &&
        profileAvatarTitle == null &&
        profileAvatarPickFromAlbum == null &&
        profileAvatarSaveImage == null &&
        profileAvatarSaved == null &&
        settingsFriendPermission == null &&
        settingsAppearance == null &&
        settingsLanguage == null &&
        settingsLightTheme == null &&
        settingsDarkTheme == null &&
        settingsSystemTheme == null &&
        settingsAbout == null &&
        settingsMore == null &&
        settingsLogout == null &&
        settingsDeleteAccount == null &&
        settingsDeleteAccountTitle == null &&
        settingsDeleteAccountHint == null &&
        settingsDeleteAccountConfirm == null &&
        deleteAccountFailed == null &&
        settingsSectionAccount == null &&
        settingsAccountSecurity == null &&
        settingsPrivacy == null &&
        privacySectionAddMe == null &&
        privacySectionFindMe == null &&
        privacySectionMisc == null &&
        privacyAddByGroup == null &&
        privacyAddByCard == null &&
        privacySearchByUsername == null &&
        privacySearchByPhone == null &&
        privacySearchByQrcode == null &&
        privacyNonFriendMessage == null &&
        privacyNonFriendView == null &&
        privacyHint == null &&
        settingsSectionGeneral == null &&
        settingsNotification == null &&
        notificationsSectionAlert == null &&
        notificationsSound == null &&
        notificationsVibration == null &&
        notificationsSectionMute == null &&
        notificationsGlobalMute == null &&
        notificationsSectionPrivacy == null &&
        notificationsHidePreview == null &&
        notificationsHidePreviewHint == null &&
        notificationsSyncFailed == null &&
        settingsSectionHelp == null &&
        settingsHelp == null &&
        settingsFeedback == null &&
        settingsSwitchAccount == null &&
        settingsAddAccount == null

fun PrivChatSettingsStrings.merge(patch: PrivChatSettingsStringsPatch?): PrivChatSettingsStrings {
    if (patch == null || patch.isEmpty) return this
    return copy(
        settingsTitle = patch.settingsTitle ?: settingsTitle,
        settingsProfile = patch.settingsProfile ?: settingsProfile,
        settingsUserId = patch.settingsUserId ?: settingsUserId,
        settingsUsername = patch.settingsUsername ?: settingsUsername,
        settingsMobile = patch.settingsMobile ?: settingsMobile,
        settingsNickname = patch.settingsNickname ?: settingsNickname,
        settingsSignature = patch.settingsSignature ?: settingsSignature,
        settingsGender = patch.settingsGender ?: settingsGender,
        settingsBirthday = patch.settingsBirthday ?: settingsBirthday,
        settingsNotSet = patch.settingsNotSet ?: settingsNotSet,
        profileLoadFailed = patch.profileLoadFailed ?: profileLoadFailed,
        profileUpdateFailed = patch.profileUpdateFailed ?: profileUpdateFailed,
        profileSessionExpired = patch.profileSessionExpired ?: profileSessionExpired,
        profileBuiltinSaveUnsupported = patch.profileBuiltinSaveUnsupported ?: profileBuiltinSaveUnsupported,
        settingsUsernameRule = patch.settingsUsernameRule ?: settingsUsernameRule,
        settingsUsernameLockedDaysPrefix = patch.settingsUsernameLockedDaysPrefix ?: settingsUsernameLockedDaysPrefix,
        settingsUsernameLockedDaysSuffix = patch.settingsUsernameLockedDaysSuffix ?: settingsUsernameLockedDaysSuffix,
        settingsUsernameErrorInvalidFormat = patch.settingsUsernameErrorInvalidFormat ?: settingsUsernameErrorInvalidFormat,
        settingsUsernameErrorReserved = patch.settingsUsernameErrorReserved ?: settingsUsernameErrorReserved,
        settingsUsernameErrorTaken = patch.settingsUsernameErrorTaken ?: settingsUsernameErrorTaken,
        settingsUsernameErrorRateLimited = patch.settingsUsernameErrorRateLimited ?: settingsUsernameErrorRateLimited,
        settingsMobileNotEditableTip = patch.settingsMobileNotEditableTip ?: settingsMobileNotEditableTip,
        profileAvatarUploading = patch.profileAvatarUploading ?: profileAvatarUploading,
        profileAvatarPendingSave = patch.profileAvatarPendingSave ?: profileAvatarPendingSave,
        profileAvatarChange = patch.profileAvatarChange ?: profileAvatarChange,
        profileAvatarTitle = patch.profileAvatarTitle ?: profileAvatarTitle,
        profileAvatarPickFromAlbum = patch.profileAvatarPickFromAlbum ?: profileAvatarPickFromAlbum,
        profileAvatarSaveImage = patch.profileAvatarSaveImage ?: profileAvatarSaveImage,
        profileAvatarSaved = patch.profileAvatarSaved ?: profileAvatarSaved,
        settingsFriendPermission = patch.settingsFriendPermission ?: settingsFriendPermission,
        settingsAppearance = patch.settingsAppearance ?: settingsAppearance,
        settingsLanguage = patch.settingsLanguage ?: settingsLanguage,
        settingsLightTheme = patch.settingsLightTheme ?: settingsLightTheme,
        settingsDarkTheme = patch.settingsDarkTheme ?: settingsDarkTheme,
        settingsSystemTheme = patch.settingsSystemTheme ?: settingsSystemTheme,
        settingsAbout = patch.settingsAbout ?: settingsAbout,
        settingsMore = patch.settingsMore ?: settingsMore,
        settingsLogout = patch.settingsLogout ?: settingsLogout,
        settingsDeleteAccount = patch.settingsDeleteAccount ?: settingsDeleteAccount,
        settingsDeleteAccountTitle = patch.settingsDeleteAccountTitle ?: settingsDeleteAccountTitle,
        settingsDeleteAccountHint = patch.settingsDeleteAccountHint ?: settingsDeleteAccountHint,
        settingsDeleteAccountConfirm = patch.settingsDeleteAccountConfirm ?: settingsDeleteAccountConfirm,
        deleteAccountFailed = patch.deleteAccountFailed ?: deleteAccountFailed,
        settingsSectionAccount = patch.settingsSectionAccount ?: settingsSectionAccount,
        settingsAccountSecurity = patch.settingsAccountSecurity ?: settingsAccountSecurity,
        settingsPrivacy = patch.settingsPrivacy ?: settingsPrivacy,
        privacySectionAddMe = patch.privacySectionAddMe ?: privacySectionAddMe,
        privacySectionFindMe = patch.privacySectionFindMe ?: privacySectionFindMe,
        privacySectionMisc = patch.privacySectionMisc ?: privacySectionMisc,
        privacyAddByGroup = patch.privacyAddByGroup ?: privacyAddByGroup,
        privacyAddByCard = patch.privacyAddByCard ?: privacyAddByCard,
        privacySearchByUsername = patch.privacySearchByUsername ?: privacySearchByUsername,
        privacySearchByPhone = patch.privacySearchByPhone ?: privacySearchByPhone,
        privacySearchByQrcode = patch.privacySearchByQrcode ?: privacySearchByQrcode,
        privacyNonFriendMessage = patch.privacyNonFriendMessage ?: privacyNonFriendMessage,
        privacyNonFriendView = patch.privacyNonFriendView ?: privacyNonFriendView,
        privacyHint = patch.privacyHint ?: privacyHint,
        settingsSectionGeneral = patch.settingsSectionGeneral ?: settingsSectionGeneral,
        settingsNotification = patch.settingsNotification ?: settingsNotification,
        notificationsSectionAlert = patch.notificationsSectionAlert ?: notificationsSectionAlert,
        notificationsSound = patch.notificationsSound ?: notificationsSound,
        notificationsVibration = patch.notificationsVibration ?: notificationsVibration,
        notificationsSectionMute = patch.notificationsSectionMute ?: notificationsSectionMute,
        notificationsGlobalMute = patch.notificationsGlobalMute ?: notificationsGlobalMute,
        notificationsSectionPrivacy = patch.notificationsSectionPrivacy ?: notificationsSectionPrivacy,
        notificationsHidePreview = patch.notificationsHidePreview ?: notificationsHidePreview,
        notificationsHidePreviewHint = patch.notificationsHidePreviewHint ?: notificationsHidePreviewHint,
        notificationsSyncFailed = patch.notificationsSyncFailed ?: notificationsSyncFailed,
        settingsSectionHelp = patch.settingsSectionHelp ?: settingsSectionHelp,
        settingsHelp = patch.settingsHelp ?: settingsHelp,
        settingsFeedback = patch.settingsFeedback ?: settingsFeedback,
        settingsSwitchAccount = patch.settingsSwitchAccount ?: settingsSwitchAccount,
        settingsAddAccount = patch.settingsAddAccount ?: settingsAddAccount,
    )
}

@Immutable
data class PrivChatAboutStrings(
    val aboutTitle: String,
    val aboutSdkVersion: String,
    val aboutVersion: String,
    val aboutPrivacyPolicy: String,
    val aboutUserAgreement: String,
    val aboutDisclaimer: String,
    val aboutContactUs: String,
    val aboutOfficialWebsite: String,
    val aboutSourceCode: String,
    val aboutCopyright: String,
    val aboutUiVersion: String,
    val aboutGitCommit: String,
    val aboutBuildTime: String,
    val aboutSdkStatus: String,
    val aboutConnectionState: String,
    val aboutLoginState: String,
    val aboutLastSdkError: String,
    val aboutConnStateDisconnected: String,
    val aboutConnStateConnecting: String,
    val aboutConnStateConnected: String,
    val aboutConnStateReconnecting: String,
    val aboutConnStateFailed: String,
    val aboutLoginStateLoggedOut: String,
    val aboutLoginStateLoggingIn: String,
    val aboutLoginStateSyncing: String,
    val aboutLoginStateSyncReady: String,
    val aboutLoginStateLoggedIn: String,
    val contactUsTitle: String,
    val contactUsSwitch: String,
    val contactUsQuestion: String,
    val contactUsServiceTime: String,
    val contactUsChatNow: String,
)

data class PrivChatAboutStringsPatch(
    val aboutTitle: String? = null,
    val aboutSdkVersion: String? = null,
    val aboutVersion: String? = null,
    val aboutPrivacyPolicy: String? = null,
    val aboutUserAgreement: String? = null,
    val aboutDisclaimer: String? = null,
    val aboutContactUs: String? = null,
    val aboutOfficialWebsite: String? = null,
    val aboutSourceCode: String? = null,
    val aboutCopyright: String? = null,
    val contactUsTitle: String? = null,
    val contactUsSwitch: String? = null,
    val contactUsQuestion: String? = null,
    val contactUsServiceTime: String? = null,
    val contactUsChatNow: String? = null,
)

val PrivChatAboutStringsPatch.isEmpty: Boolean
    get() = aboutTitle == null &&
        aboutSdkVersion == null &&
        aboutVersion == null &&
        aboutPrivacyPolicy == null &&
        aboutUserAgreement == null &&
        aboutDisclaimer == null &&
        aboutContactUs == null &&
        aboutOfficialWebsite == null &&
        aboutSourceCode == null &&
        aboutCopyright == null &&
        contactUsTitle == null &&
        contactUsSwitch == null &&
        contactUsQuestion == null &&
        contactUsServiceTime == null &&
        contactUsChatNow == null

fun PrivChatAboutStrings.merge(patch: PrivChatAboutStringsPatch?): PrivChatAboutStrings {
    if (patch == null || patch.isEmpty) return this
    return copy(
        aboutTitle = patch.aboutTitle ?: aboutTitle,
        aboutSdkVersion = patch.aboutSdkVersion ?: aboutSdkVersion,
        aboutVersion = patch.aboutVersion ?: aboutVersion,
        aboutPrivacyPolicy = patch.aboutPrivacyPolicy ?: aboutPrivacyPolicy,
        aboutUserAgreement = patch.aboutUserAgreement ?: aboutUserAgreement,
        aboutDisclaimer = patch.aboutDisclaimer ?: aboutDisclaimer,
        aboutContactUs = patch.aboutContactUs ?: aboutContactUs,
        aboutOfficialWebsite = patch.aboutOfficialWebsite ?: aboutOfficialWebsite,
        aboutSourceCode = patch.aboutSourceCode ?: aboutSourceCode,
        aboutCopyright = patch.aboutCopyright ?: aboutCopyright,
        contactUsTitle = patch.contactUsTitle ?: contactUsTitle,
        contactUsSwitch = patch.contactUsSwitch ?: contactUsSwitch,
        contactUsQuestion = patch.contactUsQuestion ?: contactUsQuestion,
        contactUsServiceTime = patch.contactUsServiceTime ?: contactUsServiceTime,
        contactUsChatNow = patch.contactUsChatNow ?: contactUsChatNow,
    )
}

@Immutable
data class PrivChatAuxiliaryStrings(
    val changePasswordTitle: String,
    val changePasswordNew: String,
    val changePasswordConfirm: String,
    val changePasswordSmsCode: String,
    val changePasswordSendCode: String,
    val changePasswordSubmit: String,
    val changePasswordSuccess: String,
    val changePasswordMismatch: String,
    val setPasswordTitle: String,
    val setPasswordHint: String,
    val setPasswordSubmit: String,
    val setPasswordSuccess: String,
    val setNicknameTitle: String,
    val setNicknameHint: String,
    val setNicknameSubmit: String,
    val setNicknameError: String,
    val requiredActionUnsupportedTitle: String,
    val requiredActionUnsupportedMessage: String,
    val requiredActionUnsupportedReload: String,
    val timeYesterday: String,
    val timeToday: String,
    val timeSunday: String,
    val timeMonday: String,
    val timeTuesday: String,
    val timeWednesday: String,
    val timeThursday: String,
    val timeFriday: String,
    val timeSaturday: String,
    val systemTemplates: Map<String, String>,
    val systemListSeparator: String,
    val previewImage: String,
    val previewVideo: String,
    val previewVoice: String,
    val previewVoiceWithDuration: String,
    val previewFile: String,
    val previewFileWithName: String,
    val previewSticker: String,
    val previewLocation: String,
    val previewLocationWithAddress: String,
    val previewLink: String,
    val previewContactCard: String,
    val previewRedPacket: String,
    /** 转发消息 / 转账的类型占位符。与服务端 `PushLocale::preview_for_type` 同一份分类。 */
    val previewForward: String,
    val previewMoneyTransfer: String,
    val previewSystemFallback: String,
    val previewUnknown: String,
    val previewRecalled: String,
    val sendSmsCodeFailed: String,
    val connectServerFailed: String,
    val sdkInitFailed: String,
    val switchAccountFailed: String,
    val logoutFailed: String,
    val loginFailed: String,
    val friendRequestAcceptFailed: String,
    val friendRequestDeclineFailed: String,
    val messageRecallFailed: String,
    val qrImageDecodeFailed: String,
    val qrScanAimHint: String,
    val qrScanPickFromAlbum: String,
    val qrScanNoQrInImage: String,
    val qrScanNotPrivChat: String,
    val qrScanUnsupported: String,
    val qrScanSelf: String,
    val qrScanResolveFailed: String,
    val qrScanGroupJoined: String,
    val qrScanGroupPending: String,
    val saveFailed: String,
    val operationFailed: String,
    val smsCodeError: String,
    val loginErrInvalidCredentials: String,
    val loginErrAccountDisabled: String,
    val registerErrUsernameTaken: String,
    val registerErrUsernameInvalid: String,
    val registerErrUsernameFormat: String,
    val registerErrPasswordTooShort: String,
    val systemMessagesName: String,
    val groupChatFallback: String,
    val signInTitle: String,
    val signInButton: String,
    val signInDoneToday: String,
    val signInContinuousPrefix: String,
    val signInTodayReward: String,
    val signInRewardList: String,
    val signInPointsUnit: String,
    val signInDayUnit: String,
    val signInSuccessTitle: String,
    val signInCashCredited: String,
    val bindMobileSkip: String,
    val bindMobileTitle: String,
    val bindMobileHint: String,
    val bindMobilePlaceholder: String,
    val bindMobileButton: String,
    val bindMobileErrTaken: String,
    val bindMobileErrBound: String,
    val bindMobileErrInvalid: String,
    val inviteBindTitle: String,
    val inviteBindPlaceholder: String,
    val inviteBindButton: String,
    val inviteBoundCode: String,
    val inviteBoundInviter: String,
    val inviteBoundAt: String,
    val inviteBoundHint: String,
    val inviteBoundAutoFriend: String,
    val inviteBindHint: String,
    val inviteBindSuccessFriend: String,
    val inviteBindSuccess: String,
    val inviteErrInvalid: String,
    val loginTabSms: String,
    val loginTabPassword: String,
    val loginUsernamePlaceholder: String,
    val loginPasswordPlaceholder: String,
    val loginPasswordNewPlaceholder: String,
    val loginNicknamePlaceholder: String,
    val loginInviteCodePlaceholder: String,
    val loginRegisterButton: String,
    val loginToRegister: String,
    val loginToLogin: String,
    // P4 运行时状态条（CLIENT_GLOBAL_STATE §17）
    val bannerConnecting: String,
    val bannerDisconnected: String,
    val bannerConnected: String,
    val bannerReconnecting: String,
    val bannerConnectFailed: String,
    val bannerSyncing: String,
    val loginExpired: String,
    val syncFailedRetry: String,
    val bannerServerBusy: String,
    val startupInitializing: String,
    val startupVerifyingAccount: String,
    val startupConnecting: String,
    val startupLoadingLocal: String,
    val startupSyncing: String,
    /**
     * 首屏同步进度。[syncStageTemplate] 是整句模板，用 {stage} 占位——英语/越南语的
     * 语序与中文不同，"正在同步" + 名词这种拼接在那两种语言里会读不通。
     */
    val syncStageTemplate: String,
    val syncStageContacts: String,
    val syncStageGroups: String,
    val syncStageConversations: String,
    val syncStageProfiles: String,
    val syncStageReadState: String,
    val syncStagePrivacy: String,
    /** 「几月几日」的展示格式。用 {m}/{d} 具名占位符——各语言的年月日顺序不同，位置参数排不出来。 */
    val dateMonthDayPattern: String,
    val dateYearMonthDayPattern: String,
    val dateYearMonthPattern: String,
    /** 日期 + 时刻的拼接顺序。 */
    val dateTimePattern: String,
    val relativeJustNow: String,
    val relativeMinutesAgo: String,
    val relativeHoursAgo: String,
    val relativeDaysAgo: String,
)

data class PrivChatAuxiliaryStringsPatch(
    val syncStageTemplate: String? = null,
    val syncStageContacts: String? = null,
    val syncStageGroups: String? = null,
    val syncStageConversations: String? = null,
    val syncStageProfiles: String? = null,
    val syncStageReadState: String? = null,
    val syncStagePrivacy: String? = null,
    val dateMonthDayPattern: String? = null,
    val dateYearMonthDayPattern: String? = null,
    val dateYearMonthPattern: String? = null,
    val dateTimePattern: String? = null,
    val relativeJustNow: String? = null,
    val relativeMinutesAgo: String? = null,
    val relativeHoursAgo: String? = null,
    val relativeDaysAgo: String? = null,
    val changePasswordTitle: String? = null,
    val changePasswordNew: String? = null,
    val changePasswordConfirm: String? = null,
    val changePasswordSmsCode: String? = null,
    val changePasswordSendCode: String? = null,
    val changePasswordSubmit: String? = null,
    val changePasswordSuccess: String? = null,
    val changePasswordMismatch: String? = null,
    val setPasswordTitle: String? = null,
    val setPasswordHint: String? = null,
    val setPasswordSubmit: String? = null,
    val setPasswordSuccess: String? = null,
    val setNicknameTitle: String? = null,
    val setNicknameHint: String? = null,
    val setNicknameSubmit: String? = null,
    val setNicknameError: String? = null,
    val requiredActionUnsupportedTitle: String? = null,
    val requiredActionUnsupportedMessage: String? = null,
    val requiredActionUnsupportedReload: String? = null,
    val timeYesterday: String? = null,
    val timeToday: String? = null,
    val timeSunday: String? = null,
    val timeMonday: String? = null,
    val timeTuesday: String? = null,
    val timeWednesday: String? = null,
    val timeThursday: String? = null,
    val timeFriday: String? = null,
    val timeSaturday: String? = null,
    val systemTemplates: Map<String, String>? = null,
    val systemListSeparator: String? = null,
    val previewImage: String? = null,
    val previewVideo: String? = null,
    val previewVoice: String? = null,
    val previewVoiceWithDuration: String? = null,
    val previewFile: String? = null,
    val previewFileWithName: String? = null,
    val previewSticker: String? = null,
    val previewLocation: String? = null,
    val previewLocationWithAddress: String? = null,
    val previewLink: String? = null,
    val previewContactCard: String? = null,
    val previewRedPacket: String? = null,
    val previewForward: String? = null,
    val previewMoneyTransfer: String? = null,
    val previewSystemFallback: String? = null,
    val previewUnknown: String? = null,
    val previewRecalled: String? = null,
    val sendSmsCodeFailed: String? = null,
    val connectServerFailed: String? = null,
    val sdkInitFailed: String? = null,
    val switchAccountFailed: String? = null,
    val logoutFailed: String? = null,
    val loginFailed: String? = null,
    val friendRequestAcceptFailed: String? = null,
    val friendRequestDeclineFailed: String? = null,
    val messageRecallFailed: String? = null,
    val qrImageDecodeFailed: String? = null,
    val qrScanAimHint: String? = null,
    val qrScanPickFromAlbum: String? = null,
    val qrScanNoQrInImage: String? = null,
    val qrScanNotPrivChat: String? = null,
    val qrScanUnsupported: String? = null,
    val qrScanSelf: String? = null,
    val qrScanResolveFailed: String? = null,
    val qrScanGroupJoined: String? = null,
    val qrScanGroupPending: String? = null,
    val saveFailed: String? = null,
    val operationFailed: String? = null,
    val smsCodeError: String? = null,
    val loginErrInvalidCredentials: String? = null,
    val loginErrAccountDisabled: String? = null,
    val registerErrUsernameTaken: String? = null,
    val registerErrUsernameInvalid: String? = null,
    val registerErrUsernameFormat: String? = null,
    val registerErrPasswordTooShort: String? = null,
    val systemMessagesName: String? = null,
    val groupChatFallback: String? = null,
    val signInTitle: String? = null,
    val signInButton: String? = null,
    val signInDoneToday: String? = null,
    val signInContinuousPrefix: String? = null,
    val signInTodayReward: String? = null,
    val signInRewardList: String? = null,
    val signInPointsUnit: String? = null,
    val signInDayUnit: String? = null,
    val signInSuccessTitle: String? = null,
    val signInCashCredited: String? = null,
    val bindMobileSkip: String? = null,
    val bindMobileTitle: String? = null,
    val bindMobileHint: String? = null,
    val bindMobilePlaceholder: String? = null,
    val bindMobileButton: String? = null,
    val bindMobileErrTaken: String? = null,
    val bindMobileErrBound: String? = null,
    val bindMobileErrInvalid: String? = null,
    val inviteBindTitle: String? = null,
    val inviteBindPlaceholder: String? = null,
    val inviteBindButton: String? = null,
    val inviteBoundCode: String? = null,
    val inviteBoundInviter: String? = null,
    val inviteBoundAt: String? = null,
    val inviteBoundHint: String? = null,
    val inviteBoundAutoFriend: String? = null,
    val inviteBindHint: String? = null,
    val inviteBindSuccessFriend: String? = null,
    val inviteBindSuccess: String? = null,
    val inviteErrInvalid: String? = null,
    val loginTabSms: String? = null,
    val loginTabPassword: String? = null,
    val loginUsernamePlaceholder: String? = null,
    val loginPasswordPlaceholder: String? = null,
    val loginPasswordNewPlaceholder: String? = null,
    val loginNicknamePlaceholder: String? = null,
    val loginInviteCodePlaceholder: String? = null,
    val loginRegisterButton: String? = null,
    val loginToRegister: String? = null,
    val loginToLogin: String? = null,
    val bannerConnecting: String? = null,
    val bannerDisconnected: String? = null,
    val bannerConnected: String? = null,
    val bannerReconnecting: String? = null,
    val bannerConnectFailed: String? = null,
    val bannerSyncing: String? = null,
    val loginExpired: String? = null,
    val syncFailedRetry: String? = null,
    val bannerServerBusy: String? = null,
    val startupInitializing: String? = null,
    val startupVerifyingAccount: String? = null,
    val startupConnecting: String? = null,
    val startupLoadingLocal: String? = null,
    val startupSyncing: String? = null,
)

val PrivChatAuxiliaryStringsPatch.isEmpty: Boolean
    get() = changePasswordTitle == null &&
        changePasswordNew == null &&
        changePasswordConfirm == null &&
        changePasswordSmsCode == null &&
        changePasswordSendCode == null &&
        changePasswordSubmit == null &&
        changePasswordSuccess == null &&
        changePasswordMismatch == null &&
        setPasswordTitle == null &&
        setPasswordHint == null &&
        setPasswordSubmit == null &&
        setPasswordSuccess == null &&
        setNicknameTitle == null &&
        setNicknameHint == null &&
        setNicknameSubmit == null &&
        setNicknameError == null &&
        requiredActionUnsupportedTitle == null &&
        requiredActionUnsupportedMessage == null &&
        requiredActionUnsupportedReload == null &&
        timeYesterday == null &&
        timeToday == null &&
        timeSunday == null &&
        timeMonday == null &&
        timeTuesday == null &&
        timeWednesday == null &&
        timeThursday == null &&
        timeFriday == null &&
        timeSaturday == null &&
        systemTemplates == null &&
        systemListSeparator == null &&
        previewImage == null &&
        previewVideo == null &&
        previewVoice == null &&
        previewVoiceWithDuration == null &&
        previewFile == null &&
        previewFileWithName == null &&
        previewSticker == null &&
        previewLocation == null &&
        previewLocationWithAddress == null &&
        previewLink == null &&
        previewContactCard == null &&
        previewRedPacket == null &&
        previewForward == null &&
        previewMoneyTransfer == null &&
        previewSystemFallback == null &&
        previewUnknown == null &&
        previewRecalled == null &&
        sendSmsCodeFailed == null &&
        connectServerFailed == null &&
        sdkInitFailed == null &&
        switchAccountFailed == null &&
        logoutFailed == null &&
        loginFailed == null &&
        friendRequestAcceptFailed == null &&
        friendRequestDeclineFailed == null &&
        messageRecallFailed == null &&
        qrImageDecodeFailed == null &&
        qrScanAimHint == null &&
        qrScanPickFromAlbum == null &&
        qrScanNoQrInImage == null &&
        qrScanNotPrivChat == null &&
        qrScanUnsupported == null &&
        qrScanSelf == null &&
        qrScanResolveFailed == null &&
        qrScanGroupJoined == null &&
        qrScanGroupPending == null &&
        saveFailed == null &&
        operationFailed == null &&
        smsCodeError == null &&
        loginErrInvalidCredentials == null &&
        loginErrAccountDisabled == null &&
        registerErrUsernameTaken == null &&
        registerErrUsernameInvalid == null &&
        registerErrUsernameFormat == null &&
        registerErrPasswordTooShort == null &&
        systemMessagesName == null &&
        groupChatFallback == null &&
        signInTitle == null &&
        signInButton == null &&
        signInDoneToday == null &&
        signInContinuousPrefix == null &&
        signInTodayReward == null &&
        signInRewardList == null &&
        signInPointsUnit == null &&
        signInDayUnit == null &&
        signInSuccessTitle == null &&
        signInCashCredited == null &&
        bindMobileSkip == null &&
        bindMobileTitle == null &&
        bindMobileHint == null &&
        bindMobilePlaceholder == null &&
        bindMobileButton == null &&
        bindMobileErrTaken == null &&
        bindMobileErrBound == null &&
        bindMobileErrInvalid == null &&
        inviteBindTitle == null &&
        inviteBindPlaceholder == null &&
        inviteBindButton == null &&
        inviteBoundCode == null &&
        inviteBoundInviter == null &&
        inviteBoundAt == null &&
        inviteBoundHint == null &&
        inviteBoundAutoFriend == null &&
        inviteBindHint == null &&
        inviteBindSuccessFriend == null &&
        inviteBindSuccess == null &&
        inviteErrInvalid == null &&
        loginTabSms == null &&
        loginTabPassword == null &&
        loginUsernamePlaceholder == null &&
        loginPasswordPlaceholder == null &&
        loginPasswordNewPlaceholder == null &&
        loginNicknamePlaceholder == null &&
        loginInviteCodePlaceholder == null &&
        loginRegisterButton == null &&
        loginToRegister == null &&
        loginToLogin == null &&
        bannerConnecting == null &&
        bannerDisconnected == null &&
        bannerConnected == null &&
        bannerReconnecting == null &&
        bannerConnectFailed == null &&
        bannerSyncing == null &&
        loginExpired == null &&
        syncFailedRetry == null &&
        bannerServerBusy == null &&
        startupInitializing == null &&
        startupVerifyingAccount == null &&
        startupConnecting == null &&
        startupLoadingLocal == null &&
        startupSyncing == null &&
        syncStageTemplate == null &&
        syncStageContacts == null &&
        syncStageGroups == null &&
        syncStageConversations == null &&
        syncStageProfiles == null &&
        syncStageReadState == null &&
        syncStagePrivacy == null &&
        dateMonthDayPattern == null &&
        dateYearMonthDayPattern == null &&
        dateYearMonthPattern == null &&
        dateTimePattern == null &&
        relativeJustNow == null &&
        relativeMinutesAgo == null &&
        relativeHoursAgo == null &&
        relativeDaysAgo == null

fun PrivChatAuxiliaryStrings.merge(patch: PrivChatAuxiliaryStringsPatch?): PrivChatAuxiliaryStrings {
    if (patch == null || patch.isEmpty) return this
    return copy(
        changePasswordTitle = patch.changePasswordTitle ?: changePasswordTitle,
        changePasswordNew = patch.changePasswordNew ?: changePasswordNew,
        changePasswordConfirm = patch.changePasswordConfirm ?: changePasswordConfirm,
        changePasswordSmsCode = patch.changePasswordSmsCode ?: changePasswordSmsCode,
        changePasswordSendCode = patch.changePasswordSendCode ?: changePasswordSendCode,
        changePasswordSubmit = patch.changePasswordSubmit ?: changePasswordSubmit,
        changePasswordSuccess = patch.changePasswordSuccess ?: changePasswordSuccess,
        changePasswordMismatch = patch.changePasswordMismatch ?: changePasswordMismatch,
        setPasswordTitle = patch.setPasswordTitle ?: setPasswordTitle,
        setPasswordHint = patch.setPasswordHint ?: setPasswordHint,
        setPasswordSubmit = patch.setPasswordSubmit ?: setPasswordSubmit,
        setPasswordSuccess = patch.setPasswordSuccess ?: setPasswordSuccess,
        setNicknameTitle = patch.setNicknameTitle ?: setNicknameTitle,
        setNicknameHint = patch.setNicknameHint ?: setNicknameHint,
        setNicknameSubmit = patch.setNicknameSubmit ?: setNicknameSubmit,
        setNicknameError = patch.setNicknameError ?: setNicknameError,
        requiredActionUnsupportedTitle = patch.requiredActionUnsupportedTitle ?: requiredActionUnsupportedTitle,
        requiredActionUnsupportedMessage = patch.requiredActionUnsupportedMessage ?: requiredActionUnsupportedMessage,
        requiredActionUnsupportedReload = patch.requiredActionUnsupportedReload ?: requiredActionUnsupportedReload,
        timeYesterday = patch.timeYesterday ?: timeYesterday,
        timeToday = patch.timeToday ?: timeToday,
        timeSunday = patch.timeSunday ?: timeSunday,
        timeMonday = patch.timeMonday ?: timeMonday,
        timeTuesday = patch.timeTuesday ?: timeTuesday,
        timeWednesday = patch.timeWednesday ?: timeWednesday,
        timeThursday = patch.timeThursday ?: timeThursday,
        timeFriday = patch.timeFriday ?: timeFriday,
        timeSaturday = patch.timeSaturday ?: timeSaturday,
        systemTemplates = patch.systemTemplates ?: systemTemplates,
        systemListSeparator = patch.systemListSeparator ?: systemListSeparator,
        previewImage = patch.previewImage ?: previewImage,
        previewVideo = patch.previewVideo ?: previewVideo,
        previewVoice = patch.previewVoice ?: previewVoice,
        previewVoiceWithDuration = patch.previewVoiceWithDuration ?: previewVoiceWithDuration,
        previewFile = patch.previewFile ?: previewFile,
        previewFileWithName = patch.previewFileWithName ?: previewFileWithName,
        previewSticker = patch.previewSticker ?: previewSticker,
        previewLocation = patch.previewLocation ?: previewLocation,
        previewLocationWithAddress = patch.previewLocationWithAddress ?: previewLocationWithAddress,
        previewLink = patch.previewLink ?: previewLink,
        previewContactCard = patch.previewContactCard ?: previewContactCard,
        previewRedPacket = patch.previewRedPacket ?: previewRedPacket,
        previewForward = patch.previewForward ?: previewForward,
        previewMoneyTransfer = patch.previewMoneyTransfer ?: previewMoneyTransfer,
        previewSystemFallback = patch.previewSystemFallback ?: previewSystemFallback,
        previewUnknown = patch.previewUnknown ?: previewUnknown,
        previewRecalled = patch.previewRecalled ?: previewRecalled,
        sendSmsCodeFailed = patch.sendSmsCodeFailed ?: sendSmsCodeFailed,
        connectServerFailed = patch.connectServerFailed ?: connectServerFailed,
        sdkInitFailed = patch.sdkInitFailed ?: sdkInitFailed,
        switchAccountFailed = patch.switchAccountFailed ?: switchAccountFailed,
        logoutFailed = patch.logoutFailed ?: logoutFailed,
        loginFailed = patch.loginFailed ?: loginFailed,
        friendRequestAcceptFailed = patch.friendRequestAcceptFailed ?: friendRequestAcceptFailed,
        friendRequestDeclineFailed = patch.friendRequestDeclineFailed ?: friendRequestDeclineFailed,
        messageRecallFailed = patch.messageRecallFailed ?: messageRecallFailed,
        qrImageDecodeFailed = patch.qrImageDecodeFailed ?: qrImageDecodeFailed,
        qrScanAimHint = patch.qrScanAimHint ?: qrScanAimHint,
        qrScanPickFromAlbum = patch.qrScanPickFromAlbum ?: qrScanPickFromAlbum,
        qrScanNoQrInImage = patch.qrScanNoQrInImage ?: qrScanNoQrInImage,
        qrScanNotPrivChat = patch.qrScanNotPrivChat ?: qrScanNotPrivChat,
        qrScanUnsupported = patch.qrScanUnsupported ?: qrScanUnsupported,
        qrScanSelf = patch.qrScanSelf ?: qrScanSelf,
        qrScanResolveFailed = patch.qrScanResolveFailed ?: qrScanResolveFailed,
        qrScanGroupJoined = patch.qrScanGroupJoined ?: qrScanGroupJoined,
        qrScanGroupPending = patch.qrScanGroupPending ?: qrScanGroupPending,
        saveFailed = patch.saveFailed ?: saveFailed,
        operationFailed = patch.operationFailed ?: operationFailed,
        smsCodeError = patch.smsCodeError ?: smsCodeError,
        loginErrInvalidCredentials = patch.loginErrInvalidCredentials ?: loginErrInvalidCredentials,
        loginErrAccountDisabled = patch.loginErrAccountDisabled ?: loginErrAccountDisabled,
        registerErrUsernameTaken = patch.registerErrUsernameTaken ?: registerErrUsernameTaken,
        registerErrUsernameInvalid = patch.registerErrUsernameInvalid ?: registerErrUsernameInvalid,
        registerErrUsernameFormat = patch.registerErrUsernameFormat ?: registerErrUsernameFormat,
        registerErrPasswordTooShort = patch.registerErrPasswordTooShort ?: registerErrPasswordTooShort,
        systemMessagesName = patch.systemMessagesName ?: systemMessagesName,
        groupChatFallback = patch.groupChatFallback ?: groupChatFallback,
        signInTitle = patch.signInTitle ?: signInTitle,
        signInButton = patch.signInButton ?: signInButton,
        signInDoneToday = patch.signInDoneToday ?: signInDoneToday,
        signInContinuousPrefix = patch.signInContinuousPrefix ?: signInContinuousPrefix,
        signInTodayReward = patch.signInTodayReward ?: signInTodayReward,
        signInRewardList = patch.signInRewardList ?: signInRewardList,
        signInPointsUnit = patch.signInPointsUnit ?: signInPointsUnit,
        signInDayUnit = patch.signInDayUnit ?: signInDayUnit,
        signInSuccessTitle = patch.signInSuccessTitle ?: signInSuccessTitle,
        signInCashCredited = patch.signInCashCredited ?: signInCashCredited,
        bindMobileSkip = patch.bindMobileSkip ?: bindMobileSkip,
        bindMobileTitle = patch.bindMobileTitle ?: bindMobileTitle,
        bindMobileHint = patch.bindMobileHint ?: bindMobileHint,
        bindMobilePlaceholder = patch.bindMobilePlaceholder ?: bindMobilePlaceholder,
        bindMobileButton = patch.bindMobileButton ?: bindMobileButton,
        bindMobileErrTaken = patch.bindMobileErrTaken ?: bindMobileErrTaken,
        bindMobileErrBound = patch.bindMobileErrBound ?: bindMobileErrBound,
        bindMobileErrInvalid = patch.bindMobileErrInvalid ?: bindMobileErrInvalid,
        inviteBindTitle = patch.inviteBindTitle ?: inviteBindTitle,
        inviteBindPlaceholder = patch.inviteBindPlaceholder ?: inviteBindPlaceholder,
        inviteBindButton = patch.inviteBindButton ?: inviteBindButton,
        inviteBoundCode = patch.inviteBoundCode ?: inviteBoundCode,
        inviteBoundInviter = patch.inviteBoundInviter ?: inviteBoundInviter,
        inviteBoundAt = patch.inviteBoundAt ?: inviteBoundAt,
        inviteBoundHint = patch.inviteBoundHint ?: inviteBoundHint,
        inviteBoundAutoFriend = patch.inviteBoundAutoFriend ?: inviteBoundAutoFriend,
        inviteBindHint = patch.inviteBindHint ?: inviteBindHint,
        inviteBindSuccessFriend = patch.inviteBindSuccessFriend ?: inviteBindSuccessFriend,
        inviteBindSuccess = patch.inviteBindSuccess ?: inviteBindSuccess,
        inviteErrInvalid = patch.inviteErrInvalid ?: inviteErrInvalid,
        loginTabSms = patch.loginTabSms ?: loginTabSms,
        loginTabPassword = patch.loginTabPassword ?: loginTabPassword,
        loginUsernamePlaceholder = patch.loginUsernamePlaceholder ?: loginUsernamePlaceholder,
        loginPasswordPlaceholder = patch.loginPasswordPlaceholder ?: loginPasswordPlaceholder,
        loginPasswordNewPlaceholder = patch.loginPasswordNewPlaceholder ?: loginPasswordNewPlaceholder,
        loginNicknamePlaceholder = patch.loginNicknamePlaceholder ?: loginNicknamePlaceholder,
        loginInviteCodePlaceholder = patch.loginInviteCodePlaceholder ?: loginInviteCodePlaceholder,
        loginRegisterButton = patch.loginRegisterButton ?: loginRegisterButton,
        loginToRegister = patch.loginToRegister ?: loginToRegister,
        loginToLogin = patch.loginToLogin ?: loginToLogin,
        bannerConnecting = patch.bannerConnecting ?: bannerConnecting,
        bannerDisconnected = patch.bannerDisconnected ?: bannerDisconnected,
        bannerConnected = patch.bannerConnected ?: bannerConnected,
        bannerReconnecting = patch.bannerReconnecting ?: bannerReconnecting,
        bannerConnectFailed = patch.bannerConnectFailed ?: bannerConnectFailed,
        bannerSyncing = patch.bannerSyncing ?: bannerSyncing,
        loginExpired = patch.loginExpired ?: loginExpired,
        syncFailedRetry = patch.syncFailedRetry ?: syncFailedRetry,
        bannerServerBusy = patch.bannerServerBusy ?: bannerServerBusy,
        startupInitializing = patch.startupInitializing ?: startupInitializing,
        startupVerifyingAccount = patch.startupVerifyingAccount ?: startupVerifyingAccount,
        startupConnecting = patch.startupConnecting ?: startupConnecting,
        startupLoadingLocal = patch.startupLoadingLocal ?: startupLoadingLocal,
        startupSyncing = patch.startupSyncing ?: startupSyncing,
        syncStageTemplate = patch.syncStageTemplate ?: syncStageTemplate,
        syncStageContacts = patch.syncStageContacts ?: syncStageContacts,
        syncStageGroups = patch.syncStageGroups ?: syncStageGroups,
        syncStageConversations = patch.syncStageConversations ?: syncStageConversations,
        syncStageProfiles = patch.syncStageProfiles ?: syncStageProfiles,
        syncStageReadState = patch.syncStageReadState ?: syncStageReadState,
        syncStagePrivacy = patch.syncStagePrivacy ?: syncStagePrivacy,
        dateMonthDayPattern = patch.dateMonthDayPattern ?: dateMonthDayPattern,
        dateYearMonthDayPattern = patch.dateYearMonthDayPattern ?: dateYearMonthDayPattern,
        dateYearMonthPattern = patch.dateYearMonthPattern ?: dateYearMonthPattern,
        dateTimePattern = patch.dateTimePattern ?: dateTimePattern,
        relativeJustNow = patch.relativeJustNow ?: relativeJustNow,
        relativeMinutesAgo = patch.relativeMinutesAgo ?: relativeMinutesAgo,
        relativeHoursAgo = patch.relativeHoursAgo ?: relativeHoursAgo,
        relativeDaysAgo = patch.relativeDaysAgo ?: relativeDaysAgo,
    )
}

/**
 * 登录 / 注册 / 启动流程 / 扫码登录。单独成域：Message 与 Auxiliary 已经逼近 data class 构造器的 254 参数上限。
 */
@Immutable
data class PrivChatAuthStrings(
    val authTagline: String,
    val authUsernamePlaceholder: String,
    val authPasswordPlaceholder: String,
    val authUsernameRequired: String,
    val authPasswordRequired: String,
    val authLogin: String,
    val authRegister: String,
    val authHaveAccount: String,
    val authNoAccount: String,
    val authGoLogin: String,
    val authGoRegister: String,
    val authPhoneTitle: String,
    val authPhoneHint: String,
    val authPhonePlaceholder: String,
    val authPhoneInvalid: String,
    val authContinue: String,
    val authChangePhone: String,
    /** 占位符=手机号。 */
    val authCodeSentTo: String,
    /** 占位符=剩余秒数。 */
    val authResendCountdown: String,
    val authResendCode: String,
    val authAccountServerUnreachable: String,
    val startupLoggingIn: String,
    val startupRegistering: String,
    val startupDataInit: String,
    val startupInitializingSdk: String,
    val startupConnectingServer: String,
    val startupPreparingAccount: String,
    val sessionExpiredGeneric: String,
    val sessionCredentialInvalid: String,
    val sessionInsufficientPrivilege: String,
    val sessionStale: String,
    val sessionAccountBanned: String,
    val qrLoginTitle: String,
    /** 占位符=设备名。 */
    val qrLoginPrompt: String,
    /** 占位符=地点。 */
    val qrLoginLocation: String,
    val qrLoginGrantNote: String,
    val qrLoginReject: String,
    val qrLoginConfirmed: String,
    val qrLoginRejected: String,
    val qrLoginPlatformOnly: String,
    val qrScanUnsupportedPlatform: String,
    val qrCameraWaitingPermission: String,
    val qrCameraFailed: String,
    val qrCameraPermissionHint: String,
    val qrCameraReauthorize: String,
    val qrLoginNetworkError: String,
    val qrLoginNeedLogin: String,
    val qrLoginCodeExpired: String,
    val qrLoginCodeUsed: String,
    /** 占位符=错误码。 */
    val qrLoginFailedWithCode: String,
    val countryChinaMainland: String,
    val countryHongKong: String,
    val countryTaiwan: String,
    val countryUnitedStates: String,
    val countrySingapore: String,
    val countryMalaysia: String,
    val countryJapan: String,
    val countryKorea: String,
    val countryUnitedKingdom: String,
    val countryAustralia: String,
    val qrMyCodeHint: String,
    val qrGroupCodeHint: String,
    val qrSaveToAlbum: String,
    val qrSaving: String,
    val qrMyCodeTitle: String,
    val qrGroupCodeTitle: String,
    val qrGroupRefresh: String,
    val qrGroupRefreshing: String,
    val qrLoadUserFailed: String,
    val qrLoadMyCodeFailed: String,
    val qrLoadGroupCodeFailed: String,
    val qrEncodeFailed: String,
    val qrRenderFailed: String,
    val qrNotLoggedIn: String,
    /** 群名为空时的兜底称呼。 */
    val groupFallbackName: String,
    val mediaCompressingVideo: String,
    val mediaProcessingImage: String,
    val mediaProcessingVideo: String,
    val mediaProcessingFile: String,
    val micPermissionRequired: String,
    val voiceRecordFileMissing: String,
    val groupLeaveFailed: String,
    val conversationNotReady: String,
    val conversationOpenFailed: String,
    val conversationCreatedNotListed: String,
    val forwardDone: String,
    /** 占位符按序=成功数、失败数。 */
    val forwardPartial: String,
    val forwardFailed: String,
    val pushEnableTitle: String,
    val pushEnableMessage: String,
    val pushEnableFailedTitle: String,
    val pushEnableFailedMessage: String,
    val pushGoToSettings: String,
    val dialogGotIt: String,
    val sdkInitFailedShort: String,
)

data class PrivChatAuthStringsPatch(
    val authTagline: String? = null,
    val authUsernamePlaceholder: String? = null,
    val authPasswordPlaceholder: String? = null,
    val authUsernameRequired: String? = null,
    val authPasswordRequired: String? = null,
    val authLogin: String? = null,
    val authRegister: String? = null,
    val authHaveAccount: String? = null,
    val authNoAccount: String? = null,
    val authGoLogin: String? = null,
    val authGoRegister: String? = null,
    val authPhoneTitle: String? = null,
    val authPhoneHint: String? = null,
    val authPhonePlaceholder: String? = null,
    val authPhoneInvalid: String? = null,
    val authContinue: String? = null,
    val authChangePhone: String? = null,
    val authCodeSentTo: String? = null,
    val authResendCountdown: String? = null,
    val authResendCode: String? = null,
    val authAccountServerUnreachable: String? = null,
    val startupLoggingIn: String? = null,
    val startupRegistering: String? = null,
    val startupDataInit: String? = null,
    val startupInitializingSdk: String? = null,
    val startupConnectingServer: String? = null,
    val startupPreparingAccount: String? = null,
    val sessionExpiredGeneric: String? = null,
    val sessionCredentialInvalid: String? = null,
    val sessionInsufficientPrivilege: String? = null,
    val sessionStale: String? = null,
    val sessionAccountBanned: String? = null,
    val qrLoginTitle: String? = null,
    val qrLoginPrompt: String? = null,
    val qrLoginLocation: String? = null,
    val qrLoginGrantNote: String? = null,
    val qrLoginReject: String? = null,
    val qrLoginConfirmed: String? = null,
    val qrLoginRejected: String? = null,
    val qrLoginPlatformOnly: String? = null,
    val qrScanUnsupportedPlatform: String? = null,
    val qrCameraWaitingPermission: String? = null,
    val qrCameraFailed: String? = null,
    val qrCameraPermissionHint: String? = null,
    val qrCameraReauthorize: String? = null,
    val qrLoginNetworkError: String? = null,
    val qrLoginNeedLogin: String? = null,
    val qrLoginCodeExpired: String? = null,
    val qrLoginCodeUsed: String? = null,
    val qrLoginFailedWithCode: String? = null,
    val countryChinaMainland: String? = null,
    val countryHongKong: String? = null,
    val countryTaiwan: String? = null,
    val countryUnitedStates: String? = null,
    val countrySingapore: String? = null,
    val countryMalaysia: String? = null,
    val countryJapan: String? = null,
    val countryKorea: String? = null,
    val countryUnitedKingdom: String? = null,
    val countryAustralia: String? = null,
    val qrMyCodeHint: String? = null,
    val qrGroupCodeHint: String? = null,
    val qrSaveToAlbum: String? = null,
    val qrSaving: String? = null,
    val qrMyCodeTitle: String? = null,
    val qrGroupCodeTitle: String? = null,
    val qrGroupRefresh: String? = null,
    val qrGroupRefreshing: String? = null,
    val qrLoadUserFailed: String? = null,
    val qrLoadMyCodeFailed: String? = null,
    val qrLoadGroupCodeFailed: String? = null,
    val qrEncodeFailed: String? = null,
    val qrRenderFailed: String? = null,
    val qrNotLoggedIn: String? = null,
    val groupFallbackName: String? = null,
    val mediaCompressingVideo: String? = null,
    val mediaProcessingImage: String? = null,
    val mediaProcessingVideo: String? = null,
    val mediaProcessingFile: String? = null,
    val micPermissionRequired: String? = null,
    val voiceRecordFileMissing: String? = null,
    val groupLeaveFailed: String? = null,
    val conversationNotReady: String? = null,
    val conversationOpenFailed: String? = null,
    val conversationCreatedNotListed: String? = null,
    val forwardDone: String? = null,
    val forwardPartial: String? = null,
    val forwardFailed: String? = null,
    val pushEnableTitle: String? = null,
    val pushEnableMessage: String? = null,
    val pushEnableFailedTitle: String? = null,
    val pushEnableFailedMessage: String? = null,
    val pushGoToSettings: String? = null,
    val dialogGotIt: String? = null,
    val sdkInitFailedShort: String? = null,
) {
    companion object
}

val PrivChatAuthStringsPatch.isEmpty: Boolean
    get() = authTagline == null &&
        authUsernamePlaceholder == null &&
        authPasswordPlaceholder == null &&
        authUsernameRequired == null &&
        authPasswordRequired == null &&
        authLogin == null &&
        authRegister == null &&
        authHaveAccount == null &&
        authNoAccount == null &&
        authGoLogin == null &&
        authGoRegister == null &&
        authPhoneTitle == null &&
        authPhoneHint == null &&
        authPhonePlaceholder == null &&
        authPhoneInvalid == null &&
        authContinue == null &&
        authChangePhone == null &&
        authCodeSentTo == null &&
        authResendCountdown == null &&
        authResendCode == null &&
        authAccountServerUnreachable == null &&
        startupLoggingIn == null &&
        startupRegistering == null &&
        startupDataInit == null &&
        startupInitializingSdk == null &&
        startupConnectingServer == null &&
        startupPreparingAccount == null &&
        sessionExpiredGeneric == null &&
        sessionCredentialInvalid == null &&
        sessionInsufficientPrivilege == null &&
        sessionStale == null &&
        sessionAccountBanned == null &&
        qrLoginTitle == null &&
        qrLoginPrompt == null &&
        qrLoginLocation == null &&
        qrLoginGrantNote == null &&
        qrLoginReject == null &&
        qrLoginConfirmed == null &&
        qrLoginRejected == null &&
        qrLoginPlatformOnly == null &&
        qrScanUnsupportedPlatform == null &&
        qrCameraWaitingPermission == null &&
        qrCameraFailed == null &&
        qrCameraPermissionHint == null &&
        qrCameraReauthorize == null &&
        qrLoginNetworkError == null &&
        qrLoginNeedLogin == null &&
        qrLoginCodeExpired == null &&
        qrLoginCodeUsed == null &&
        qrLoginFailedWithCode == null &&
        countryChinaMainland == null &&
        countryHongKong == null &&
        countryTaiwan == null &&
        countryUnitedStates == null &&
        countrySingapore == null &&
        countryMalaysia == null &&
        countryJapan == null &&
        countryKorea == null &&
        countryUnitedKingdom == null &&
        countryAustralia == null &&
        qrMyCodeHint == null &&
        qrGroupCodeHint == null &&
        qrSaveToAlbum == null &&
        qrSaving == null &&
        qrMyCodeTitle == null &&
        qrGroupCodeTitle == null &&
        qrGroupRefresh == null &&
        qrGroupRefreshing == null &&
        qrLoadUserFailed == null &&
        qrLoadMyCodeFailed == null &&
        qrLoadGroupCodeFailed == null &&
        qrEncodeFailed == null &&
        qrRenderFailed == null &&
        qrNotLoggedIn == null &&
        groupFallbackName == null &&
        mediaCompressingVideo == null &&
        mediaProcessingImage == null &&
        mediaProcessingVideo == null &&
        mediaProcessingFile == null &&
        micPermissionRequired == null &&
        voiceRecordFileMissing == null &&
        groupLeaveFailed == null &&
        conversationNotReady == null &&
        conversationOpenFailed == null &&
        conversationCreatedNotListed == null &&
        forwardDone == null &&
        forwardPartial == null &&
        forwardFailed == null &&
        pushEnableTitle == null &&
        pushEnableMessage == null &&
        pushEnableFailedTitle == null &&
        pushEnableFailedMessage == null &&
        pushGoToSettings == null &&
        dialogGotIt == null &&
        sdkInitFailedShort == null

fun PrivChatAuthStrings.merge(patch: PrivChatAuthStringsPatch?): PrivChatAuthStrings {
    if (patch == null || patch.isEmpty) return this
    return copy(
        authTagline = patch.authTagline ?: authTagline,
        authUsernamePlaceholder = patch.authUsernamePlaceholder ?: authUsernamePlaceholder,
        authPasswordPlaceholder = patch.authPasswordPlaceholder ?: authPasswordPlaceholder,
        authUsernameRequired = patch.authUsernameRequired ?: authUsernameRequired,
        authPasswordRequired = patch.authPasswordRequired ?: authPasswordRequired,
        authLogin = patch.authLogin ?: authLogin,
        authRegister = patch.authRegister ?: authRegister,
        authHaveAccount = patch.authHaveAccount ?: authHaveAccount,
        authNoAccount = patch.authNoAccount ?: authNoAccount,
        authGoLogin = patch.authGoLogin ?: authGoLogin,
        authGoRegister = patch.authGoRegister ?: authGoRegister,
        authPhoneTitle = patch.authPhoneTitle ?: authPhoneTitle,
        authPhoneHint = patch.authPhoneHint ?: authPhoneHint,
        authPhonePlaceholder = patch.authPhonePlaceholder ?: authPhonePlaceholder,
        authPhoneInvalid = patch.authPhoneInvalid ?: authPhoneInvalid,
        authContinue = patch.authContinue ?: authContinue,
        authChangePhone = patch.authChangePhone ?: authChangePhone,
        authCodeSentTo = patch.authCodeSentTo ?: authCodeSentTo,
        authResendCountdown = patch.authResendCountdown ?: authResendCountdown,
        authResendCode = patch.authResendCode ?: authResendCode,
        authAccountServerUnreachable = patch.authAccountServerUnreachable ?: authAccountServerUnreachable,
        startupLoggingIn = patch.startupLoggingIn ?: startupLoggingIn,
        startupRegistering = patch.startupRegistering ?: startupRegistering,
        startupDataInit = patch.startupDataInit ?: startupDataInit,
        startupInitializingSdk = patch.startupInitializingSdk ?: startupInitializingSdk,
        startupConnectingServer = patch.startupConnectingServer ?: startupConnectingServer,
        startupPreparingAccount = patch.startupPreparingAccount ?: startupPreparingAccount,
        sessionExpiredGeneric = patch.sessionExpiredGeneric ?: sessionExpiredGeneric,
        sessionCredentialInvalid = patch.sessionCredentialInvalid ?: sessionCredentialInvalid,
        sessionInsufficientPrivilege = patch.sessionInsufficientPrivilege ?: sessionInsufficientPrivilege,
        sessionStale = patch.sessionStale ?: sessionStale,
        sessionAccountBanned = patch.sessionAccountBanned ?: sessionAccountBanned,
        qrLoginTitle = patch.qrLoginTitle ?: qrLoginTitle,
        qrLoginPrompt = patch.qrLoginPrompt ?: qrLoginPrompt,
        qrLoginLocation = patch.qrLoginLocation ?: qrLoginLocation,
        qrLoginGrantNote = patch.qrLoginGrantNote ?: qrLoginGrantNote,
        qrLoginReject = patch.qrLoginReject ?: qrLoginReject,
        qrLoginConfirmed = patch.qrLoginConfirmed ?: qrLoginConfirmed,
        qrLoginRejected = patch.qrLoginRejected ?: qrLoginRejected,
        qrLoginPlatformOnly = patch.qrLoginPlatformOnly ?: qrLoginPlatformOnly,
        qrScanUnsupportedPlatform = patch.qrScanUnsupportedPlatform ?: qrScanUnsupportedPlatform,
        qrCameraWaitingPermission = patch.qrCameraWaitingPermission ?: qrCameraWaitingPermission,
        qrCameraFailed = patch.qrCameraFailed ?: qrCameraFailed,
        qrCameraPermissionHint = patch.qrCameraPermissionHint ?: qrCameraPermissionHint,
        qrCameraReauthorize = patch.qrCameraReauthorize ?: qrCameraReauthorize,
        qrLoginNetworkError = patch.qrLoginNetworkError ?: qrLoginNetworkError,
        qrLoginNeedLogin = patch.qrLoginNeedLogin ?: qrLoginNeedLogin,
        qrLoginCodeExpired = patch.qrLoginCodeExpired ?: qrLoginCodeExpired,
        qrLoginCodeUsed = patch.qrLoginCodeUsed ?: qrLoginCodeUsed,
        qrLoginFailedWithCode = patch.qrLoginFailedWithCode ?: qrLoginFailedWithCode,
        countryChinaMainland = patch.countryChinaMainland ?: countryChinaMainland,
        countryHongKong = patch.countryHongKong ?: countryHongKong,
        countryTaiwan = patch.countryTaiwan ?: countryTaiwan,
        countryUnitedStates = patch.countryUnitedStates ?: countryUnitedStates,
        countrySingapore = patch.countrySingapore ?: countrySingapore,
        countryMalaysia = patch.countryMalaysia ?: countryMalaysia,
        countryJapan = patch.countryJapan ?: countryJapan,
        countryKorea = patch.countryKorea ?: countryKorea,
        countryUnitedKingdom = patch.countryUnitedKingdom ?: countryUnitedKingdom,
        countryAustralia = patch.countryAustralia ?: countryAustralia,
        qrMyCodeHint = patch.qrMyCodeHint ?: qrMyCodeHint,
        qrGroupCodeHint = patch.qrGroupCodeHint ?: qrGroupCodeHint,
        qrSaveToAlbum = patch.qrSaveToAlbum ?: qrSaveToAlbum,
        qrSaving = patch.qrSaving ?: qrSaving,
        qrMyCodeTitle = patch.qrMyCodeTitle ?: qrMyCodeTitle,
        qrGroupCodeTitle = patch.qrGroupCodeTitle ?: qrGroupCodeTitle,
        qrGroupRefresh = patch.qrGroupRefresh ?: qrGroupRefresh,
        qrGroupRefreshing = patch.qrGroupRefreshing ?: qrGroupRefreshing,
        qrLoadUserFailed = patch.qrLoadUserFailed ?: qrLoadUserFailed,
        qrLoadMyCodeFailed = patch.qrLoadMyCodeFailed ?: qrLoadMyCodeFailed,
        qrLoadGroupCodeFailed = patch.qrLoadGroupCodeFailed ?: qrLoadGroupCodeFailed,
        qrEncodeFailed = patch.qrEncodeFailed ?: qrEncodeFailed,
        qrRenderFailed = patch.qrRenderFailed ?: qrRenderFailed,
        qrNotLoggedIn = patch.qrNotLoggedIn ?: qrNotLoggedIn,
        groupFallbackName = patch.groupFallbackName ?: groupFallbackName,
        mediaCompressingVideo = patch.mediaCompressingVideo ?: mediaCompressingVideo,
        mediaProcessingImage = patch.mediaProcessingImage ?: mediaProcessingImage,
        mediaProcessingVideo = patch.mediaProcessingVideo ?: mediaProcessingVideo,
        mediaProcessingFile = patch.mediaProcessingFile ?: mediaProcessingFile,
        micPermissionRequired = patch.micPermissionRequired ?: micPermissionRequired,
        voiceRecordFileMissing = patch.voiceRecordFileMissing ?: voiceRecordFileMissing,
        groupLeaveFailed = patch.groupLeaveFailed ?: groupLeaveFailed,
        conversationNotReady = patch.conversationNotReady ?: conversationNotReady,
        conversationOpenFailed = patch.conversationOpenFailed ?: conversationOpenFailed,
        conversationCreatedNotListed = patch.conversationCreatedNotListed ?: conversationCreatedNotListed,
        forwardDone = patch.forwardDone ?: forwardDone,
        forwardPartial = patch.forwardPartial ?: forwardPartial,
        forwardFailed = patch.forwardFailed ?: forwardFailed,
        pushEnableTitle = patch.pushEnableTitle ?: pushEnableTitle,
        pushEnableMessage = patch.pushEnableMessage ?: pushEnableMessage,
        pushEnableFailedTitle = patch.pushEnableFailedTitle ?: pushEnableFailedTitle,
        pushEnableFailedMessage = patch.pushEnableFailedMessage ?: pushEnableFailedMessage,
        pushGoToSettings = patch.pushGoToSettings ?: pushGoToSettings,
        dialogGotIt = patch.dialogGotIt ?: dialogGotIt,
        sdkInitFailedShort = patch.sdkInitFailedShort ?: sdkInitFailedShort,
    )
}

/**
 * 钱包 / 提现 / 银行卡 / 红包 / 转账 / 冻结。单独成域，避免把已有域顶到 data class 构造器的 254 参数上限。
 */
@Immutable
data class PrivChatWalletStrings(
    val walletTitle: String,
    val walletBalance: String,
    val walletUnsupportedMode: String,
    /** 占位符=被冻结金额（元）。 */
    val walletFrozenBanner: String,
    val walletTransactions: String,
    val walletBankCards: String,
    val walletApplyWithdraw: String,
    val walletBindCard: String,
    val walletNoCards: String,
    val walletCardHolder: String,
    val walletCardBank: String,
    val walletCardNumber: String,
    val walletSubmit: String,
    /** 占位符=可用余额。 */
    val walletAvailableBalance: String,
    val walletWithdrawAmount: String,
    val walletBindCardFirst: String,
    val walletCardSelected: String,
    val walletSubmitWithdraw: String,
    val walletWithdrawHistory: String,
    val walletWithdrawRecords: String,
    val walletNoWithdrawRecords: String,
    val walletWithdrawDetail: String,
    val walletFieldWithdrawAmount: String,
    val walletFieldFee: String,
    val walletFieldActualAmount: String,
    val walletFieldStatus: String,
    val walletFieldAppliedAt: String,
    val walletFieldPaidAt: String,
    val walletFieldHoldReason: String,
    val walletFieldRejectReason: String,
    val walletNoTransactions: String,
    /** 占位符=交易后余额。 */
    val walletBalanceAfter: String,
    val withdrawStatusPendingReview: String,
    val withdrawStatusPendingPayout: String,
    val withdrawStatusProcessing: String,
    val withdrawStatusPaid: String,
    val withdrawStatusRejected: String,
    val withdrawStatusFailed: String,
    val withdrawStatusCancelled: String,
    val withdrawStatusOnHold: String,
    val walletStatusUnknown: String,
    val bizTypeRecharge: String,
    val bizTypeRechargeRefund: String,
    val bizTypeSystemAdjust: String,
    val bizTypeWithdrawFreeze: String,
    val bizTypeWithdrawUnfreeze: String,
    val bizTypeWithdrawDebit: String,
    val bizTypeWithdrawRefund: String,
    val bizTypeRedPacketSend: String,
    val bizTypeRedPacketClaim: String,
    val bizTypeRedPacketRefund: String,
    val bizTypeTransferOut: String,
    val bizTypeTransferIn: String,
    val bizTypeTransferRefund: String,
    val bizTypeSignInReward: String,
    val bizTypeOther: String,
    val walletErrSessionExpired: String,
    val walletErrBadRequest: String,
    val walletErrNotFound: String,
    val walletErrConflict: String,
    val walletErrLoadFailed: String,
    val walletErrActionFailed: String,
    val redPacketSendTitle: String,
    val redPacketSentTitle: String,
    val redPacketAcceptedTitle: String,
    val redPacketSentHint: String,
    val redPacketAcceptedHint: String,
    val redPacketLuckyMode: String,
    val redPacketNormalMode: String,
    val redPacketLuckyHint: String,
    val redPacketNormalHint: String,
    val redPacketTotalAmount: String,
    val redPacketAmount: String,
    val redPacketCount: String,
    val redPacketGreetingPlaceholder: String,
    val redPacketStuff: String,
    val redPacketErrAmount: String,
    val redPacketErrCount: String,
    val redPacketErrDmSingle: String,
    val redPacketErrTooSmall: String,
    val walletViewDetail: String,
    val walletDone: String,
    val transferSendTitle: String,
    val transferDmOnly: String,
    val transferCompletedTitle: String,
    val transferAcceptedTitle: String,
    val transferCompletedHint: String,
    val transferAcceptedHint: String,
    /** 占位符=收款人。 */
    val transferConfirmTo: String,
    /** 占位符=备注内容。 */
    val transferRemarkPrefix: String,
    val transferConfirmAction: String,
    val transferAmountPlaceholder: String,
    val transferRemarkPlaceholder: String,
    val transferErrAmount: String,
    val transferDetailTitle: String,
    val transferFieldAmount: String,
    val transferFieldSender: String,
    val transferFieldReceiver: String,
    val transferFieldRemark: String,
    val transferFieldTime: String,
    val transferFieldOrderId: String,
    val transferStateRefunded: String,
    val transferStateSucceeded: String,
    val redPacketDetailTitle: String,
    val redPacketSenderSelf: String,
    /** 没有昵称时的兜底称呼，占位符=uid。 */
    val walletUserFallback: String,
    val redPacketFromSelf: String,
    /** 占位符=发送者。 */
    val redPacketFromPeer: String,
    val redPacketDefaultGreeting: String,
    val redPacketCreditedToWallet: String,
    val redPacketOpen: String,
    /** 占位符按序=已领个数、总个数、已领金额、总金额。 */
    val redPacketClaimProgress: String,
    /** 占位符按序=红包个数、总金额；后面直接拼状态文案。 */
    val redPacketSummary: String,
    val redPacketBestLuck: String,
    /** 占位符=退回金额。 */
    val redPacketStateRefundedToYou: String,
    val redPacketStateAllClaimed: String,
    val redPacketStateExpired: String,
    val redPacketStateWaiting: String,
    val redPacketStateClaimedByMe: String,
    val redPacketStateDrained: String,
    val redPacketStateClaimable: String,
    val redPacketCoverFallbackName: String,
    val redPacketCoverPeerFallback: String,
    /** 占位符=发送者。 */
    val redPacketCoverFrom: String,
    val redPacketOpenShort: String,
    val redPacketViewDetail: String,
    val freezeTypeWithdraw: String,
    val freezeTypeAudit: String,
    val freezeTypeAccountFrozen: String,
    val freezeTypeOther: String,
    val freezeWithdrawStatusReviewing: String,
    val freezeWithdrawStatusReturned: String,
    val freezeWithdrawStatusPaid: String,
    val freezeStatusExpired: String,
    val freezeStatusHolding: String,
    val freezeStatusReleased: String,
    val freezeStatusDeducted: String,
    val freezeRecordsTitle: String,
    val freezeNoRecords: String,
    val freezeDetailTitle: String,
    val freezeFieldType: String,
    val freezeFieldWithdrawAmount: String,
    val freezeFieldFrozenAmount: String,
    val freezeFieldRelatedOrder: String,
    val freezeFieldNote: String,
    val freezeFieldReason: String,
    val previewLargeImage: String,
    val previewImageThumb: String,
    val previewVideoThumb: String,
    /** 占位符=进度百分比。 */
    val previewLoadingWithPercent: String,
    /** 占位符=进度百分比。 */
    val previewPausedWithPercent: String,
    val previewLoadFailedRetrying: String,
    /** 自动重试用尽后的终态：必须能点，且不能再说"正在重试"。 */
    val previewLoadFailedTapToRetry: String,
    val reportReasonSpam: String,
    val reportReasonHarassment: String,
    val reportReasonSexual: String,
    val reportReasonFraud: String,
    val reportReasonOther: String,
    val reportPickReason: String,
    val reportUnsentMessage: String,
    val reportSubmitted: String,
    val reportSubmitFailed: String,
    val accountCurrentlySignedIn: String,
    /** 占位符=账号副标题。 */
    val accountCurrentlySignedInWith: String,
    val desktopPickConversation: String,
    /** 推送/会话预览里的「有人@我」前缀，占位符=原标题。 */
    val previewMentionPrefix: String,
    /** 占位符=链接标题。 */
    val previewLinkWithTitle: String,
    /** 占位符=地点名。 */
    val previewLocationWithName: String,
    /** 占位符=uid。 */
    val previewContactWithUid: String,
    val connectingShort: String,
    val initializingShort: String,
    val avatarNotAnImage: String,
)

data class PrivChatWalletStringsPatch(
    val walletTitle: String? = null,
    val walletBalance: String? = null,
    val walletUnsupportedMode: String? = null,
    val walletFrozenBanner: String? = null,
    val walletTransactions: String? = null,
    val walletBankCards: String? = null,
    val walletApplyWithdraw: String? = null,
    val walletBindCard: String? = null,
    val walletNoCards: String? = null,
    val walletCardHolder: String? = null,
    val walletCardBank: String? = null,
    val walletCardNumber: String? = null,
    val walletSubmit: String? = null,
    val walletAvailableBalance: String? = null,
    val walletWithdrawAmount: String? = null,
    val walletBindCardFirst: String? = null,
    val walletCardSelected: String? = null,
    val walletSubmitWithdraw: String? = null,
    val walletWithdrawHistory: String? = null,
    val walletWithdrawRecords: String? = null,
    val walletNoWithdrawRecords: String? = null,
    val walletWithdrawDetail: String? = null,
    val walletFieldWithdrawAmount: String? = null,
    val walletFieldFee: String? = null,
    val walletFieldActualAmount: String? = null,
    val walletFieldStatus: String? = null,
    val walletFieldAppliedAt: String? = null,
    val walletFieldPaidAt: String? = null,
    val walletFieldHoldReason: String? = null,
    val walletFieldRejectReason: String? = null,
    val walletNoTransactions: String? = null,
    val walletBalanceAfter: String? = null,
    val withdrawStatusPendingReview: String? = null,
    val withdrawStatusPendingPayout: String? = null,
    val withdrawStatusProcessing: String? = null,
    val withdrawStatusPaid: String? = null,
    val withdrawStatusRejected: String? = null,
    val withdrawStatusFailed: String? = null,
    val withdrawStatusCancelled: String? = null,
    val withdrawStatusOnHold: String? = null,
    val walletStatusUnknown: String? = null,
    val bizTypeRecharge: String? = null,
    val bizTypeRechargeRefund: String? = null,
    val bizTypeSystemAdjust: String? = null,
    val bizTypeWithdrawFreeze: String? = null,
    val bizTypeWithdrawUnfreeze: String? = null,
    val bizTypeWithdrawDebit: String? = null,
    val bizTypeWithdrawRefund: String? = null,
    val bizTypeRedPacketSend: String? = null,
    val bizTypeRedPacketClaim: String? = null,
    val bizTypeRedPacketRefund: String? = null,
    val bizTypeTransferOut: String? = null,
    val bizTypeTransferIn: String? = null,
    val bizTypeTransferRefund: String? = null,
    val bizTypeSignInReward: String? = null,
    val bizTypeOther: String? = null,
    val walletErrSessionExpired: String? = null,
    val walletErrBadRequest: String? = null,
    val walletErrNotFound: String? = null,
    val walletErrConflict: String? = null,
    val walletErrLoadFailed: String? = null,
    val walletErrActionFailed: String? = null,
    val redPacketSendTitle: String? = null,
    val redPacketSentTitle: String? = null,
    val redPacketAcceptedTitle: String? = null,
    val redPacketSentHint: String? = null,
    val redPacketAcceptedHint: String? = null,
    val redPacketLuckyMode: String? = null,
    val redPacketNormalMode: String? = null,
    val redPacketLuckyHint: String? = null,
    val redPacketNormalHint: String? = null,
    val redPacketTotalAmount: String? = null,
    val redPacketAmount: String? = null,
    val redPacketCount: String? = null,
    val redPacketGreetingPlaceholder: String? = null,
    val redPacketStuff: String? = null,
    val redPacketErrAmount: String? = null,
    val redPacketErrCount: String? = null,
    val redPacketErrDmSingle: String? = null,
    val redPacketErrTooSmall: String? = null,
    val walletViewDetail: String? = null,
    val walletDone: String? = null,
    val transferSendTitle: String? = null,
    val transferDmOnly: String? = null,
    val transferCompletedTitle: String? = null,
    val transferAcceptedTitle: String? = null,
    val transferCompletedHint: String? = null,
    val transferAcceptedHint: String? = null,
    val transferConfirmTo: String? = null,
    val transferRemarkPrefix: String? = null,
    val transferConfirmAction: String? = null,
    val transferAmountPlaceholder: String? = null,
    val transferRemarkPlaceholder: String? = null,
    val transferErrAmount: String? = null,
    val transferDetailTitle: String? = null,
    val transferFieldAmount: String? = null,
    val transferFieldSender: String? = null,
    val transferFieldReceiver: String? = null,
    val transferFieldRemark: String? = null,
    val transferFieldTime: String? = null,
    val transferFieldOrderId: String? = null,
    val transferStateRefunded: String? = null,
    val transferStateSucceeded: String? = null,
    val redPacketDetailTitle: String? = null,
    val redPacketSenderSelf: String? = null,
    val walletUserFallback: String? = null,
    val redPacketFromSelf: String? = null,
    val redPacketFromPeer: String? = null,
    val redPacketDefaultGreeting: String? = null,
    val redPacketCreditedToWallet: String? = null,
    val redPacketOpen: String? = null,
    val redPacketClaimProgress: String? = null,
    val redPacketSummary: String? = null,
    val redPacketBestLuck: String? = null,
    val redPacketStateRefundedToYou: String? = null,
    val redPacketStateAllClaimed: String? = null,
    val redPacketStateExpired: String? = null,
    val redPacketStateWaiting: String? = null,
    val redPacketStateClaimedByMe: String? = null,
    val redPacketStateDrained: String? = null,
    val redPacketStateClaimable: String? = null,
    val redPacketCoverFallbackName: String? = null,
    val redPacketCoverPeerFallback: String? = null,
    val redPacketCoverFrom: String? = null,
    val redPacketOpenShort: String? = null,
    val redPacketViewDetail: String? = null,
    val freezeTypeWithdraw: String? = null,
    val freezeTypeAudit: String? = null,
    val freezeTypeAccountFrozen: String? = null,
    val freezeTypeOther: String? = null,
    val freezeWithdrawStatusReviewing: String? = null,
    val freezeWithdrawStatusReturned: String? = null,
    val freezeWithdrawStatusPaid: String? = null,
    val freezeStatusExpired: String? = null,
    val freezeStatusHolding: String? = null,
    val freezeStatusReleased: String? = null,
    val freezeStatusDeducted: String? = null,
    val freezeRecordsTitle: String? = null,
    val freezeNoRecords: String? = null,
    val freezeDetailTitle: String? = null,
    val freezeFieldType: String? = null,
    val freezeFieldWithdrawAmount: String? = null,
    val freezeFieldFrozenAmount: String? = null,
    val freezeFieldRelatedOrder: String? = null,
    val freezeFieldNote: String? = null,
    val freezeFieldReason: String? = null,
    val previewLargeImage: String? = null,
    val previewImageThumb: String? = null,
    val previewVideoThumb: String? = null,
    val previewLoadingWithPercent: String? = null,
    val previewPausedWithPercent: String? = null,
    val previewLoadFailedRetrying: String? = null,
    val previewLoadFailedTapToRetry: String? = null,
    val reportReasonSpam: String? = null,
    val reportReasonHarassment: String? = null,
    val reportReasonSexual: String? = null,
    val reportReasonFraud: String? = null,
    val reportReasonOther: String? = null,
    val reportPickReason: String? = null,
    val reportUnsentMessage: String? = null,
    val reportSubmitted: String? = null,
    val reportSubmitFailed: String? = null,
    val accountCurrentlySignedIn: String? = null,
    val accountCurrentlySignedInWith: String? = null,
    val desktopPickConversation: String? = null,
    val previewMentionPrefix: String? = null,
    val previewLinkWithTitle: String? = null,
    val previewLocationWithName: String? = null,
    val previewContactWithUid: String? = null,
    val connectingShort: String? = null,
    val initializingShort: String? = null,
    val avatarNotAnImage: String? = null,
) {
    companion object
}

val PrivChatWalletStringsPatch.isEmpty: Boolean
    get() = walletTitle == null &&
        walletBalance == null &&
        walletUnsupportedMode == null &&
        walletFrozenBanner == null &&
        walletTransactions == null &&
        walletBankCards == null &&
        walletApplyWithdraw == null &&
        walletBindCard == null &&
        walletNoCards == null &&
        walletCardHolder == null &&
        walletCardBank == null &&
        walletCardNumber == null &&
        walletSubmit == null &&
        walletAvailableBalance == null &&
        walletWithdrawAmount == null &&
        walletBindCardFirst == null &&
        walletCardSelected == null &&
        walletSubmitWithdraw == null &&
        walletWithdrawHistory == null &&
        walletWithdrawRecords == null &&
        walletNoWithdrawRecords == null &&
        walletWithdrawDetail == null &&
        walletFieldWithdrawAmount == null &&
        walletFieldFee == null &&
        walletFieldActualAmount == null &&
        walletFieldStatus == null &&
        walletFieldAppliedAt == null &&
        walletFieldPaidAt == null &&
        walletFieldHoldReason == null &&
        walletFieldRejectReason == null &&
        walletNoTransactions == null &&
        walletBalanceAfter == null &&
        withdrawStatusPendingReview == null &&
        withdrawStatusPendingPayout == null &&
        withdrawStatusProcessing == null &&
        withdrawStatusPaid == null &&
        withdrawStatusRejected == null &&
        withdrawStatusFailed == null &&
        withdrawStatusCancelled == null &&
        withdrawStatusOnHold == null &&
        walletStatusUnknown == null &&
        bizTypeRecharge == null &&
        bizTypeRechargeRefund == null &&
        bizTypeSystemAdjust == null &&
        bizTypeWithdrawFreeze == null &&
        bizTypeWithdrawUnfreeze == null &&
        bizTypeWithdrawDebit == null &&
        bizTypeWithdrawRefund == null &&
        bizTypeRedPacketSend == null &&
        bizTypeRedPacketClaim == null &&
        bizTypeRedPacketRefund == null &&
        bizTypeTransferOut == null &&
        bizTypeTransferIn == null &&
        bizTypeTransferRefund == null &&
        bizTypeSignInReward == null &&
        bizTypeOther == null &&
        walletErrSessionExpired == null &&
        walletErrBadRequest == null &&
        walletErrNotFound == null &&
        walletErrConflict == null &&
        walletErrLoadFailed == null &&
        walletErrActionFailed == null &&
        redPacketSendTitle == null &&
        redPacketSentTitle == null &&
        redPacketAcceptedTitle == null &&
        redPacketSentHint == null &&
        redPacketAcceptedHint == null &&
        redPacketLuckyMode == null &&
        redPacketNormalMode == null &&
        redPacketLuckyHint == null &&
        redPacketNormalHint == null &&
        redPacketTotalAmount == null &&
        redPacketAmount == null &&
        redPacketCount == null &&
        redPacketGreetingPlaceholder == null &&
        redPacketStuff == null &&
        redPacketErrAmount == null &&
        redPacketErrCount == null &&
        redPacketErrDmSingle == null &&
        redPacketErrTooSmall == null &&
        walletViewDetail == null &&
        walletDone == null &&
        transferSendTitle == null &&
        transferDmOnly == null &&
        transferCompletedTitle == null &&
        transferAcceptedTitle == null &&
        transferCompletedHint == null &&
        transferAcceptedHint == null &&
        transferConfirmTo == null &&
        transferRemarkPrefix == null &&
        transferConfirmAction == null &&
        transferAmountPlaceholder == null &&
        transferRemarkPlaceholder == null &&
        transferErrAmount == null &&
        transferDetailTitle == null &&
        transferFieldAmount == null &&
        transferFieldSender == null &&
        transferFieldReceiver == null &&
        transferFieldRemark == null &&
        transferFieldTime == null &&
        transferFieldOrderId == null &&
        transferStateRefunded == null &&
        transferStateSucceeded == null &&
        redPacketDetailTitle == null &&
        redPacketSenderSelf == null &&
        walletUserFallback == null &&
        redPacketFromSelf == null &&
        redPacketFromPeer == null &&
        redPacketDefaultGreeting == null &&
        redPacketCreditedToWallet == null &&
        redPacketOpen == null &&
        redPacketClaimProgress == null &&
        redPacketSummary == null &&
        redPacketBestLuck == null &&
        redPacketStateRefundedToYou == null &&
        redPacketStateAllClaimed == null &&
        redPacketStateExpired == null &&
        redPacketStateWaiting == null &&
        redPacketStateClaimedByMe == null &&
        redPacketStateDrained == null &&
        redPacketStateClaimable == null &&
        redPacketCoverFallbackName == null &&
        redPacketCoverPeerFallback == null &&
        redPacketCoverFrom == null &&
        redPacketOpenShort == null &&
        redPacketViewDetail == null &&
        freezeTypeWithdraw == null &&
        freezeTypeAudit == null &&
        freezeTypeAccountFrozen == null &&
        freezeTypeOther == null &&
        freezeWithdrawStatusReviewing == null &&
        freezeWithdrawStatusReturned == null &&
        freezeWithdrawStatusPaid == null &&
        freezeStatusExpired == null &&
        freezeStatusHolding == null &&
        freezeStatusReleased == null &&
        freezeStatusDeducted == null &&
        freezeRecordsTitle == null &&
        freezeNoRecords == null &&
        freezeDetailTitle == null &&
        freezeFieldType == null &&
        freezeFieldWithdrawAmount == null &&
        freezeFieldFrozenAmount == null &&
        freezeFieldRelatedOrder == null &&
        freezeFieldNote == null &&
        freezeFieldReason == null &&
        previewLargeImage == null &&
        previewImageThumb == null &&
        previewVideoThumb == null &&
        previewLoadingWithPercent == null &&
        previewPausedWithPercent == null &&
        previewLoadFailedRetrying == null &&
        previewLoadFailedTapToRetry == null &&
        reportReasonSpam == null &&
        reportReasonHarassment == null &&
        reportReasonSexual == null &&
        reportReasonFraud == null &&
        reportReasonOther == null &&
        reportPickReason == null &&
        reportUnsentMessage == null &&
        reportSubmitted == null &&
        reportSubmitFailed == null &&
        accountCurrentlySignedIn == null &&
        accountCurrentlySignedInWith == null &&
        desktopPickConversation == null &&
        previewMentionPrefix == null &&
        previewLinkWithTitle == null &&
        previewLocationWithName == null &&
        previewContactWithUid == null &&
        connectingShort == null &&
        initializingShort == null &&
        avatarNotAnImage == null

fun PrivChatWalletStrings.merge(patch: PrivChatWalletStringsPatch?): PrivChatWalletStrings {
    if (patch == null || patch.isEmpty) return this
    return copy(
        walletTitle = patch.walletTitle ?: walletTitle,
        walletBalance = patch.walletBalance ?: walletBalance,
        walletUnsupportedMode = patch.walletUnsupportedMode ?: walletUnsupportedMode,
        walletFrozenBanner = patch.walletFrozenBanner ?: walletFrozenBanner,
        walletTransactions = patch.walletTransactions ?: walletTransactions,
        walletBankCards = patch.walletBankCards ?: walletBankCards,
        walletApplyWithdraw = patch.walletApplyWithdraw ?: walletApplyWithdraw,
        walletBindCard = patch.walletBindCard ?: walletBindCard,
        walletNoCards = patch.walletNoCards ?: walletNoCards,
        walletCardHolder = patch.walletCardHolder ?: walletCardHolder,
        walletCardBank = patch.walletCardBank ?: walletCardBank,
        walletCardNumber = patch.walletCardNumber ?: walletCardNumber,
        walletSubmit = patch.walletSubmit ?: walletSubmit,
        walletAvailableBalance = patch.walletAvailableBalance ?: walletAvailableBalance,
        walletWithdrawAmount = patch.walletWithdrawAmount ?: walletWithdrawAmount,
        walletBindCardFirst = patch.walletBindCardFirst ?: walletBindCardFirst,
        walletCardSelected = patch.walletCardSelected ?: walletCardSelected,
        walletSubmitWithdraw = patch.walletSubmitWithdraw ?: walletSubmitWithdraw,
        walletWithdrawHistory = patch.walletWithdrawHistory ?: walletWithdrawHistory,
        walletWithdrawRecords = patch.walletWithdrawRecords ?: walletWithdrawRecords,
        walletNoWithdrawRecords = patch.walletNoWithdrawRecords ?: walletNoWithdrawRecords,
        walletWithdrawDetail = patch.walletWithdrawDetail ?: walletWithdrawDetail,
        walletFieldWithdrawAmount = patch.walletFieldWithdrawAmount ?: walletFieldWithdrawAmount,
        walletFieldFee = patch.walletFieldFee ?: walletFieldFee,
        walletFieldActualAmount = patch.walletFieldActualAmount ?: walletFieldActualAmount,
        walletFieldStatus = patch.walletFieldStatus ?: walletFieldStatus,
        walletFieldAppliedAt = patch.walletFieldAppliedAt ?: walletFieldAppliedAt,
        walletFieldPaidAt = patch.walletFieldPaidAt ?: walletFieldPaidAt,
        walletFieldHoldReason = patch.walletFieldHoldReason ?: walletFieldHoldReason,
        walletFieldRejectReason = patch.walletFieldRejectReason ?: walletFieldRejectReason,
        walletNoTransactions = patch.walletNoTransactions ?: walletNoTransactions,
        walletBalanceAfter = patch.walletBalanceAfter ?: walletBalanceAfter,
        withdrawStatusPendingReview = patch.withdrawStatusPendingReview ?: withdrawStatusPendingReview,
        withdrawStatusPendingPayout = patch.withdrawStatusPendingPayout ?: withdrawStatusPendingPayout,
        withdrawStatusProcessing = patch.withdrawStatusProcessing ?: withdrawStatusProcessing,
        withdrawStatusPaid = patch.withdrawStatusPaid ?: withdrawStatusPaid,
        withdrawStatusRejected = patch.withdrawStatusRejected ?: withdrawStatusRejected,
        withdrawStatusFailed = patch.withdrawStatusFailed ?: withdrawStatusFailed,
        withdrawStatusCancelled = patch.withdrawStatusCancelled ?: withdrawStatusCancelled,
        withdrawStatusOnHold = patch.withdrawStatusOnHold ?: withdrawStatusOnHold,
        walletStatusUnknown = patch.walletStatusUnknown ?: walletStatusUnknown,
        bizTypeRecharge = patch.bizTypeRecharge ?: bizTypeRecharge,
        bizTypeRechargeRefund = patch.bizTypeRechargeRefund ?: bizTypeRechargeRefund,
        bizTypeSystemAdjust = patch.bizTypeSystemAdjust ?: bizTypeSystemAdjust,
        bizTypeWithdrawFreeze = patch.bizTypeWithdrawFreeze ?: bizTypeWithdrawFreeze,
        bizTypeWithdrawUnfreeze = patch.bizTypeWithdrawUnfreeze ?: bizTypeWithdrawUnfreeze,
        bizTypeWithdrawDebit = patch.bizTypeWithdrawDebit ?: bizTypeWithdrawDebit,
        bizTypeWithdrawRefund = patch.bizTypeWithdrawRefund ?: bizTypeWithdrawRefund,
        bizTypeRedPacketSend = patch.bizTypeRedPacketSend ?: bizTypeRedPacketSend,
        bizTypeRedPacketClaim = patch.bizTypeRedPacketClaim ?: bizTypeRedPacketClaim,
        bizTypeRedPacketRefund = patch.bizTypeRedPacketRefund ?: bizTypeRedPacketRefund,
        bizTypeTransferOut = patch.bizTypeTransferOut ?: bizTypeTransferOut,
        bizTypeTransferIn = patch.bizTypeTransferIn ?: bizTypeTransferIn,
        bizTypeTransferRefund = patch.bizTypeTransferRefund ?: bizTypeTransferRefund,
        bizTypeSignInReward = patch.bizTypeSignInReward ?: bizTypeSignInReward,
        bizTypeOther = patch.bizTypeOther ?: bizTypeOther,
        walletErrSessionExpired = patch.walletErrSessionExpired ?: walletErrSessionExpired,
        walletErrBadRequest = patch.walletErrBadRequest ?: walletErrBadRequest,
        walletErrNotFound = patch.walletErrNotFound ?: walletErrNotFound,
        walletErrConflict = patch.walletErrConflict ?: walletErrConflict,
        walletErrLoadFailed = patch.walletErrLoadFailed ?: walletErrLoadFailed,
        walletErrActionFailed = patch.walletErrActionFailed ?: walletErrActionFailed,
        redPacketSendTitle = patch.redPacketSendTitle ?: redPacketSendTitle,
        redPacketSentTitle = patch.redPacketSentTitle ?: redPacketSentTitle,
        redPacketAcceptedTitle = patch.redPacketAcceptedTitle ?: redPacketAcceptedTitle,
        redPacketSentHint = patch.redPacketSentHint ?: redPacketSentHint,
        redPacketAcceptedHint = patch.redPacketAcceptedHint ?: redPacketAcceptedHint,
        redPacketLuckyMode = patch.redPacketLuckyMode ?: redPacketLuckyMode,
        redPacketNormalMode = patch.redPacketNormalMode ?: redPacketNormalMode,
        redPacketLuckyHint = patch.redPacketLuckyHint ?: redPacketLuckyHint,
        redPacketNormalHint = patch.redPacketNormalHint ?: redPacketNormalHint,
        redPacketTotalAmount = patch.redPacketTotalAmount ?: redPacketTotalAmount,
        redPacketAmount = patch.redPacketAmount ?: redPacketAmount,
        redPacketCount = patch.redPacketCount ?: redPacketCount,
        redPacketGreetingPlaceholder = patch.redPacketGreetingPlaceholder ?: redPacketGreetingPlaceholder,
        redPacketStuff = patch.redPacketStuff ?: redPacketStuff,
        redPacketErrAmount = patch.redPacketErrAmount ?: redPacketErrAmount,
        redPacketErrCount = patch.redPacketErrCount ?: redPacketErrCount,
        redPacketErrDmSingle = patch.redPacketErrDmSingle ?: redPacketErrDmSingle,
        redPacketErrTooSmall = patch.redPacketErrTooSmall ?: redPacketErrTooSmall,
        walletViewDetail = patch.walletViewDetail ?: walletViewDetail,
        walletDone = patch.walletDone ?: walletDone,
        transferSendTitle = patch.transferSendTitle ?: transferSendTitle,
        transferDmOnly = patch.transferDmOnly ?: transferDmOnly,
        transferCompletedTitle = patch.transferCompletedTitle ?: transferCompletedTitle,
        transferAcceptedTitle = patch.transferAcceptedTitle ?: transferAcceptedTitle,
        transferCompletedHint = patch.transferCompletedHint ?: transferCompletedHint,
        transferAcceptedHint = patch.transferAcceptedHint ?: transferAcceptedHint,
        transferConfirmTo = patch.transferConfirmTo ?: transferConfirmTo,
        transferRemarkPrefix = patch.transferRemarkPrefix ?: transferRemarkPrefix,
        transferConfirmAction = patch.transferConfirmAction ?: transferConfirmAction,
        transferAmountPlaceholder = patch.transferAmountPlaceholder ?: transferAmountPlaceholder,
        transferRemarkPlaceholder = patch.transferRemarkPlaceholder ?: transferRemarkPlaceholder,
        transferErrAmount = patch.transferErrAmount ?: transferErrAmount,
        transferDetailTitle = patch.transferDetailTitle ?: transferDetailTitle,
        transferFieldAmount = patch.transferFieldAmount ?: transferFieldAmount,
        transferFieldSender = patch.transferFieldSender ?: transferFieldSender,
        transferFieldReceiver = patch.transferFieldReceiver ?: transferFieldReceiver,
        transferFieldRemark = patch.transferFieldRemark ?: transferFieldRemark,
        transferFieldTime = patch.transferFieldTime ?: transferFieldTime,
        transferFieldOrderId = patch.transferFieldOrderId ?: transferFieldOrderId,
        transferStateRefunded = patch.transferStateRefunded ?: transferStateRefunded,
        transferStateSucceeded = patch.transferStateSucceeded ?: transferStateSucceeded,
        redPacketDetailTitle = patch.redPacketDetailTitle ?: redPacketDetailTitle,
        redPacketSenderSelf = patch.redPacketSenderSelf ?: redPacketSenderSelf,
        walletUserFallback = patch.walletUserFallback ?: walletUserFallback,
        redPacketFromSelf = patch.redPacketFromSelf ?: redPacketFromSelf,
        redPacketFromPeer = patch.redPacketFromPeer ?: redPacketFromPeer,
        redPacketDefaultGreeting = patch.redPacketDefaultGreeting ?: redPacketDefaultGreeting,
        redPacketCreditedToWallet = patch.redPacketCreditedToWallet ?: redPacketCreditedToWallet,
        redPacketOpen = patch.redPacketOpen ?: redPacketOpen,
        redPacketClaimProgress = patch.redPacketClaimProgress ?: redPacketClaimProgress,
        redPacketSummary = patch.redPacketSummary ?: redPacketSummary,
        redPacketBestLuck = patch.redPacketBestLuck ?: redPacketBestLuck,
        redPacketStateRefundedToYou = patch.redPacketStateRefundedToYou ?: redPacketStateRefundedToYou,
        redPacketStateAllClaimed = patch.redPacketStateAllClaimed ?: redPacketStateAllClaimed,
        redPacketStateExpired = patch.redPacketStateExpired ?: redPacketStateExpired,
        redPacketStateWaiting = patch.redPacketStateWaiting ?: redPacketStateWaiting,
        redPacketStateClaimedByMe = patch.redPacketStateClaimedByMe ?: redPacketStateClaimedByMe,
        redPacketStateDrained = patch.redPacketStateDrained ?: redPacketStateDrained,
        redPacketStateClaimable = patch.redPacketStateClaimable ?: redPacketStateClaimable,
        redPacketCoverFallbackName = patch.redPacketCoverFallbackName ?: redPacketCoverFallbackName,
        redPacketCoverPeerFallback = patch.redPacketCoverPeerFallback ?: redPacketCoverPeerFallback,
        redPacketCoverFrom = patch.redPacketCoverFrom ?: redPacketCoverFrom,
        redPacketOpenShort = patch.redPacketOpenShort ?: redPacketOpenShort,
        redPacketViewDetail = patch.redPacketViewDetail ?: redPacketViewDetail,
        freezeTypeWithdraw = patch.freezeTypeWithdraw ?: freezeTypeWithdraw,
        freezeTypeAudit = patch.freezeTypeAudit ?: freezeTypeAudit,
        freezeTypeAccountFrozen = patch.freezeTypeAccountFrozen ?: freezeTypeAccountFrozen,
        freezeTypeOther = patch.freezeTypeOther ?: freezeTypeOther,
        freezeWithdrawStatusReviewing = patch.freezeWithdrawStatusReviewing ?: freezeWithdrawStatusReviewing,
        freezeWithdrawStatusReturned = patch.freezeWithdrawStatusReturned ?: freezeWithdrawStatusReturned,
        freezeWithdrawStatusPaid = patch.freezeWithdrawStatusPaid ?: freezeWithdrawStatusPaid,
        freezeStatusExpired = patch.freezeStatusExpired ?: freezeStatusExpired,
        freezeStatusHolding = patch.freezeStatusHolding ?: freezeStatusHolding,
        freezeStatusReleased = patch.freezeStatusReleased ?: freezeStatusReleased,
        freezeStatusDeducted = patch.freezeStatusDeducted ?: freezeStatusDeducted,
        freezeRecordsTitle = patch.freezeRecordsTitle ?: freezeRecordsTitle,
        freezeNoRecords = patch.freezeNoRecords ?: freezeNoRecords,
        freezeDetailTitle = patch.freezeDetailTitle ?: freezeDetailTitle,
        freezeFieldType = patch.freezeFieldType ?: freezeFieldType,
        freezeFieldWithdrawAmount = patch.freezeFieldWithdrawAmount ?: freezeFieldWithdrawAmount,
        freezeFieldFrozenAmount = patch.freezeFieldFrozenAmount ?: freezeFieldFrozenAmount,
        freezeFieldRelatedOrder = patch.freezeFieldRelatedOrder ?: freezeFieldRelatedOrder,
        freezeFieldNote = patch.freezeFieldNote ?: freezeFieldNote,
        freezeFieldReason = patch.freezeFieldReason ?: freezeFieldReason,
        previewLargeImage = patch.previewLargeImage ?: previewLargeImage,
        previewImageThumb = patch.previewImageThumb ?: previewImageThumb,
        previewVideoThumb = patch.previewVideoThumb ?: previewVideoThumb,
        previewLoadingWithPercent = patch.previewLoadingWithPercent ?: previewLoadingWithPercent,
        previewPausedWithPercent = patch.previewPausedWithPercent ?: previewPausedWithPercent,
        previewLoadFailedRetrying = patch.previewLoadFailedRetrying ?: previewLoadFailedRetrying,
        previewLoadFailedTapToRetry = patch.previewLoadFailedTapToRetry ?: previewLoadFailedTapToRetry,
        reportReasonSpam = patch.reportReasonSpam ?: reportReasonSpam,
        reportReasonHarassment = patch.reportReasonHarassment ?: reportReasonHarassment,
        reportReasonSexual = patch.reportReasonSexual ?: reportReasonSexual,
        reportReasonFraud = patch.reportReasonFraud ?: reportReasonFraud,
        reportReasonOther = patch.reportReasonOther ?: reportReasonOther,
        reportPickReason = patch.reportPickReason ?: reportPickReason,
        reportUnsentMessage = patch.reportUnsentMessage ?: reportUnsentMessage,
        reportSubmitted = patch.reportSubmitted ?: reportSubmitted,
        reportSubmitFailed = patch.reportSubmitFailed ?: reportSubmitFailed,
        accountCurrentlySignedIn = patch.accountCurrentlySignedIn ?: accountCurrentlySignedIn,
        accountCurrentlySignedInWith = patch.accountCurrentlySignedInWith ?: accountCurrentlySignedInWith,
        desktopPickConversation = patch.desktopPickConversation ?: desktopPickConversation,
        previewMentionPrefix = patch.previewMentionPrefix ?: previewMentionPrefix,
        previewLinkWithTitle = patch.previewLinkWithTitle ?: previewLinkWithTitle,
        previewLocationWithName = patch.previewLocationWithName ?: previewLocationWithName,
        previewContactWithUid = patch.previewContactWithUid ?: previewContactWithUid,
        connectingShort = patch.connectingShort ?: connectingShort,
        initializingShort = patch.initializingShort ?: initializingShort,
        avatarNotAnImage = patch.avatarNotAnImage ?: avatarNotAnImage,
    )
}
