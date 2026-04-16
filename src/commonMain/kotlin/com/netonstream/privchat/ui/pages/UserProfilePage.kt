package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.*
import om.netonstream.privchat.sdk.dto.UserEntry
import om.netonstream.privchat.sdk.dto.FriendEntry
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
import com.gearui.foundation.primitives.GearLazyColumn
import com.gearui.foundation.typography.Typography
import com.gearui.foundation.AvatarSpecs
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.*
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
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    val isSystemUser = user.userType.toInt() == 1
    var isAddingFriend by remember { mutableStateOf(false) }
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
        GearLazyColumn(modifier = Modifier.fillMaxSize()) {
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
                        size = AvatarSpecs.Size.large,
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // 右侧信息
                    Column(modifier = Modifier.weight(1f)) {
                        // 昵称
                        Text(
                            text = user.nickname ?: user.username,
                            style = Typography.TitleLarge,
                            color = Theme.colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // 账号
                        Text(
                            text = if (isSystemUser) {
                                strings.userProfileSystemAccount
                            } else {
                                "${strings.userProfileUserId}: ${user.username}"
                            },
                            style = Typography.BodySmall,
                            color = Theme.colors.textSecondary
                        )
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
                    } else {
                        // 非好友，显示添加好友按钮
                        Button(
                            text = if (isAddingFriend) {
                                strings.userProfileAdding
                            } else if (isFromFriendRequest) {
                                strings.userProfileAcceptFriendRequest
                            } else {
                                strings.userProfileAddFriend
                            },
                            type = ButtonType.FILL,
                            theme = ButtonTheme.PRIMARY,
                            onClick = {
                                if (!isAddingFriend) {
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
                                                    Toast.error(error.message ?: strings.networkError)
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
                                    Toast.success(strings.userProfileRequestSent)
                                    onBackToRoot()
                                },
                                onFailure = { error ->
                                    isAddingFriend = false
                                    Toast.error(error.message ?: strings.networkError)
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
        GearLazyColumn(modifier = Modifier.fillMaxSize()) {
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
                        size = AvatarSpecs.Size.large,
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // 右侧信息
                    Column(modifier = Modifier.weight(1f)) {
                        val remark = friend.remark
                        // 备注名（如果有）
                        if (!remark.isNullOrBlank()) {
                            Text(
                                text = remark,
                                style = Typography.TitleLarge,
                                color = Theme.colors.textPrimary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // 昵称（作为副标题）
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${strings.userProfileNickname}: ",
                                    style = Typography.BodySmall,
                                    color = Theme.colors.textSecondary
                                )
                                Text(
                                    text = friend.nickname ?: friend.username,
                                    style = Typography.BodySmall,
                                    color = Theme.colors.textSecondary
                                )
                            }
                        } else {
                            // 没有备注，昵称作为主标题
                            Text(
                                text = friend.nickname ?: friend.username,
                                style = Typography.TitleLarge,
                                color = Theme.colors.textPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // 账号
                        Text(
                            text = "${strings.userProfileUserId}: ${friend.username}",
                            style = Typography.BodySmall,
                            color = Theme.colors.textSecondary
                        )
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
