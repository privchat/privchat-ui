package com.netonstream.privchat.ui.i18n

import androidx.compose.runtime.Immutable

/**
 * Field-level override for [PrivChatStrings]. Every field is nullable;
 * null means "inherit from base pack". Apps supply patches keyed by
 * language tag via [PrivChatI18nProvider].
 *
 * See gearui-kit `docs/I18N_INTEGRATION.md`.
 */
@Immutable
data class PrivChatStringsPatch(
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
    /** Patch overlay：增量覆写 source 标签；merge 时与基线 map 取 union（patch 覆盖）。 */
    val friendRequestSourceLabels: Map<String, String>? = null,
    /** 同上，针对 status 标签。 */
    val friendRequestStatusLabels: Map<Int, String>? = null,
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
    val friendSettingsTitle: String? = null,
    val friendSettingsShare: String? = null,
    val friendSettingsSpecialFollow: String? = null,
    val friendSettingsDelete: String? = null,
    val chatSettingsTitle: String? = null,
    val chatSettingsGroupName: String? = null,
    val chatSettingsGroupQrCode: String? = null,
    val chatSettingsGroupMembers: String? = null,
    val chatSettingsGroupManage: String? = null,
    val chatSettingsMute: String? = null,
    val chatSettingsPin: String? = null,
    val chatSettingsLeaveGroup: String? = null,
    val chatSettingsLeaveGroupConfirmTitle: String? = null,
    val chatSettingsLeaveGroupConfirmMessage: String? = null,
    val groupMembers: String? = null,
    val groupOwner: String? = null,
    val groupAdmin: String? = null,
    val groupMember: String? = null,
    val groupCreate: String? = null,
    val groupLeave: String? = null,
    val groupDissolve: String? = null,
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
    val settingsSectionGeneral: String? = null,
    val settingsNotification: String? = null,
    val settingsSectionHelp: String? = null,
    val settingsHelp: String? = null,
    val settingsFeedback: String? = null,
    val settingsSwitchAccount: String? = null,
    val permissionAllowAny: String? = null,
    val permissionDenyAny: String? = null,
    val permissionRequireRequest: String? = null,
    val permissionNone: String? = null,
    val genderUnknown: String? = null,
    val genderMale: String? = null,
    val genderFemale: String? = null,
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
    val profileEditSave: String? = null,
    val profileEditAvatarHint: String? = null,
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
    val timeYesterday: String? = null,
    val timeToday: String? = null,
    val timeSunday: String? = null,
    val timeMonday: String? = null,
    val timeTuesday: String? = null,
    val timeWednesday: String? = null,
    val timeThursday: String? = null,
    val timeFriday: String? = null,
    val timeSaturday: String? = null,
    /** 系统消息模板覆盖（patch 优先于内置语言包；只覆盖给定 key，未给的保留原值）。 */
    val systemTemplates: Map<String, String>? = null,
    /** 列表展开占位符 `{n+}` 的元素分隔符覆盖。 */
    val systemListSeparator: String? = null,
    // 会话列表预览覆盖
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
)

val PrivChatStringsPatch.isEmpty: Boolean
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
        tabConversation == null &&
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
        conversationUnread == null &&
        messageImage == null &&
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
        presenceLastSeenPrefix == null &&
        contactTitle == null &&
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
        friendRequestStatusLabels == null &&
        searchUserTitle == null &&
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
        userProfileTitle == null &&
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
        friendSettingsTitle == null &&
        friendSettingsShare == null &&
        friendSettingsSpecialFollow == null &&
        friendSettingsDelete == null &&
        chatSettingsTitle == null &&
        chatSettingsGroupName == null &&
        chatSettingsGroupQrCode == null &&
        chatSettingsGroupMembers == null &&
        chatSettingsGroupManage == null &&
        chatSettingsMute == null &&
        chatSettingsPin == null &&
        chatSettingsLeaveGroup == null &&
        chatSettingsLeaveGroupConfirmTitle == null &&
        chatSettingsLeaveGroupConfirmMessage == null &&
        groupMembers == null &&
        groupOwner == null &&
        groupAdmin == null &&
        groupMember == null &&
        groupCreate == null &&
        groupLeave == null &&
        groupDissolve == null &&
        settingsTitle == null &&
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
        settingsSectionGeneral == null &&
        settingsNotification == null &&
        settingsSectionHelp == null &&
        settingsHelp == null &&
        settingsFeedback == null &&
        settingsSwitchAccount == null &&
        permissionAllowAny == null &&
        permissionDenyAny == null &&
        permissionRequireRequest == null &&
        permissionNone == null &&
        genderUnknown == null &&
        genderMale == null &&
        genderFemale == null &&
        aboutTitle == null &&
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
        contactUsChatNow == null &&
        profileEditSave == null &&
        profileEditAvatarHint == null &&
        changePasswordTitle == null &&
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
        previewRecalled == null

/**
 * Apply [patch] on top of base strings. Returns the receiver unchanged when
 * [patch] is `null` or all-null, so the hot path allocates nothing.
 */
fun PrivChatStrings.merge(patch: PrivChatStringsPatch?): PrivChatStrings {
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
        friendRequestSourceLabels = patch.friendRequestSourceLabels
            ?.let { friendRequestSourceLabels + it }
            ?: friendRequestSourceLabels,
        friendRequestStatusLabels = patch.friendRequestStatusLabels
            ?.let { friendRequestStatusLabels + it }
            ?: friendRequestStatusLabels,
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
        friendSettingsTitle = patch.friendSettingsTitle ?: friendSettingsTitle,
        friendSettingsShare = patch.friendSettingsShare ?: friendSettingsShare,
        friendSettingsSpecialFollow = patch.friendSettingsSpecialFollow ?: friendSettingsSpecialFollow,
        friendSettingsDelete = patch.friendSettingsDelete ?: friendSettingsDelete,
        chatSettingsTitle = patch.chatSettingsTitle ?: chatSettingsTitle,
        chatSettingsGroupName = patch.chatSettingsGroupName ?: chatSettingsGroupName,
        chatSettingsGroupQrCode = patch.chatSettingsGroupQrCode ?: chatSettingsGroupQrCode,
        chatSettingsGroupMembers = patch.chatSettingsGroupMembers ?: chatSettingsGroupMembers,
        chatSettingsGroupManage = patch.chatSettingsGroupManage ?: chatSettingsGroupManage,
        chatSettingsMute = patch.chatSettingsMute ?: chatSettingsMute,
        chatSettingsPin = patch.chatSettingsPin ?: chatSettingsPin,
        chatSettingsLeaveGroup = patch.chatSettingsLeaveGroup ?: chatSettingsLeaveGroup,
        chatSettingsLeaveGroupConfirmTitle = patch.chatSettingsLeaveGroupConfirmTitle ?: chatSettingsLeaveGroupConfirmTitle,
        chatSettingsLeaveGroupConfirmMessage = patch.chatSettingsLeaveGroupConfirmMessage ?: chatSettingsLeaveGroupConfirmMessage,
        groupMembers = patch.groupMembers ?: groupMembers,
        groupOwner = patch.groupOwner ?: groupOwner,
        groupAdmin = patch.groupAdmin ?: groupAdmin,
        groupMember = patch.groupMember ?: groupMember,
        groupCreate = patch.groupCreate ?: groupCreate,
        groupLeave = patch.groupLeave ?: groupLeave,
        groupDissolve = patch.groupDissolve ?: groupDissolve,
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
        settingsSectionGeneral = patch.settingsSectionGeneral ?: settingsSectionGeneral,
        settingsNotification = patch.settingsNotification ?: settingsNotification,
        settingsSectionHelp = patch.settingsSectionHelp ?: settingsSectionHelp,
        settingsHelp = patch.settingsHelp ?: settingsHelp,
        settingsFeedback = patch.settingsFeedback ?: settingsFeedback,
        settingsSwitchAccount = patch.settingsSwitchAccount ?: settingsSwitchAccount,
        permissionAllowAny = patch.permissionAllowAny ?: permissionAllowAny,
        permissionDenyAny = patch.permissionDenyAny ?: permissionDenyAny,
        permissionRequireRequest = patch.permissionRequireRequest ?: permissionRequireRequest,
        permissionNone = patch.permissionNone ?: permissionNone,
        genderUnknown = patch.genderUnknown ?: genderUnknown,
        genderMale = patch.genderMale ?: genderMale,
        genderFemale = patch.genderFemale ?: genderFemale,
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
        profileEditSave = patch.profileEditSave ?: profileEditSave,
        profileEditAvatarHint = patch.profileEditAvatarHint ?: profileEditAvatarHint,
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
        timeYesterday = patch.timeYesterday ?: timeYesterday,
        timeToday = patch.timeToday ?: timeToday,
        timeSunday = patch.timeSunday ?: timeSunday,
        timeMonday = patch.timeMonday ?: timeMonday,
        timeTuesday = patch.timeTuesday ?: timeTuesday,
        timeWednesday = patch.timeWednesday ?: timeWednesday,
        timeThursday = patch.timeThursday ?: timeThursday,
        timeFriday = patch.timeFriday ?: timeFriday,
        timeSaturday = patch.timeSaturday ?: timeSaturday,
        systemTemplates = patch.systemTemplates
            ?.let { override -> systemTemplates + override }
            ?: systemTemplates,
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
    )
}
