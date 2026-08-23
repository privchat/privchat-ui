package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.*
import com.netonstream.privchat.sdk.dto.UserEntry
import com.netonstream.privchat.sdk.dto.FriendEntry
import com.netonstream.privchat.ui.error.UserFacingError
import com.netonstream.privchat.ui.components.ChatAvatar
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.gearui.components.icon.Icons
import com.gearui.components.navbar.NavBar
import com.gearui.components.navbar.NavBarItem
import com.gearui.components.button.Button
import com.gearui.components.button.ButtonType
import com.gearui.components.button.ButtonTheme
import com.gearui.components.button.ButtonSize
import com.gearui.components.cell.Cell
import com.gearui.components.input.Input
import com.gearui.components.dialog.Dialog
import com.gearui.components.dialog.DialogContent
import com.gearui.components.toast.Toast

import com.gearui.theme.Theme
import com.gearui.foundation.primitives.Text
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.gearui.foundation.typography.Typography
import com.gearui.foundation.avatar.AvatarSizeTokens
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 用户详情页面（搜索到的用户）
 *
 * @param user 用户信息
 * @param isFriend 是否已是好友
 * @param onBack 返回回调
 * @param onBackToRoot 返回根页面回调
 * @param onSendMessage 发送消息回调
 * @param onAddFriend 添加好友回调，接收申请理由
 * @param modifier Modifier
 */
@Composable
fun UserProfilePage(
    user: UserEntry,
    isFriend: Boolean = false,
    isSelf: Boolean = false,
    isFromFriendRequest: Boolean = false,
    onBack: () -> Unit,
    onBackToRoot: () -> Unit = onBack,
    onSendMessage: () -> Unit = {},
    onAddFriend: suspend (remark: String?) -> Result<ULong> = { Result.failure(NotImplementedError()) },
    onAcceptFriendRequest: suspend () -> Result<ULong> = { Result.failure(NotImplementedError()) },
    /**
     * Bot 关注回调（spec `02-server/SERVICE_ACCOUNT_FOLLOW_SPEC` §3.1）。
     * 仅在 `user.userType == 2` 时触发；成功后由调用方决定 navigate（典型用法：
     * 先调 [onSendMessage] 打开会话）。
     */
    onFollowBot: suspend () -> Result<Unit> = { Result.failure(NotImplementedError()) },
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    val isSystemUser = user.userType.toInt() == 1
    val isBot = user.userType.toInt() == 2
    var isAddingFriend by remember { mutableStateOf(false) }
    // 本地已发送标记：发送成功后保留在页面上，按钮变"已发送"且 disable，
    // 避免重复发申请；同时也让 Toast 有空间渲染（不被 onBackToRoot 立刻打断）。
    var hasSentFriendRequest by remember(user.userId) { mutableStateOf(false) }
    var isFollowingBot by remember { mutableStateOf(false) }
    // 关注状态由 server 端 user 信息驱动；点关注成功后本地翻转，避免回退页面重进时重置
    var hasFollowedBot by remember(user.userId, user.isFollow) { mutableStateOf(user.isFollow) }
    val scope = rememberCoroutineScope()

    // Dialog 状态
    var showRemarkDialog by remember { mutableStateOf(false) }
    var remarkInput by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize().background(Theme.colors.background)) {
        // 顶部导航栏
        NavBar(
            title = strings.userProfileTitle,
            useDefaultBack = true,
            onBackClick = onBack,
        )

        // 可滚动内容
        // Keep profile actions out of the shared drag gesture wrapper.
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // 用户头像和信息区域 - 微信风格：左侧头像，右侧信息
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Theme.colors.surface)
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // 左侧头像
                    ChatAvatar(
                        url = user.avatarUrl,
                        name = user.nickname ?: user.username,
                        size = AvatarSizeTokens.Large.size,
                        userId = user.userId.toLong(),
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // 右侧信息
                    Column(modifier = Modifier.weight(1f)) {
                        // 昵称 + 用户类型标记（系统/机器人）
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.nickname ?: user.username,
                                style = Typography.TitleLarge,
                                color = Theme.colors.foreground
                            )
                            UserTypeBadge(
                                userType = user.userType,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }

                        // 账号：对外只展示昵称 + 用户名（自设 handle，空则整行隐藏）。
                        // 内部用户 ID 不对外展示（对用户无意义，也不泄露内部编号）。
                        val handle = user.username.takeIf { it.isNotBlank() }
                        if (isSystemUser) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = strings.userProfileSystemAccount,
                                style = Typography.BodySmall,
                                color = Theme.colors.mutedForeground
                            )
                        } else if (handle != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${strings.settingsUsername}: $handle",
                                style = Typography.BodySmall,
                                color = Theme.colors.mutedForeground
                            )
                        }
                    }
                }
            }

            // 分隔间距
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 操作按钮 - 独立区域
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Theme.colors.surface)
                        .padding(16.dp),
                ) {
                    if (isFriend || isSystemUser) {
                        // 已是好友或系统账号，显示发送消息按钮
                        Button(
                            text = strings.userProfileSendMessage,
                            type = ButtonType.FILL,
                            theme = ButtonTheme.PRIMARY,
                            onClick = onSendMessage,
                            block = true
                        )
                    } else if (isSelf) {
                        // 自己，显示不可用按钮
                        Button(
                            text = strings.userProfileCannotAddSelf,
                            type = ButtonType.FILL,
                            theme = ButtonTheme.DEFAULT,
                            onClick = {},
                            disabled = true,
                            block = true
                        )
                    } else if (isBot) {
                        // Bot（user_type=2）：双入口（与微信服务号 / Telegram bot 一致）。
                        //   - 主按钮"发消息"：直接 onSendMessage → channel/direct/get_or_create 拿 channel
                        //     →不写 follow 表（spec SERVICE_ACCOUNT_FOLLOW_SPEC §7：B 路径）
                        //   - 次按钮"关注 / 已关注"：onFollowBot → 写 follow 表 + 触发 application binding
                        //     →server 幂等；已关注后按钮变 disabled，避免误点
                        Button(
                            text = strings.userProfileSendMessage,
                            type = ButtonType.FILL,
                            theme = ButtonTheme.PRIMARY,
                            onClick = onSendMessage,
                            block = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            text = when {
                                isFollowingBot -> strings.userProfileFollowingBot
                                hasFollowedBot -> strings.userProfileFollowedBotToast
                                else -> strings.userProfileFollowBot
                            },
                            type = ButtonType.FILL,
                            theme = ButtonTheme.DEFAULT,
                            disabled = hasFollowedBot,
                            onClick = {
                                if (!isFollowingBot && !hasFollowedBot) {
                                    isFollowingBot = true
                                    scope.launch {
                                        onFollowBot().fold(
                                            onSuccess = {
                                                isFollowingBot = false
                                                hasFollowedBot = true
                                                Toast.success(strings.userProfileFollowedBotToast)
                                            },
                                            onFailure = { error ->
                                                isFollowingBot = false
                                                Toast.error(UserFacingError.message(error, strings.networkError))
                                            }
                                        )
                                    }
                                }
                            },
                            block = true
                        )
                    } else {
                        // 非好友，显示添加好友按钮。三个互斥状态：
                        //   - isAddingFriend：网络中（loading）
                        //   - hasSentFriendRequest：刚发完，结果"已发送"，按钮 disable 防重复
                        //   - 默认：可点击
                        Button(
                            text = when {
                                isAddingFriend -> strings.userProfileAdding
                                hasSentFriendRequest -> strings.userProfileRequestSent
                                isFromFriendRequest -> strings.userProfileAcceptFriendRequest
                                else -> strings.userProfileAddFriend
                            },
                            type = ButtonType.FILL,
                            theme = ButtonTheme.PRIMARY,
                            disabled = hasSentFriendRequest,
                            onClick = {
                                if (!isAddingFriend && !hasSentFriendRequest) {
                                    if (isFromFriendRequest) {
                                        isAddingFriend = true
                                        scope.launch {
                                            onAcceptFriendRequest().fold(
                                                onSuccess = {
                                                    isAddingFriend = false
                                                    onSendMessage()
                                                },
                                                onFailure = { error ->
                                                    isAddingFriend = false
                                                    Toast.error(UserFacingError.message(error, strings.networkError))
                                                }
                                            )
                                        }
                                    } else {
                                        // 显示输入申请理由的 Dialog
                                        remarkInput = ""
                                        showRemarkDialog = true
                                    }
                                }
                            },
                            block = true
                        )
                    }
                }
            }

            // 底部间距
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // 输入申请理由的 Dialog
    Dialog.Host(
        visible = showRemarkDialog,
        dismissOnOutside = true,
        onDismiss = { showRemarkDialog = false }
    ) {
        DialogContent(
            title = strings.friendRequestInputTitle,
            content = {
                Input(
                    value = remarkInput,
                    onValueChange = { remarkInput = it },
                    placeholder = strings.friendRequestInputPlaceholder,
                    maxLines = 3
                )
            },
            actions = {
                Button(
                    text = strings.cancel,
                    type = ButtonType.TEXT,
                    theme = ButtonTheme.DEFAULT,
                    size = ButtonSize.SMALL,
                    onClick = { showRemarkDialog = false }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    text = strings.confirm,
                    type = ButtonType.FILL,
                    theme = ButtonTheme.PRIMARY,
                    size = ButtonSize.SMALL,
                    onClick = {
                        showRemarkDialog = false
                        isAddingFriend = true
                        scope.launch {
                            onAddFriend(remarkInput.ifBlank { null }).fold(
                                onSuccess = {
                                    isAddingFriend = false
                                    hasSentFriendRequest = true
                                    Toast.success(strings.userProfileRequestSent)
                                    // 不再立即 onBackToRoot——避免 page unmount + scope cancel
                                    // 跟 Toast 的 overlay LaunchedEffect 抢渲染，导致用户看不到提示。
                                    // 按钮已切到"申请已发送"disable 态，用户读完 toast 自行返回。
                                },
                                onFailure = { error ->
                                    isAddingFriend = false
                                    // 20311 GroupAddFriendDisabled：群业务策略禁止成员互加好友，
                                    // 给明确文案而不是笼统的网络错误（ERROR_CODE_SPEC）。
                                    val msg = if (UserFacingError.serverReasonCode(error) == 20311) {
                                        strings.userProfileGroupAddFriendDisabled
                                    } else {
                                        UserFacingError.message(error, strings.networkError)
                                    }
                                    Toast.error(msg)
                                }
                            )
                        }
                    }
                )
            }
        )
    }
}

/**
 * 好友详情页面
 *
 * @param friend 好友信息
 * @param onBack 返回回调
 * @param onSendMessage 发送消息回调
 * @param onFriendSettings 好友设置回调
 * @param modifier Modifier
 */
@Composable
fun FriendProfilePage(
    friend: FriendEntry,
    onBack: () -> Unit,
    onSendMessage: () -> Unit = {},
    onFriendSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings

    Column(modifier = modifier.fillMaxSize().background(Theme.colors.background)) {
        // 顶部导航栏
        NavBar(
            title = strings.userProfileTitle,
            useDefaultBack = true,
            onBackClick = onBack,
            rightItems = listOf(
                NavBarItem(icon = Icons.more_horiz, onClick = onFriendSettings)
            ),
        )

        // 可滚动内容
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // 好友头像和信息区域 - 微信风格：左侧头像，右侧信息
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Theme.colors.surface)
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // 左侧头像
                    ChatAvatar(
                        url = friend.avatarUrl,
                        name = friend.remark ?: friend.nickname ?: friend.username,
                        size = AvatarSizeTokens.Large.size,
                        userId = friend.userId.toLong(),
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // 右侧信息
                    Column(modifier = Modifier.weight(1f)) {
                        val remark = friend.remark
                        // 备注名（如果有）
                        if (!remark.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = remark,
                                    style = Typography.TitleLarge,
                                    color = Theme.colors.foreground
                                )
                                UserTypeBadge(
                                    userType = friend.userType,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // 昵称（作为副标题）
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${strings.userProfileNickname}: ",
                                    style = Typography.BodySmall,
                                    color = Theme.colors.mutedForeground
                                )
                                Text(
                                    text = friend.nickname ?: friend.username,
                                    style = Typography.BodySmall,
                                    color = Theme.colors.mutedForeground
                                )
                            }
                        } else {
                            // 没有备注，昵称作为主标题
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = friend.nickname ?: friend.username,
                                    style = Typography.TitleLarge,
                                    color = Theme.colors.foreground
                                )
                                UserTypeBadge(
                                    userType = friend.userType,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }

                        // 账号：用户名有值才展示；内部用户 ID 不对外展示（同陌生人视图）。
                        val friendHandle = friend.username.takeIf { it.isNotBlank() }
                        if (friendHandle != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${strings.settingsUsername}: $friendHandle",
                                style = Typography.BodySmall,
                                color = Theme.colors.mutedForeground
                            )
                        }
                    }
                }
            }

            // 分隔间距
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 操作按钮 - 独立区域
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Theme.colors.surface)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        text = strings.userProfileSendMessage,
                        type = ButtonType.FILL,
                        theme = ButtonTheme.PRIMARY,
                        onClick = onSendMessage,
                        block = true
                    )
                }
            }

            // 底部间距
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

/**
 * 用户类型标记：系统账号 / 机器人 显示彩色 pill；普通用户（userType=0）不展示。
 *
 * - userType=1 SYSTEM → warning 色系（橙黄，"官方/系统")
 * - userType=2 BOT    → primary 色系（品牌蓝，"自动化"）
 */
@Composable
private fun UserTypeBadge(userType: Short, modifier: Modifier = Modifier) {
    val strings = PrivChatI18n.strings
    val (label, fg, bg) = when (userType.toInt()) {
        1 -> Triple(strings.userBadgeSystem, Theme.colors.warning, Theme.colors.warning.copy(alpha = 0.12f))
        2 -> Triple(strings.userBadgeBot, Theme.colors.primary, Theme.colors.muted)
        else -> return
    }
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = Typography.Label,
            color = fg,
        )
    }
}
