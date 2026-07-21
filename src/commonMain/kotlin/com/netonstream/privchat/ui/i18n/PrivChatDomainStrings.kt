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
        networkError == null

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
    val messageInputHint: String,
    val messageVoiceHint: String,
    val presenceOnline: String,
    val presenceOffline: String,
    val presenceLastSeenPrefix: String,
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
    val messageInputHint: String? = null,
    val messageVoiceHint: String? = null,
    val presenceOnline: String? = null,
    val presenceOffline: String? = null,
    val presenceLastSeenPrefix: String? = null,
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
        messageInputHint == null &&
        messageVoiceHint == null &&
        presenceOnline == null &&
        presenceOffline == null &&
        presenceLastSeenPrefix == null

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
        messageInputHint = patch.messageInputHint ?: messageInputHint,
        messageVoiceHint = patch.messageVoiceHint ?: messageVoiceHint,
        presenceOnline = patch.presenceOnline ?: presenceOnline,
        presenceOffline = patch.presenceOffline ?: presenceOffline,
        presenceLastSeenPrefix = patch.presenceLastSeenPrefix ?: presenceLastSeenPrefix,
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
    val friendSettingsShare: String,
    val friendSettingsSpecialFollow: String,
    val friendSettingsDelete: String,
    val permissionAllowAny: String,
    val permissionDenyAny: String,
    val permissionRequireRequest: String,
    val permissionNone: String,
)

data class PrivChatFriendStringsPatch(
    val friendSettingsTitle: String? = null,
    val friendSettingsShare: String? = null,
    val friendSettingsSpecialFollow: String? = null,
    val friendSettingsDelete: String? = null,
    val permissionAllowAny: String? = null,
    val permissionDenyAny: String? = null,
    val permissionRequireRequest: String? = null,
    val permissionNone: String? = null,
)

val PrivChatFriendStringsPatch.isEmpty: Boolean
    get() = friendSettingsTitle == null &&
        friendSettingsShare == null &&
        friendSettingsSpecialFollow == null &&
        friendSettingsDelete == null &&
        permissionAllowAny == null &&
        permissionDenyAny == null &&
        permissionRequireRequest == null &&
        permissionNone == null

fun PrivChatFriendStrings.merge(patch: PrivChatFriendStringsPatch?): PrivChatFriendStrings {
    if (patch == null || patch.isEmpty) return this
    return copy(
        friendSettingsTitle = patch.friendSettingsTitle ?: friendSettingsTitle,
        friendSettingsShare = patch.friendSettingsShare ?: friendSettingsShare,
        friendSettingsSpecialFollow = patch.friendSettingsSpecialFollow ?: friendSettingsSpecialFollow,
        friendSettingsDelete = patch.friendSettingsDelete ?: friendSettingsDelete,
        permissionAllowAny = patch.permissionAllowAny ?: permissionAllowAny,
        permissionDenyAny = patch.permissionDenyAny ?: permissionDenyAny,
        permissionRequireRequest = patch.permissionRequireRequest ?: permissionRequireRequest,
        permissionNone = patch.permissionNone ?: permissionNone,
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
    val groupMember: String,
    val groupCreate: String,
    val groupLeave: String,
    val groupDissolve: String,
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
    val groupMember: String? = null,
    val groupCreate: String? = null,
    val groupLeave: String? = null,
    val groupDissolve: String? = null,
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
        groupMember == null &&
        groupCreate == null &&
        groupLeave == null &&
        groupDissolve == null

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
        groupMember = patch.groupMember ?: groupMember,
        groupCreate = patch.groupCreate ?: groupCreate,
        groupLeave = patch.groupLeave ?: groupLeave,
        groupDissolve = patch.groupDissolve ?: groupDissolve,
    )
}

@Immutable
data class PrivChatSettingsStrings(
    val settingsTitle: String,
    val settingsProfile: String,
    val settingsWallet: String,
    val settingsUserId: String,
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
    val settingsFriendPermission: String,
    val settingsAppearance: String,
    val settingsLanguage: String,
    val settingsLightTheme: String,
    val settingsDarkTheme: String,
    val settingsSystemTheme: String,
    val settingsAbout: String,
    val settingsMore: String,
    val settingsLogout: String,
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
    val settingsSectionHelp: String,
    val settingsHelp: String,
    val settingsFeedback: String,
    val settingsSwitchAccount: String,
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
    val settingsFriendPermission: String? = null,
    val settingsAppearance: String? = null,
    val settingsLanguage: String? = null,
    val settingsLightTheme: String? = null,
    val settingsDarkTheme: String? = null,
    val settingsSystemTheme: String? = null,
    val settingsAbout: String? = null,
    val settingsMore: String? = null,
    val settingsLogout: String? = null,
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
    val settingsSectionHelp: String? = null,
    val settingsHelp: String? = null,
    val settingsFeedback: String? = null,
    val settingsSwitchAccount: String? = null,
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
        settingsFriendPermission == null &&
        settingsAppearance == null &&
        settingsLanguage == null &&
        settingsLightTheme == null &&
        settingsDarkTheme == null &&
        settingsSystemTheme == null &&
        settingsAbout == null &&
        settingsMore == null &&
        settingsLogout == null &&
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
        settingsSectionHelp == null &&
        settingsHelp == null &&
        settingsFeedback == null &&
        settingsSwitchAccount == null

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
        settingsFriendPermission = patch.settingsFriendPermission ?: settingsFriendPermission,
        settingsAppearance = patch.settingsAppearance ?: settingsAppearance,
        settingsLanguage = patch.settingsLanguage ?: settingsLanguage,
        settingsLightTheme = patch.settingsLightTheme ?: settingsLightTheme,
        settingsDarkTheme = patch.settingsDarkTheme ?: settingsDarkTheme,
        settingsSystemTheme = patch.settingsSystemTheme ?: settingsSystemTheme,
        settingsAbout = patch.settingsAbout ?: settingsAbout,
        settingsMore = patch.settingsMore ?: settingsMore,
        settingsLogout = patch.settingsLogout ?: settingsLogout,
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
        settingsSectionHelp = patch.settingsSectionHelp ?: settingsSectionHelp,
        settingsHelp = patch.settingsHelp ?: settingsHelp,
        settingsFeedback = patch.settingsFeedback ?: settingsFeedback,
        settingsSwitchAccount = patch.settingsSwitchAccount ?: settingsSwitchAccount,
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
)

data class PrivChatAuxiliaryStringsPatch(
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
        bannerServerBusy == null

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
    )
}
