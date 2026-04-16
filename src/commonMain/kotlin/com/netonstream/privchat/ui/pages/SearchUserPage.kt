package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.*
import om.netonstream.privchat.sdk.dto.UserEntry
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.gearui.components.navbar.NavBar
import com.gearui.components.searchbar.SearchBar
import com.gearui.components.cell.Cell
import com.gearui.components.empty.EmptyState
import com.gearui.components.dialog.Dialog
import com.gearui.components.dialog.DialogContent
import com.gearui.components.button.Button
import com.gearui.components.button.ButtonType
import com.gearui.components.button.ButtonTheme
import com.gearui.components.button.ButtonSize
import com.gearui.theme.Theme
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.primitives.GearLazyColumn
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 搜索用户页面
 *
 * @param myAccount 我的账号
 * @param onBack 返回回调
 * @param onSearch 搜索回调，返回搜索结果
 * @param onUserFound 找到用户后的回调，传入第一个找到的用户
 * @param onScanQrCode 扫一扫回调
 * @param onPhoneContacts 手机联系人回调
 * @param onMyQrCode 我的二维码回调
 * @param modifier Modifier
 */
@Composable
fun SearchUserPage(
    myAccount: String = "",
    onBack: () -> Unit,
    onSearch: suspend (String) -> Result<List<UserEntry>>,
    onUserFound: (UserEntry) -> Unit,
    onScanQrCode: () -> Unit = {},
    onPhoneContacts: () -> Unit = {},
    onMyQrCode: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    val colors = Theme.colors
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var noResult by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // 执行搜索
    fun doSearch() {
        if (searchQuery.isNotBlank() && !isSearching) {
            isSearching = true
            errorMessage = null
            noResult = false

            scope.launch {
                onSearch(searchQuery.trim()).fold(
                    onSuccess = { users ->
                        isSearching = false
                        if (users.isNotEmpty()) {
                            // 找到用户，跳转到第一个用户的详情页
                            onUserFound(users.first())
                        } else {
                            noResult = true
                        }
                    },
                    onFailure = { error ->
                        isSearching = false
                        errorMessage = error.message ?: strings.searchUserError
                    }
                )
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        // 顶部导航栏
        NavBar(
            title = strings.searchUserTitle,
            useDefaultBack = true,
            onBackClick = onBack,
        )

        // 搜索栏 - 方形居中
        SearchBar(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                errorMessage = null
                noResult = false
            },
            placeholder = strings.searchUserPlaceholder,
            showCancel = true,
            onCancel = onBack,
            onSearch = { doSearch() },
            shape = com.gearui.components.searchbar.SearchBarShape.SQUARE,
            alignment = com.gearui.components.searchbar.SearchBarAlignment.CENTER,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // 内容区域
        if (isSearching || noResult) {
            // 搜索状态显示
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isSearching -> {
                        Text(
                            text = strings.searchUserSearching,
                            color = colors.textSecondary
                        )
                    }

                    noResult -> {
                        EmptyState(
                            message = strings.searchUserNoResult,
                            description = strings.searchUserTryAgain
                        )
                    }
                }
            }
        } else {
            // 功能入口列表
            GearLazyColumn(modifier = Modifier.fillMaxSize()) {
                // 功能入口
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 扫一扫
                item {
                    Cell(
                        title = strings.searchUserScan,
                        arrow = true,
                        onClick = onScanQrCode,
                    )
                }

                // 手机联系人
                item {
                    Cell(
                        title = strings.searchUserPhoneContacts,
                        arrow = true,
                        onClick = onPhoneContacts,
                    )
                }

                // 分隔
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 我的二维码
                item {
                    Cell(
                        title = strings.searchUserMyQrCode,
                        arrow = true,
                        onClick = onMyQrCode,
                    )
                }

                // 我的账号
                item {
                    Cell(
                        title = strings.searchUserMyAccount,
                        note = myAccount,
                    )
                }

                // 底部间距
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    Dialog.Host(
        visible = errorMessage != null,
        dismissOnOutside = true,
        onDismiss = { errorMessage = null }
    ) {
        DialogContent(
            title = strings.networkError,
            message = errorMessage ?: strings.searchUserError,
            actions = {
                Button(
                    text = strings.confirm,
                    type = ButtonType.FILL,
                    theme = ButtonTheme.PRIMARY,
                    size = ButtonSize.SMALL,
                    onClick = { errorMessage = null }
                )
            }
        )
    }
}
