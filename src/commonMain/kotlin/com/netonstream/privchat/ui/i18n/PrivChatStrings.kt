package com.netonstream.privchat.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.gearui.i18n.normalizeLanguageTag
import com.gearui.i18n.resolveLanguagePack

/**
 * PrivChat 多语言字符串定义
 *
 * 参照 gearui-kit 的 I18n 架构实现
 */
@Immutable
data class PrivChatStrings(
    // ========== 通用 ==========
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

    // ========== 导航 ==========
    val tabConversation: String,
    val tabContact: String,
    val tabMe: String,

    // ========== 会话列表 ==========
    val conversationTitle: String,
    val conversationEmpty: String,
    val conversationMuted: String,
    val conversationPinned: String,
    val conversationPin: String,
    val conversationUnpin: String,
    val conversationDraft: String,
    val conversationAtMe: String,
    val conversationAtAll: String,
    val conversationUnread: String,

    // ========== 消息 ==========
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

    // ========== 联系人 ==========
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

    // ========== 首页下拉菜单 ==========
    val menuCreateGroup: String,
    val menuAddFriend: String,
    val menuScan: String,
    val menuMyQrCode: String,

    // ========== 好友申请页面 ==========
    val friendRequestTitle: String,
    val friendRequestEmpty: String,
    val friendRequestAccept: String,
    val friendRequestReject: String,
    val friendRequestMessage: String,
    val friendRequestTime: String,
    val friendRequestAccepted: String,
    val friendRequestRejected: String,

    // ========== 搜索用户 ==========
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

    // ========== 用户详情 ==========
    val userProfileTitle: String,
    val userProfileUserId: String,
    val userProfileSystemAccount: String,
    val userProfileBio: String,
    val userProfileRemark: String,
    val userProfileRemarkPlaceholder: String,
    val userProfileNickname: String,
    val userProfileSendMessage: String,
    val userProfileAddFriend: String,
    val userProfileCannotAddSelf: String,
    val userProfileAcceptFriendRequest: String,
    val userProfileAdding: String,
    val userProfileRequestSent: String,
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

    // ========== 好友设置 ==========
    val friendSettingsTitle: String,
    val friendSettingsShare: String,
    val friendSettingsSpecialFollow: String,
    val friendSettingsDelete: String,

    // ========== 聊天设置 ==========
    val chatSettingsTitle: String,
    val chatSettingsGroupName: String,
    val chatSettingsGroupQrCode: String,
    val chatSettingsGroupMembers: String,
    val chatSettingsGroupManage: String,
    val chatSettingsMute: String,
    val chatSettingsPin: String,
    val chatSettingsLeaveGroup: String,
    val chatSettingsLeaveGroupConfirmTitle: String,
    val chatSettingsLeaveGroupConfirmMessage: String,

    // ========== 群组 ==========
    val groupMembers: String,
    val groupOwner: String,
    val groupAdmin: String,
    val groupMember: String,
    val groupCreate: String,
    val groupLeave: String,
    val groupDissolve: String,

    // ========== 设置 ==========
    val settingsTitle: String,
    val settingsProfile: String,
    val settingsUserId: String,
    val settingsNickname: String,
    val settingsSignature: String,
    val settingsGender: String,
    val settingsBirthday: String,
    val settingsNotSet: String,
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
    val settingsSectionGeneral: String,
    val settingsNotification: String,
    val settingsSectionHelp: String,
    val settingsHelp: String,
    val settingsFeedback: String,
    val settingsSwitchAccount: String,

    // ========== 好友权限 ==========
    val permissionAllowAny: String,
    val permissionDenyAny: String,
    val permissionRequireRequest: String,
    val permissionNone: String,

    // ========== 性别 ==========
    val genderUnknown: String,
    val genderMale: String,
    val genderFemale: String,

    // ========== 关于 ==========
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

    // ========== 联系我们 ==========
    val contactUsTitle: String,
    val contactUsSwitch: String,
    val contactUsQuestion: String,
    val contactUsServiceTime: String,
    val contactUsChatNow: String,

    // ========== 个人资料编辑 ==========
    val profileEditSave: String,
    val profileEditAvatarHint: String,

    // ========== 修改密码 ==========
    val changePasswordTitle: String,
    val changePasswordNew: String,
    val changePasswordConfirm: String,
    val changePasswordSmsCode: String,
    val changePasswordSendCode: String,
    val changePasswordSubmit: String,
    val changePasswordSuccess: String,
    val changePasswordMismatch: String,

    // ========== 设置密码（首次） ==========
    val setPasswordTitle: String,
    val setPasswordHint: String,
    val setPasswordSubmit: String,
    val setPasswordSuccess: String,

    // ========== 设置昵称（首次） ==========
    val setNicknameTitle: String,
    val setNicknameHint: String,
    val setNicknameSubmit: String,

    // ========== 时间 ==========
    val timeYesterday: String,
    val timeToday: String,
    val timeSunday: String,
    val timeMonday: String,
    val timeTuesday: String,
    val timeWednesday: String,
    val timeThursday: String,
    val timeFriday: String,
    val timeSaturday: String,
)

/**
 * 内置语言包
 */
object PrivChatStringSets {

    val Chinese = PrivChatStrings(
        // 通用
        appName = "PrivChat",
        confirm = "确认",
        cancel = "取消",
        save = "保存",
        edit = "编辑",
        delete = "删除",
        search = "搜索",
        loading = "加载中...",
        retry = "重试",
        noData = "暂无数据",
        networkError = "网络错误",

        // 导航
        tabConversation = "消息",
        tabContact = "联系人",
        tabMe = "我",

        // 会话列表
        conversationTitle = "消息",
        conversationEmpty = "暂无会话",
        conversationMuted = "已免打扰",
        conversationPinned = "已置顶",
        conversationPin = "置顶",
        conversationUnpin = "取消置顶",
        conversationDraft = "[草稿]",
        conversationAtMe = "[有人@我]",
        conversationAtAll = "[@所有人]",
        conversationUnread = "[未读]",

        // 消息
        messageImage = "[图片]",
        messageVideo = "[视频]",
        messageVoice = "[语音]",
        messageFile = "[文件]",
        messageLocation = "[位置]",
        messageSticker = "[表情]",
        messageRevoked = "消息已撤回",
        messageSystem = "[系统消息]",
        messageUnknown = "[未知消息]",
        messageSending = "发送中...",
        messageSendFailed = "发送失败",
        messageInputHint = "输入消息...",
        messageVoiceHint = "按住说话",
        presenceOnline = "在线",
        presenceOffline = "离线",
        presenceLastSeenPrefix = "最近在线",

        // 联系人
        contactTitle = "联系人",
        contactFriends = "好友",
        contactGroups = "群组",
        contactEmpty = "暂无联系人",
        contactNewFriend = "新的朋友",
        contactFriendRequest = "好友申请",
        contactAddFriend = "添加好友",
        contactDeleteFriend = "删除好友",
        contactMyGroups = "我的群组",
        contactGroupsEmpty = "暂无群组",
        contactSearchUser = "搜索用户",
        myQrCodeTitle = "我的二维码",

        // 首页下拉菜单
        menuCreateGroup = "发起群聊",
        menuAddFriend = "添加好友",
        menuScan = "扫一扫",
        menuMyQrCode = "我的二维码",

        // 好友申请页面
        friendRequestTitle = "好友申请",
        friendRequestEmpty = "暂无好友申请",
        friendRequestAccept = "同意",
        friendRequestReject = "拒绝",
        friendRequestMessage = "验证消息",
        friendRequestTime = "申请时间",
        friendRequestAccepted = "已同意",
        friendRequestRejected = "已拒绝",

        // 搜索用户
        searchUserTitle = "添加朋友",
        searchUserPlaceholder = "账号/手机号",
        searchUserSearching = "搜索中...",
        searchUserNoResult = "未找到用户",
        searchUserTryAgain = "请尝试其他关键词",
        searchUserHint = "输入用户名后按回车搜索",
        searchUserError = "搜索失败",
        searchUserScan = "扫一扫",
        searchUserPhoneContacts = "手机联系人",
        searchUserMyQrCode = "我的二维码",
        searchUserMyAccount = "我的账号",

        // 用户详情
        userProfileTitle = "用户资料",
        userProfileUserId = "用户 ID",
        userProfileSystemAccount = "系统账号",
        userProfileBio = "简介",
        userProfileRemark = "备注",
        userProfileRemarkPlaceholder = "请输入备注名",
        userProfileNickname = "昵称",
        userProfileSendMessage = "发消息",
        userProfileAddFriend = "添加好友",
        userProfileCannotAddSelf = "不能添加自己为好友",
        userProfileAcceptFriendRequest = "同意好友申请",
        userProfileAdding = "添加中...",
        userProfileRequestSent = "好友申请已发送，请等待对方确认",
        userProfileDeleteFriend = "删除好友",
        userProfileDeleting = "删除中...",
        userProfileBlockUser = "加入黑名单",
        userProfileBlocking = "处理中...",
        userProfileDeleteFriendConfirmTitle = "删除好友",
        userProfileDeleteFriendConfirmMessage = "确定要删除该好友吗？删除后将无法收到对方消息。",
        userProfileBlockUserConfirmTitle = "加入黑名单",
        userProfileBlockUserConfirmMessage = "确定要将该用户加入黑名单吗？加入后将无法收到对方消息。",
        friendRequestInputTitle = "添加好友",
        friendRequestInputPlaceholder = "请输入验证信息（选填）",
        friendRequestSentTitle = "发送成功",

        // 好友设置
        friendSettingsTitle = "好友设置",
        friendSettingsShare = "推荐给朋友",
        friendSettingsSpecialFollow = "特别关注",
        friendSettingsDelete = "删除联系人",

        // 聊天设置
        chatSettingsTitle = "聊天设置",
        chatSettingsGroupName = "群名称",
        chatSettingsGroupQrCode = "群二维码",
        chatSettingsGroupMembers = "群成员",
        chatSettingsGroupManage = "群管理",
        chatSettingsMute = "消息免打扰",
        chatSettingsPin = "置顶聊天",
        chatSettingsLeaveGroup = "退出群聊",
        chatSettingsLeaveGroupConfirmTitle = "退出群聊",
        chatSettingsLeaveGroupConfirmMessage = "确定要退出该群聊吗？退出后将无法接收该群的消息。",

        // 群组
        groupMembers = "群成员",
        groupOwner = "群主",
        groupAdmin = "管理员",
        groupMember = "成员",
        groupCreate = "创建群组",
        groupLeave = "退出群组",
        groupDissolve = "解散群组",

        // 设置
        settingsTitle = "设置",
        settingsProfile = "个人资料",
        settingsUserId = "用户 ID",
        settingsNickname = "昵称",
        settingsSignature = "个性签名",
        settingsGender = "性别",
        settingsBirthday = "生日",
        settingsNotSet = "未设置",
        settingsFriendPermission = "好友权限",
        settingsAppearance = "外观",
        settingsLanguage = "语言",
        settingsLightTheme = "浅色",
        settingsDarkTheme = "深色",
        settingsSystemTheme = "跟随系统",
        settingsAbout = "关于",
        settingsMore = "更多",
        settingsLogout = "退出登录",
        settingsSectionAccount = "账号",
        settingsAccountSecurity = "账号安全",
        settingsPrivacy = "隐私设置",
        settingsSectionGeneral = "通用",
        settingsNotification = "通知",
        settingsSectionHelp = "帮助",
        settingsHelp = "帮助中心",
        settingsFeedback = "意见反馈",
        settingsSwitchAccount = "切换账号",

        // 好友权限
        permissionAllowAny = "允许任何人",
        permissionDenyAny = "拒绝任何人",
        permissionRequireRequest = "需要验证",
        permissionNone = "无",

        // 性别
        genderUnknown = "未知",
        genderMale = "男",
        genderFemale = "女",

        // 关于
        aboutTitle = "关于",
        aboutSdkVersion = "SDK 版本",
        aboutVersion = "版本",
        aboutPrivacyPolicy = "隐私政策",
        aboutUserAgreement = "用户协议",
        aboutDisclaimer = "免责声明",
        aboutContactUs = "联系我们",
        aboutOfficialWebsite = "官方网站",
        aboutSourceCode = "源代码",
        aboutCopyright = "Copyright © 2024 PrivChat. All Rights Reserved.",

        // 联系我们
        contactUsTitle = "联系我们",
        contactUsSwitch = "切换渠道",
        contactUsQuestion = "如有问题，请通过以下方式联系我们",
        contactUsServiceTime = "服务时间：工作日 9:00 - 18:00",
        contactUsChatNow = "立即联系",

        // 个人资料编辑
        profileEditSave = "保存",
        profileEditAvatarHint = "点击修改头像",

        // 修改密码
        changePasswordTitle = "修改密码",
        changePasswordNew = "新密码",
        changePasswordConfirm = "确认密码",
        changePasswordSmsCode = "验证码",
        changePasswordSendCode = "发送验证码",
        changePasswordSubmit = "确认修改",
        changePasswordSuccess = "密码修改成功",
        changePasswordMismatch = "两次密码不一致",

        // 设置密码（首次）
        setPasswordTitle = "设置密码",
        setPasswordHint = "设置密码后可使用密码登录",
        setPasswordSubmit = "确认设置",
        setPasswordSuccess = "密码设置成功",

        // 设置昵称（首次）
        setNicknameTitle = "设置昵称",
        setNicknameHint = "给自己取个名字吧",
        setNicknameSubmit = "完成",

        // 时间
        timeYesterday = "昨天",
        timeToday = "今天",
        timeSunday = "周日",
        timeMonday = "周一",
        timeTuesday = "周二",
        timeWednesday = "周三",
        timeThursday = "周四",
        timeFriday = "周五",
        timeSaturday = "周六",
    )

    val English = PrivChatStrings(
        // Common
        appName = "PrivChat",
        confirm = "Confirm",
        cancel = "Cancel",
        save = "Save",
        edit = "Edit",
        delete = "Delete",
        search = "Search",
        loading = "Loading...",
        retry = "Retry",
        noData = "No data",
        networkError = "Network error",

        // Navigation
        tabConversation = "Messages",
        tabContact = "Contacts",
        tabMe = "Me",

        // Conversation list
        conversationTitle = "Messages",
        conversationEmpty = "No conversations",
        conversationMuted = "Muted",
        conversationPinned = "Pinned",
        conversationPin = "Pin",
        conversationUnpin = "Unpin",
        conversationDraft = "[Draft]",
        conversationAtMe = "[@Me]",
        conversationAtAll = "[@All]",
        conversationUnread = "[Unread]",

        // Messages
        messageImage = "[Image]",
        messageVideo = "[Video]",
        messageVoice = "[Voice]",
        messageFile = "[File]",
        messageLocation = "[Location]",
        messageSticker = "[Sticker]",
        messageRevoked = "Message recalled",
        messageSystem = "[System]",
        messageUnknown = "[Unknown]",
        messageSending = "Sending...",
        messageSendFailed = "Send failed",
        messageInputHint = "Type a message...",
        messageVoiceHint = "Hold to talk",
        presenceOnline = "Online",
        presenceOffline = "Offline",
        presenceLastSeenPrefix = "Last seen",

        // Contacts
        contactTitle = "Contacts",
        contactFriends = "Friends",
        contactGroups = "Groups",
        contactEmpty = "No contacts",
        contactNewFriend = "New Friends",
        contactFriendRequest = "Friend Requests",
        contactAddFriend = "Add Friend",
        contactDeleteFriend = "Delete Friend",
        contactMyGroups = "My Groups",
        contactGroupsEmpty = "No groups",
        contactSearchUser = "Search User",
        myQrCodeTitle = "My QR Code",

        // Home dropdown menu
        menuCreateGroup = "New Group Chat",
        menuAddFriend = "Add Friend",
        menuScan = "Scan",
        menuMyQrCode = "My QR Code",

        // Friend Request Page
        friendRequestTitle = "Friend Requests",
        friendRequestEmpty = "No friend requests",
        friendRequestAccept = "Accept",
        friendRequestReject = "Reject",
        friendRequestMessage = "Message",
        friendRequestTime = "Request time",
        friendRequestAccepted = "Accepted",
        friendRequestRejected = "Rejected",

        // Search User
        searchUserTitle = "Add Friend",
        searchUserPlaceholder = "Account/Phone",
        searchUserSearching = "Searching...",
        searchUserNoResult = "User not found",
        searchUserTryAgain = "Try another keyword",
        searchUserHint = "Enter username and press Enter to search",
        searchUserError = "Search failed",
        searchUserScan = "Scan QR Code",
        searchUserPhoneContacts = "Phone Contacts",
        searchUserMyQrCode = "My QR Code",
        searchUserMyAccount = "My Account",

        // User Profile
        userProfileTitle = "User Profile",
        userProfileUserId = "User ID",
        userProfileSystemAccount = "System Account",
        userProfileBio = "Bio",
        userProfileRemark = "Remark",
        userProfileRemarkPlaceholder = "Enter a remark name",
        userProfileNickname = "Nickname",
        userProfileSendMessage = "Send Message",
        userProfileAddFriend = "Add Friend",
        userProfileCannotAddSelf = "Cannot add yourself as friend",
        userProfileAcceptFriendRequest = "Accept Friend Request",
        userProfileAdding = "Adding...",
        userProfileRequestSent = "Friend request sent, please wait for confirmation",
        userProfileDeleteFriend = "Delete Friend",
        userProfileDeleting = "Deleting...",
        userProfileBlockUser = "Block User",
        userProfileBlocking = "Processing...",
        userProfileDeleteFriendConfirmTitle = "Delete Friend",
        userProfileDeleteFriendConfirmMessage = "Are you sure you want to delete this friend? You will no longer receive messages from them.",
        userProfileBlockUserConfirmTitle = "Block User",
        userProfileBlockUserConfirmMessage = "Are you sure you want to block this user? You will no longer receive messages from them.",
        friendRequestInputTitle = "Add Friend",
        friendRequestInputPlaceholder = "Enter verification message (optional)",
        friendRequestSentTitle = "Sent Successfully",

        // Friend Settings
        friendSettingsTitle = "Friend Settings",
        friendSettingsShare = "Share to Friends",
        friendSettingsSpecialFollow = "Special Follow",
        friendSettingsDelete = "Delete Contact",

        // Chat Settings
        chatSettingsTitle = "Chat Settings",
        chatSettingsGroupName = "Group Name",
        chatSettingsGroupQrCode = "Group QR Code",
        chatSettingsGroupMembers = "Group Members",
        chatSettingsGroupManage = "Group Management",
        chatSettingsMute = "Mute Notifications",
        chatSettingsPin = "Pin Chat",
        chatSettingsLeaveGroup = "Leave Group",
        chatSettingsLeaveGroupConfirmTitle = "Leave Group",
        chatSettingsLeaveGroupConfirmMessage = "Are you sure you want to leave this group? You will no longer receive messages from this group.",

        // Groups
        groupMembers = "Members",
        groupOwner = "Owner",
        groupAdmin = "Admin",
        groupMember = "Member",
        groupCreate = "Create Group",
        groupLeave = "Leave Group",
        groupDissolve = "Dissolve Group",

        // Settings
        settingsTitle = "Settings",
        settingsProfile = "Profile",
        settingsUserId = "User ID",
        settingsNickname = "Nickname",
        settingsSignature = "Signature",
        settingsGender = "Gender",
        settingsBirthday = "Birthday",
        settingsNotSet = "Not set",
        settingsFriendPermission = "Friend Permission",
        settingsAppearance = "Appearance",
        settingsLanguage = "Language",
        settingsLightTheme = "Light",
        settingsDarkTheme = "Dark",
        settingsSystemTheme = "System",
        settingsAbout = "About",
        settingsMore = "More",
        settingsLogout = "Log out",
        settingsSectionAccount = "Account",
        settingsAccountSecurity = "Account Security",
        settingsPrivacy = "Privacy",
        settingsSectionGeneral = "General",
        settingsNotification = "Notifications",
        settingsSectionHelp = "Help",
        settingsHelp = "Help Center",
        settingsFeedback = "Feedback",
        settingsSwitchAccount = "Switch Account",

        // Friend permission
        permissionAllowAny = "Allow anyone",
        permissionDenyAny = "Deny anyone",
        permissionRequireRequest = "Require request",
        permissionNone = "None",

        // Gender
        genderUnknown = "Unknown",
        genderMale = "Male",
        genderFemale = "Female",

        // About
        aboutTitle = "About",
        aboutSdkVersion = "SDK Version",
        aboutVersion = "Version",
        aboutPrivacyPolicy = "Privacy Policy",
        aboutUserAgreement = "User Agreement",
        aboutDisclaimer = "Disclaimer",
        aboutContactUs = "Contact Us",
        aboutOfficialWebsite = "Official Website",
        aboutSourceCode = "Source Code",
        aboutCopyright = "Copyright © 2024 PrivChat. All Rights Reserved.",

        // Contact Us
        contactUsTitle = "Contact Us",
        contactUsSwitch = "Switch channel",
        contactUsQuestion = "If you have any questions, please contact us",
        contactUsServiceTime = "Service hours: Weekdays 9:00 - 18:00",
        contactUsChatNow = "Chat Now",

        // Profile Edit
        profileEditSave = "Save",
        profileEditAvatarHint = "Tap to change avatar",

        // Change Password
        changePasswordTitle = "Change Password",
        changePasswordNew = "New Password",
        changePasswordConfirm = "Confirm Password",
        changePasswordSmsCode = "Verification Code",
        changePasswordSendCode = "Send Code",
        changePasswordSubmit = "Submit",
        changePasswordSuccess = "Password changed successfully",
        changePasswordMismatch = "Passwords don't match",

        // Set Password (first time)
        setPasswordTitle = "Set Password",
        setPasswordHint = "Set a password to log in with your account",
        setPasswordSubmit = "Confirm",
        setPasswordSuccess = "Password set successfully",

        // Set Nickname (first time)
        setNicknameTitle = "Set Nickname",
        setNicknameHint = "Give yourself a name",
        setNicknameSubmit = "Done",

        // Time
        timeYesterday = "Yesterday",
        timeToday = "Today",
        timeSunday = "Sun",
        timeMonday = "Mon",
        timeTuesday = "Tue",
        timeWednesday = "Wed",
        timeThursday = "Thu",
        timeFriday = "Fri",
        timeSaturday = "Sat",
    )

    val ChineseTraditional: PrivChatStrings = PrivChatStringsZhHant
    val Vietnamese: PrivChatStrings = PrivChatStringsViVn

    const val DEFAULT_LANGUAGE_TAG = "zh-Hans"
    const val CHINESE_SIMPLIFIED_TAG = "zh-Hans"
    const val CHINESE_TRADITIONAL_TAG = "zh-Hant"
    const val ENGLISH_TAG = "en-US"
    const val VIETNAMESE_TAG = "vi-VN"

    val builtIn: Map<String, PrivChatStrings> = mapOf(
        CHINESE_SIMPLIFIED_TAG to Chinese,
        "zh" to Chinese,
        "zh-CN" to Chinese,
        "zh-Hans-CN" to Chinese,
        CHINESE_TRADITIONAL_TAG to ChineseTraditional,
        "zh-TW" to ChineseTraditional,
        "zh-HK" to ChineseTraditional,
        ENGLISH_TAG to English,
        "en" to English,
        VIETNAMESE_TAG to Vietnamese,
        "vi" to Vietnamese,
    )
}

/**
 * CompositionLocal 存储当前语言包
 */
val LocalPrivChatStrings = staticCompositionLocalOf {
    PrivChatStringSets.Chinese  // 默认中文
}

val LocalPrivChatLanguageTag = staticCompositionLocalOf {
    PrivChatStringSets.CHINESE_SIMPLIFIED_TAG
}

/**
 * PrivChat 国际化包装 Composable
 */
@Composable
fun PrivChatI18n(
    strings: PrivChatStrings = PrivChatStringSets.Chinese,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalPrivChatStrings provides strings,
        LocalPrivChatLanguageTag provides PrivChatStringSets.CHINESE_SIMPLIFIED_TAG,
        content = content
    )
}

@Composable
fun PrivChatI18n(
    languageTag: String,
    packs: Map<String, PrivChatStrings> = PrivChatStringSets.builtIn,
    defaultTag: String = PrivChatStringSets.DEFAULT_LANGUAGE_TAG,
    content: @Composable () -> Unit
) {
    val normalizedTag = remember(languageTag) { normalizeLanguageTag(languageTag) }
    val strings = remember(normalizedTag, packs, defaultTag) {
        resolveLanguagePack(
            languageTag = normalizedTag,
            packs = packs,
            defaultTag = defaultTag,
        )
    }
    CompositionLocalProvider(
        LocalPrivChatStrings provides strings,
        LocalPrivChatLanguageTag provides normalizedTag,
        content = content
    )
}

/**
 * 获取当前语言包的便捷访问器
 */
object PrivChatI18n {
    val strings: PrivChatStrings
        @Composable get() = LocalPrivChatStrings.current

    val languageTag: String
        @Composable get() = LocalPrivChatLanguageTag.current
}
