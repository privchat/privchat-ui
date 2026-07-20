package com.netonstream.privchat.ui.i18n

// 生成/维护说明：i18n 文案按语义域拆分（gearui 标准：分域小 data class，避免
// 单类字段爆炸触发 DEX 方法字节码 / 255 参数上限）。门面 PrivChatStrings 用委托
// getter 暴露扁平 `strings.xxx` API 保持调用点稳定。新增文案加到对应域即可。

import androidx.compose.runtime.Immutable

/**
 * i18n 门面：聚合各语义域，委托 getter 暴露扁平 `strings.xxx` API。
 * 加字段：改对应域 data class + 各语言域实例（PrivChatStringPacks*.kt）。
 */
@Immutable
data class PrivChatStrings(
    val dCommon: PrivChatCommonStrings,
    val dConversation: PrivChatConversationStrings,
    val dMessage: PrivChatMessageStrings,
    val dContact: PrivChatContactStrings,
    val dSearch: PrivChatSearchStrings,
    val dUser: PrivChatUserStrings,
    val dFriend: PrivChatFriendStrings,
    val dChatSettings: PrivChatChatSettingsStrings,
    val dGroup: PrivChatGroupStrings,
    val dSettings: PrivChatSettingsStrings,
    val dAbout: PrivChatAboutStrings,
    val dAuxiliary: PrivChatAuxiliaryStrings,
) {
    val appName: String get() = dCommon.appName
    val confirm: String get() = dCommon.confirm
    val cancel: String get() = dCommon.cancel
    val save: String get() = dCommon.save
    val edit: String get() = dCommon.edit
    val delete: String get() = dCommon.delete
    val search: String get() = dCommon.search
    val loading: String get() = dCommon.loading
    val retry: String get() = dCommon.retry
    val noData: String get() = dCommon.noData
    val networkError: String get() = dCommon.networkError
    val tabConversation: String get() = dConversation.tabConversation
    val tabContact: String get() = dConversation.tabContact
    val tabMe: String get() = dConversation.tabMe
    val conversationTitle: String get() = dConversation.conversationTitle
    val conversationEmpty: String get() = dConversation.conversationEmpty
    val conversationMuted: String get() = dConversation.conversationMuted
    val conversationPinned: String get() = dConversation.conversationPinned
    val conversationPin: String get() = dConversation.conversationPin
    val conversationUnpin: String get() = dConversation.conversationUnpin
    val conversationHide: String get() = dConversation.conversationHide
    val conversationDelete: String get() = dConversation.conversationDelete
    val conversationDraft: String get() = dConversation.conversationDraft
    val conversationAtMe: String get() = dConversation.conversationAtMe
    val conversationAtAll: String get() = dConversation.conversationAtAll
    val conversationUnread: String get() = dConversation.conversationUnread
    val messageImage: String get() = dMessage.messageImage
    val messageVideo: String get() = dMessage.messageVideo
    val messageVoice: String get() = dMessage.messageVoice
    val messageFile: String get() = dMessage.messageFile
    val messageLocation: String get() = dMessage.messageLocation
    val messageSticker: String get() = dMessage.messageSticker
    val messageRevoked: String get() = dMessage.messageRevoked
    val messageSystem: String get() = dMessage.messageSystem
    val messageUnknown: String get() = dMessage.messageUnknown
    val messageSending: String get() = dMessage.messageSending
    val messageSendFailed: String get() = dMessage.messageSendFailed
    val messageInputHint: String get() = dMessage.messageInputHint
    val messageVoiceHint: String get() = dMessage.messageVoiceHint
    val presenceOnline: String get() = dMessage.presenceOnline
    val presenceOffline: String get() = dMessage.presenceOffline
    val presenceLastSeenPrefix: String get() = dMessage.presenceLastSeenPrefix
    val contactTitle: String get() = dContact.contactTitle
    val contactFriends: String get() = dContact.contactFriends
    val contactGroups: String get() = dContact.contactGroups
    val contactEmpty: String get() = dContact.contactEmpty
    val contactNewFriend: String get() = dContact.contactNewFriend
    val contactFriendRequest: String get() = dContact.contactFriendRequest
    val contactAddFriend: String get() = dContact.contactAddFriend
    val contactDeleteFriend: String get() = dContact.contactDeleteFriend
    val contactMyGroups: String get() = dContact.contactMyGroups
    val contactGroupsEmpty: String get() = dContact.contactGroupsEmpty
    val contactSearchUser: String get() = dContact.contactSearchUser
    val myQrCodeTitle: String get() = dContact.myQrCodeTitle
    val menuCreateGroup: String get() = dContact.menuCreateGroup
    val menuAddFriend: String get() = dContact.menuAddFriend
    val menuScan: String get() = dContact.menuScan
    val menuMyQrCode: String get() = dContact.menuMyQrCode
    val friendRequestTitle: String get() = dContact.friendRequestTitle
    val friendRequestEmpty: String get() = dContact.friendRequestEmpty
    val friendRequestAccept: String get() = dContact.friendRequestAccept
    val friendRequestReject: String get() = dContact.friendRequestReject
    val friendRequestMessage: String get() = dContact.friendRequestMessage
    val friendRequestTime: String get() = dContact.friendRequestTime
    val friendRequestAccepted: String get() = dContact.friendRequestAccepted
    val friendRequestRejected: String get() = dContact.friendRequestRejected
    val friendRequestView: String get() = dContact.friendRequestView
    val friendRequestAdded: String get() = dContact.friendRequestAdded
    val friendRequestTabReceived: String get() = dContact.friendRequestTabReceived
    val friendRequestTabSent: String get() = dContact.friendRequestTabSent
    val friendRequestDecline: String get() = dContact.friendRequestDecline
    val friendRequestRecall: String get() = dContact.friendRequestRecall
    val friendRequestRecallSoon: String get() = dContact.friendRequestRecallSoon
    val friendRequestSectionOlder: String get() = dContact.friendRequestSectionOlder
    val friendRequestSourceUnknown: String get() = dContact.friendRequestSourceUnknown
    val friendRequestSentEmpty: String get() = dContact.friendRequestSentEmpty
    val friendRequestSourceLabels: Map<String, String> get() = dContact.friendRequestSourceLabels
    val friendRequestStatusLabels: Map<Int, String> get() = dContact.friendRequestStatusLabels
    val searchUserTitle: String get() = dSearch.searchUserTitle
    val searchUserPlaceholder: String get() = dSearch.searchUserPlaceholder
    val searchUserSearching: String get() = dSearch.searchUserSearching
    val searchUserNoResult: String get() = dSearch.searchUserNoResult
    val searchUserTryAgain: String get() = dSearch.searchUserTryAgain
    val searchUserHint: String get() = dSearch.searchUserHint
    val searchUserError: String get() = dSearch.searchUserError
    val searchUserScan: String get() = dSearch.searchUserScan
    val searchUserPhoneContacts: String get() = dSearch.searchUserPhoneContacts
    val searchUserMyQrCode: String get() = dSearch.searchUserMyQrCode
    val searchUserMyAccount: String get() = dSearch.searchUserMyAccount
    val globalSearchTitle: String get() = dSearch.globalSearchTitle
    val globalSearchPlaceholder: String get() = dSearch.globalSearchPlaceholder
    val globalSearchSectionChannels: String get() = dSearch.globalSearchSectionChannels
    val globalSearchSectionMessages: String get() = dSearch.globalSearchSectionMessages
    val globalSearchNoResult: String get() = dSearch.globalSearchNoResult
    val globalSearchLoadMore: String get() = dSearch.globalSearchLoadMore
    val globalSearchAnchorMissing: String get() = dSearch.globalSearchAnchorMissing
    val userProfileTitle: String get() = dUser.userProfileTitle
    val userProfileUserId: String get() = dUser.userProfileUserId
    val userProfileSystemAccount: String get() = dUser.userProfileSystemAccount
    val userBadgeSystem: String get() = dUser.userBadgeSystem
    val userBadgeBot: String get() = dUser.userBadgeBot
    val userProfileBio: String get() = dUser.userProfileBio
    val userProfileRemark: String get() = dUser.userProfileRemark
    val userProfileRemarkPlaceholder: String get() = dUser.userProfileRemarkPlaceholder
    val userProfileNickname: String get() = dUser.userProfileNickname
    val userProfileSendMessage: String get() = dUser.userProfileSendMessage
    val userProfileAddFriend: String get() = dUser.userProfileAddFriend
    val userProfileFollowBot: String get() = dUser.userProfileFollowBot
    val userProfileFollowingBot: String get() = dUser.userProfileFollowingBot
    val userProfileFollowedBotToast: String get() = dUser.userProfileFollowedBotToast
    val userProfileCannotAddSelf: String get() = dUser.userProfileCannotAddSelf
    val userProfileAcceptFriendRequest: String get() = dUser.userProfileAcceptFriendRequest
    val userProfileAdding: String get() = dUser.userProfileAdding
    val userProfileRequestSent: String get() = dUser.userProfileRequestSent
    val userProfileDeleteFriend: String get() = dUser.userProfileDeleteFriend
    val userProfileDeleting: String get() = dUser.userProfileDeleting
    val userProfileBlockUser: String get() = dUser.userProfileBlockUser
    val userProfileBlocking: String get() = dUser.userProfileBlocking
    val userProfileDeleteFriendConfirmTitle: String get() = dUser.userProfileDeleteFriendConfirmTitle
    val userProfileDeleteFriendConfirmMessage: String get() = dUser.userProfileDeleteFriendConfirmMessage
    val userProfileBlockUserConfirmTitle: String get() = dUser.userProfileBlockUserConfirmTitle
    val userProfileBlockUserConfirmMessage: String get() = dUser.userProfileBlockUserConfirmMessage
    val friendRequestInputTitle: String get() = dUser.friendRequestInputTitle
    val friendRequestInputPlaceholder: String get() = dUser.friendRequestInputPlaceholder
    val friendRequestSentTitle: String get() = dUser.friendRequestSentTitle
    val friendSettingsTitle: String get() = dFriend.friendSettingsTitle
    val friendSettingsShare: String get() = dFriend.friendSettingsShare
    val friendSettingsSpecialFollow: String get() = dFriend.friendSettingsSpecialFollow
    val friendSettingsDelete: String get() = dFriend.friendSettingsDelete
    val chatSettingsTitle: String get() = dChatSettings.chatSettingsTitle
    val chatSettingsGroupName: String get() = dChatSettings.chatSettingsGroupName
    val groupNameEditTitle: String get() = dChatSettings.groupNameEditTitle
    val chatSettingsGroupQrCode: String get() = dChatSettings.chatSettingsGroupQrCode
    val chatSettingsGroupMembers: String get() = dChatSettings.chatSettingsGroupMembers
    val chatSettingsGroupManage: String get() = dChatSettings.chatSettingsGroupManage
    val chatSettingsMute: String get() = dChatSettings.chatSettingsMute
    val chatSettingsPin: String get() = dChatSettings.chatSettingsPin
    val chatSettingsLeaveGroup: String get() = dChatSettings.chatSettingsLeaveGroup
    val chatSettingsLeaveGroupConfirmTitle: String get() = dChatSettings.chatSettingsLeaveGroupConfirmTitle
    val chatSettingsLeaveGroupConfirmMessage: String get() = dChatSettings.chatSettingsLeaveGroupConfirmMessage
    val groupSettingsSectionTitle: String get() = dGroup.groupSettingsSectionTitle
    val groupSettingsAllowSearch: String get() = dGroup.groupSettingsAllowSearch
    val groupSettingsMemberCanInvite: String get() = dGroup.groupSettingsMemberCanInvite
    val groupSettingsAllowMemberAddFriend: String get() = dGroup.groupSettingsAllowMemberAddFriend
    val groupSettingsAllMuted: String get() = dGroup.groupSettingsAllMuted
    val groupSettingsJoinPolicy: String get() = dGroup.groupSettingsJoinPolicy
    val groupSettingsJoinPolicyNone: String get() = dGroup.groupSettingsJoinPolicyNone
    val groupSettingsJoinPolicyApproval: String get() = dGroup.groupSettingsJoinPolicyApproval
    val groupApprovalTitle: String get() = dGroup.groupApprovalTitle
    val groupApprovalEmpty: String get() = dGroup.groupApprovalEmpty
    val groupSettingsJoinPolicyOpen: String get() = dGroup.groupSettingsJoinPolicyOpen
    val groupSettingsUpdateFailed: String get() = dGroup.groupSettingsUpdateFailed
    val groupMemberMute: String get() = dGroup.groupMemberMute
    val groupMemberRemove: String get() = dGroup.groupMemberRemove
    val groupMemberUnmute: String get() = dGroup.groupMemberUnmute
    val groupMuteDurationTitle: String get() = dGroup.groupMuteDurationTitle
    val groupMuteDuration10m: String get() = dGroup.groupMuteDuration10m
    val groupMuteDuration1h: String get() = dGroup.groupMuteDuration1h
    val groupMuteDuration1d: String get() = dGroup.groupMuteDuration1d
    val groupMuteDurationForever: String get() = dGroup.groupMuteDurationForever
    val groupMuteSuccess: String get() = dGroup.groupMuteSuccess
    val groupUnmuteSuccess: String get() = dGroup.groupUnmuteSuccess
    val messagePin: String get() = dGroup.messagePin
    val messageUnpin: String get() = dGroup.messageUnpin
    val pinnedMessagesTitle: String get() = dGroup.pinnedMessagesTitle
    val messagePinSuccess: String get() = dGroup.messagePinSuccess
    val messageUnpinSuccess: String get() = dGroup.messageUnpinSuccess
    val presenceOfflineMinutesAgo: String get() = dGroup.presenceOfflineMinutesAgo
    val presenceOfflineHoursAgo: String get() = dGroup.presenceOfflineHoursAgo
    val presenceOfflineDaysAgo: String get() = dGroup.presenceOfflineDaysAgo
    val groupMembers: String get() = dGroup.groupMembers
    val groupOwner: String get() = dGroup.groupOwner
    val groupAdmin: String get() = dGroup.groupAdmin
    val groupMember: String get() = dGroup.groupMember
    val groupCreate: String get() = dGroup.groupCreate
    val groupLeave: String get() = dGroup.groupLeave
    val groupDissolve: String get() = dGroup.groupDissolve
    val settingsTitle: String get() = dSettings.settingsTitle
    val settingsProfile: String get() = dSettings.settingsProfile
    val settingsWallet: String get() = dSettings.settingsWallet
    val settingsUserId: String get() = dSettings.settingsUserId
    val settingsUsername: String get() = dSettings.settingsUsername
    val settingsMobile: String get() = dSettings.settingsMobile
    val settingsNickname: String get() = dSettings.settingsNickname
    val settingsSignature: String get() = dSettings.settingsSignature
    val settingsGender: String get() = dSettings.settingsGender
    val settingsBirthday: String get() = dSettings.settingsBirthday
    val settingsNotSet: String get() = dSettings.settingsNotSet
    val profileLoadFailed: String get() = dSettings.profileLoadFailed
    val profileUpdateFailed: String get() = dSettings.profileUpdateFailed
    val profileSessionExpired: String get() = dSettings.profileSessionExpired
    val profileBuiltinSaveUnsupported: String get() = dSettings.profileBuiltinSaveUnsupported
    val settingsUsernameRule: String get() = dSettings.settingsUsernameRule
    val settingsUsernameLockedDaysPrefix: String get() = dSettings.settingsUsernameLockedDaysPrefix
    val settingsUsernameLockedDaysSuffix: String get() = dSettings.settingsUsernameLockedDaysSuffix
    val settingsUsernameErrorInvalidFormat: String get() = dSettings.settingsUsernameErrorInvalidFormat
    val settingsUsernameErrorReserved: String get() = dSettings.settingsUsernameErrorReserved
    val settingsUsernameErrorTaken: String get() = dSettings.settingsUsernameErrorTaken
    val settingsUsernameErrorRateLimited: String get() = dSettings.settingsUsernameErrorRateLimited
    val settingsMobileNotEditableTip: String get() = dSettings.settingsMobileNotEditableTip
    val profileAvatarUploading: String get() = dSettings.profileAvatarUploading
    val profileAvatarPendingSave: String get() = dSettings.profileAvatarPendingSave
    val profileAvatarChange: String get() = dSettings.profileAvatarChange
    val settingsFriendPermission: String get() = dSettings.settingsFriendPermission
    val settingsAppearance: String get() = dSettings.settingsAppearance
    val settingsLanguage: String get() = dSettings.settingsLanguage
    val settingsLightTheme: String get() = dSettings.settingsLightTheme
    val settingsDarkTheme: String get() = dSettings.settingsDarkTheme
    val settingsSystemTheme: String get() = dSettings.settingsSystemTheme
    val settingsAbout: String get() = dSettings.settingsAbout
    val settingsMore: String get() = dSettings.settingsMore
    val settingsLogout: String get() = dSettings.settingsLogout
    val settingsSectionAccount: String get() = dSettings.settingsSectionAccount
    val settingsAccountSecurity: String get() = dSettings.settingsAccountSecurity
    val settingsPrivacy: String get() = dSettings.settingsPrivacy
    val settingsSectionGeneral: String get() = dSettings.settingsSectionGeneral
    val settingsNotification: String get() = dSettings.settingsNotification
    val settingsSectionHelp: String get() = dSettings.settingsSectionHelp
    val settingsHelp: String get() = dSettings.settingsHelp
    val settingsFeedback: String get() = dSettings.settingsFeedback
    val settingsSwitchAccount: String get() = dSettings.settingsSwitchAccount
    val permissionAllowAny: String get() = dFriend.permissionAllowAny
    val permissionDenyAny: String get() = dFriend.permissionDenyAny
    val permissionRequireRequest: String get() = dFriend.permissionRequireRequest
    val permissionNone: String get() = dFriend.permissionNone
    val genderUnknown: String get() = dUser.genderUnknown
    val genderMale: String get() = dUser.genderMale
    val genderFemale: String get() = dUser.genderFemale
    val aboutTitle: String get() = dAbout.aboutTitle
    val aboutSdkVersion: String get() = dAbout.aboutSdkVersion
    val aboutVersion: String get() = dAbout.aboutVersion
    val aboutPrivacyPolicy: String get() = dAbout.aboutPrivacyPolicy
    val aboutUserAgreement: String get() = dAbout.aboutUserAgreement
    val aboutDisclaimer: String get() = dAbout.aboutDisclaimer
    val aboutContactUs: String get() = dAbout.aboutContactUs
    val aboutOfficialWebsite: String get() = dAbout.aboutOfficialWebsite
    val aboutSourceCode: String get() = dAbout.aboutSourceCode
    val aboutCopyright: String get() = dAbout.aboutCopyright
    val aboutUiVersion: String get() = dAbout.aboutUiVersion
    val aboutGitCommit: String get() = dAbout.aboutGitCommit
    val aboutBuildTime: String get() = dAbout.aboutBuildTime
    val aboutSdkStatus: String get() = dAbout.aboutSdkStatus
    val aboutConnectionState: String get() = dAbout.aboutConnectionState
    val aboutLoginState: String get() = dAbout.aboutLoginState
    val aboutLastSdkError: String get() = dAbout.aboutLastSdkError
    val aboutConnStateDisconnected: String get() = dAbout.aboutConnStateDisconnected
    val aboutConnStateConnecting: String get() = dAbout.aboutConnStateConnecting
    val aboutConnStateConnected: String get() = dAbout.aboutConnStateConnected
    val aboutConnStateReconnecting: String get() = dAbout.aboutConnStateReconnecting
    val aboutConnStateFailed: String get() = dAbout.aboutConnStateFailed
    val aboutLoginStateLoggedOut: String get() = dAbout.aboutLoginStateLoggedOut
    val aboutLoginStateLoggingIn: String get() = dAbout.aboutLoginStateLoggingIn
    val aboutLoginStateSyncing: String get() = dAbout.aboutLoginStateSyncing
    val aboutLoginStateSyncReady: String get() = dAbout.aboutLoginStateSyncReady
    val aboutLoginStateLoggedIn: String get() = dAbout.aboutLoginStateLoggedIn
    val contactUsTitle: String get() = dAbout.contactUsTitle
    val contactUsSwitch: String get() = dAbout.contactUsSwitch
    val contactUsQuestion: String get() = dAbout.contactUsQuestion
    val contactUsServiceTime: String get() = dAbout.contactUsServiceTime
    val contactUsChatNow: String get() = dAbout.contactUsChatNow
    val profileEditSave: String get() = dUser.profileEditSave
    val profileEditAvatarHint: String get() = dUser.profileEditAvatarHint
    val changePasswordTitle: String get() = dAuxiliary.changePasswordTitle
    val changePasswordNew: String get() = dAuxiliary.changePasswordNew
    val changePasswordConfirm: String get() = dAuxiliary.changePasswordConfirm
    val changePasswordSmsCode: String get() = dAuxiliary.changePasswordSmsCode
    val changePasswordSendCode: String get() = dAuxiliary.changePasswordSendCode
    val changePasswordSubmit: String get() = dAuxiliary.changePasswordSubmit
    val changePasswordSuccess: String get() = dAuxiliary.changePasswordSuccess
    val changePasswordMismatch: String get() = dAuxiliary.changePasswordMismatch
    val setPasswordTitle: String get() = dAuxiliary.setPasswordTitle
    val setPasswordHint: String get() = dAuxiliary.setPasswordHint
    val setPasswordSubmit: String get() = dAuxiliary.setPasswordSubmit
    val setPasswordSuccess: String get() = dAuxiliary.setPasswordSuccess
    val setNicknameTitle: String get() = dAuxiliary.setNicknameTitle
    val setNicknameHint: String get() = dAuxiliary.setNicknameHint
    val setNicknameSubmit: String get() = dAuxiliary.setNicknameSubmit
    val setNicknameError: String get() = dAuxiliary.setNicknameError
    val requiredActionUnsupportedTitle: String get() = dAuxiliary.requiredActionUnsupportedTitle
    val requiredActionUnsupportedMessage: String get() = dAuxiliary.requiredActionUnsupportedMessage
    val requiredActionUnsupportedReload: String get() = dAuxiliary.requiredActionUnsupportedReload
    val timeYesterday: String get() = dAuxiliary.timeYesterday
    val timeToday: String get() = dAuxiliary.timeToday
    val timeSunday: String get() = dAuxiliary.timeSunday
    val timeMonday: String get() = dAuxiliary.timeMonday
    val timeTuesday: String get() = dAuxiliary.timeTuesday
    val timeWednesday: String get() = dAuxiliary.timeWednesday
    val timeThursday: String get() = dAuxiliary.timeThursday
    val timeFriday: String get() = dAuxiliary.timeFriday
    val timeSaturday: String get() = dAuxiliary.timeSaturday
    val systemTemplates: Map<String, String> get() = dAuxiliary.systemTemplates
    val systemListSeparator: String get() = dAuxiliary.systemListSeparator
    val previewImage: String get() = dAuxiliary.previewImage
    val previewVideo: String get() = dAuxiliary.previewVideo
    val previewVoice: String get() = dAuxiliary.previewVoice
    val previewVoiceWithDuration: String get() = dAuxiliary.previewVoiceWithDuration
    val previewFile: String get() = dAuxiliary.previewFile
    val previewFileWithName: String get() = dAuxiliary.previewFileWithName
    val previewSticker: String get() = dAuxiliary.previewSticker
    val previewLocation: String get() = dAuxiliary.previewLocation
    val previewLocationWithAddress: String get() = dAuxiliary.previewLocationWithAddress
    val previewLink: String get() = dAuxiliary.previewLink
    val previewContactCard: String get() = dAuxiliary.previewContactCard
    val previewRedPacket: String get() = dAuxiliary.previewRedPacket
    val previewSystemFallback: String get() = dAuxiliary.previewSystemFallback
    val previewUnknown: String get() = dAuxiliary.previewUnknown
    val previewRecalled: String get() = dAuxiliary.previewRecalled
    val sendSmsCodeFailed: String get() = dAuxiliary.sendSmsCodeFailed
    val connectServerFailed: String get() = dAuxiliary.connectServerFailed
    val sdkInitFailed: String get() = dAuxiliary.sdkInitFailed
    val switchAccountFailed: String get() = dAuxiliary.switchAccountFailed
    val logoutFailed: String get() = dAuxiliary.logoutFailed
    val loginFailed: String get() = dAuxiliary.loginFailed
    val friendRequestAcceptFailed: String get() = dAuxiliary.friendRequestAcceptFailed
    val friendRequestDeclineFailed: String get() = dAuxiliary.friendRequestDeclineFailed
    val messageRecallFailed: String get() = dAuxiliary.messageRecallFailed
    val qrImageDecodeFailed: String get() = dAuxiliary.qrImageDecodeFailed
    val saveFailed: String get() = dAuxiliary.saveFailed
    val operationFailed: String get() = dAuxiliary.operationFailed
    val smsCodeError: String get() = dAuxiliary.smsCodeError
    val loginErrInvalidCredentials: String get() = dAuxiliary.loginErrInvalidCredentials
    val loginErrAccountDisabled: String get() = dAuxiliary.loginErrAccountDisabled
    val registerErrUsernameTaken: String get() = dAuxiliary.registerErrUsernameTaken
    val registerErrUsernameInvalid: String get() = dAuxiliary.registerErrUsernameInvalid
    val registerErrUsernameFormat: String get() = dAuxiliary.registerErrUsernameFormat
    val registerErrPasswordTooShort: String get() = dAuxiliary.registerErrPasswordTooShort
    val systemMessagesName: String get() = dAuxiliary.systemMessagesName
    val groupChatFallback: String get() = dAuxiliary.groupChatFallback
    val signInTitle: String get() = dAuxiliary.signInTitle
    val signInButton: String get() = dAuxiliary.signInButton
    val signInDoneToday: String get() = dAuxiliary.signInDoneToday
    val signInContinuousPrefix: String get() = dAuxiliary.signInContinuousPrefix
    val signInTodayReward: String get() = dAuxiliary.signInTodayReward
    val signInRewardList: String get() = dAuxiliary.signInRewardList
    val signInPointsUnit: String get() = dAuxiliary.signInPointsUnit
    val signInDayUnit: String get() = dAuxiliary.signInDayUnit
    val signInSuccessTitle: String get() = dAuxiliary.signInSuccessTitle
    val signInCashCredited: String get() = dAuxiliary.signInCashCredited
    val inviteBindTitle: String get() = dAuxiliary.inviteBindTitle
    val inviteBindPlaceholder: String get() = dAuxiliary.inviteBindPlaceholder
    val inviteBindButton: String get() = dAuxiliary.inviteBindButton
    val inviteBoundCode: String get() = dAuxiliary.inviteBoundCode
    val inviteBoundInviter: String get() = dAuxiliary.inviteBoundInviter
    val inviteBoundAt: String get() = dAuxiliary.inviteBoundAt
    val inviteBoundHint: String get() = dAuxiliary.inviteBoundHint
    val inviteBoundAutoFriend: String get() = dAuxiliary.inviteBoundAutoFriend
    val inviteBindHint: String get() = dAuxiliary.inviteBindHint
    val inviteBindSuccessFriend: String get() = dAuxiliary.inviteBindSuccessFriend
    val inviteBindSuccess: String get() = dAuxiliary.inviteBindSuccess
    val inviteErrInvalid: String get() = dAuxiliary.inviteErrInvalid
    val loginTabSms: String get() = dAuxiliary.loginTabSms
    val loginTabPassword: String get() = dAuxiliary.loginTabPassword
    val loginUsernamePlaceholder: String get() = dAuxiliary.loginUsernamePlaceholder
    val loginPasswordPlaceholder: String get() = dAuxiliary.loginPasswordPlaceholder
    val loginPasswordNewPlaceholder: String get() = dAuxiliary.loginPasswordNewPlaceholder
    val loginNicknamePlaceholder: String get() = dAuxiliary.loginNicknamePlaceholder
    val loginInviteCodePlaceholder: String get() = dAuxiliary.loginInviteCodePlaceholder
    val loginRegisterButton: String get() = dAuxiliary.loginRegisterButton
    val loginToRegister: String get() = dAuxiliary.loginToRegister
    val loginToLogin: String get() = dAuxiliary.loginToLogin
    // P4 运行时状态条（CLIENT_GLOBAL_STATE §17）
    val bannerConnecting: String get() = dAuxiliary.bannerConnecting
    val bannerDisconnected: String get() = dAuxiliary.bannerDisconnected
    val bannerConnected: String get() = dAuxiliary.bannerConnected
    val bannerReconnecting: String get() = dAuxiliary.bannerReconnecting
    val bannerConnectFailed: String get() = dAuxiliary.bannerConnectFailed
    val bannerSyncing: String get() = dAuxiliary.bannerSyncing
    val loginExpired: String get() = dAuxiliary.loginExpired
    val syncFailedRetry: String get() = dAuxiliary.syncFailedRetry
    val bannerServerBusy: String get() = dAuxiliary.bannerServerBusy
}

data class PrivChatStringsPatch(
    val dCommon: PrivChatCommonStringsPatch? = null,
    val dConversation: PrivChatConversationStringsPatch? = null,
    val dMessage: PrivChatMessageStringsPatch? = null,
    val dContact: PrivChatContactStringsPatch? = null,
    val dSearch: PrivChatSearchStringsPatch? = null,
    val dUser: PrivChatUserStringsPatch? = null,
    val dFriend: PrivChatFriendStringsPatch? = null,
    val dChatSettings: PrivChatChatSettingsStringsPatch? = null,
    val dGroup: PrivChatGroupStringsPatch? = null,
    val dSettings: PrivChatSettingsStringsPatch? = null,
    val dAbout: PrivChatAboutStringsPatch? = null,
    val dAuxiliary: PrivChatAuxiliaryStringsPatch? = null,
)

val PrivChatStringsPatch.isEmpty: Boolean
    get() = (dCommon == null || dCommon.isEmpty) &&
        (dConversation == null || dConversation.isEmpty) &&
        (dMessage == null || dMessage.isEmpty) &&
        (dContact == null || dContact.isEmpty) &&
        (dSearch == null || dSearch.isEmpty) &&
        (dUser == null || dUser.isEmpty) &&
        (dFriend == null || dFriend.isEmpty) &&
        (dChatSettings == null || dChatSettings.isEmpty) &&
        (dGroup == null || dGroup.isEmpty) &&
        (dSettings == null || dSettings.isEmpty) &&
        (dAbout == null || dAbout.isEmpty) &&
        (dAuxiliary == null || dAuxiliary.isEmpty)

fun PrivChatStrings.merge(patch: PrivChatStringsPatch?): PrivChatStrings {
    if (patch == null || patch.isEmpty) return this
    return copy(
        dCommon = dCommon.merge(patch.dCommon),
        dConversation = dConversation.merge(patch.dConversation),
        dMessage = dMessage.merge(patch.dMessage),
        dContact = dContact.merge(patch.dContact),
        dSearch = dSearch.merge(patch.dSearch),
        dUser = dUser.merge(patch.dUser),
        dFriend = dFriend.merge(patch.dFriend),
        dChatSettings = dChatSettings.merge(patch.dChatSettings),
        dGroup = dGroup.merge(patch.dGroup),
        dSettings = dSettings.merge(patch.dSettings),
        dAbout = dAbout.merge(patch.dAbout),
        dAuxiliary = dAuxiliary.merge(patch.dAuxiliary),
    )
}

object PrivChatStringPacks {
    const val DEFAULT_LANGUAGE_TAG = "zh-Hans"

    val Chinese = PrivChatStrings(
        dCommon = zhCommon,
        dConversation = zhConversation,
        dMessage = zhMessage,
        dContact = zhContact,
        dSearch = zhSearch,
        dUser = zhUser,
        dFriend = zhFriend,
        dChatSettings = zhChatSettings,
        dGroup = zhGroup,
        dSettings = zhSettings,
        dAbout = zhAbout,
        dAuxiliary = zhAuxiliary,
    )

    val English = PrivChatStrings(
        dCommon = enCommon,
        dConversation = enConversation,
        dMessage = enMessage,
        dContact = enContact,
        dSearch = enSearch,
        dUser = enUser,
        dFriend = enFriend,
        dChatSettings = enChatSettings,
        dGroup = enGroup,
        dSettings = enSettings,
        dAbout = enAbout,
        dAuxiliary = enAuxiliary,
    )

    val ChineseTraditional = PrivChatStrings(
        dCommon = hantCommon,
        dConversation = hantConversation,
        dMessage = hantMessage,
        dContact = hantContact,
        dSearch = hantSearch,
        dUser = hantUser,
        dFriend = hantFriend,
        dChatSettings = hantChatSettings,
        dGroup = hantGroup,
        dSettings = hantSettings,
        dAbout = hantAbout,
        dAuxiliary = hantAuxiliary,
    )

    val Vietnamese = PrivChatStrings(
        dCommon = viCommon,
        dConversation = viConversation,
        dMessage = viMessage,
        dContact = viContact,
        dSearch = viSearch,
        dUser = viUser,
        dFriend = viFriend,
        dChatSettings = viChatSettings,
        dGroup = viGroup,
        dSettings = viSettings,
        dAbout = viAbout,
        dAuxiliary = viAuxiliary,
    )

    val builtIn: Map<String, PrivChatStrings> = mapOf(
        "zh-Hans" to Chinese, "zh" to Chinese, "zh-CN" to Chinese, "zh-Hans-CN" to Chinese,
        "zh-Hant" to ChineseTraditional, "zh-TW" to ChineseTraditional, "zh-HK" to ChineseTraditional,
        "en-US" to English, "en" to English,
        "vi-VN" to Vietnamese, "vi" to Vietnamese,
    )
}
