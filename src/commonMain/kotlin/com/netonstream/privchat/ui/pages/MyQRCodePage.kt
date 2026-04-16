package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.*
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.gearui.theme.Theme
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.typography.Typography
import com.gearui.foundation.AvatarSpecs
import com.gearui.primitives.Avatar
import com.gearui.components.navbar.NavBar
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 我的二维码页面
 */
@Composable
fun MyQRCodePage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = PrivChatI18n.strings
    val currentUserId by PrivChat.currentUserId.collectAsState()
    val currentUserName by PrivChat.currentUserName.collectAsState()
    val displayName = currentUserName ?: currentUserId?.toString() ?: ""

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.background)
    ) {
        NavBar(
            title = strings.myQrCodeTitle,
            useDefaultBack = true,
            onBackClick = onBack,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            // QR Code card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Theme.colors.surface)
                    .padding(32.dp)
            ) {
                // Avatar
                Avatar(
                    text = displayName.firstOrNull()?.uppercase(),
                    size = AvatarSpecs.Size.large,
                    radius = AvatarSpecs.squareRadius,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Name
                Text(
                    text = displayName,
                    style = Typography.TitleMedium,
                    color = Theme.colors.textPrimary,
                )

                Spacer(modifier = Modifier.height(4.dp))

                // ID
                Text(
                    text = "ID: ${currentUserId?.toString() ?: ""}",
                    style = Typography.BodySmall,
                    color = Theme.colors.textSecondary,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // QR Code placeholder
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Theme.colors.background),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "QR Code",
                        style = Typography.BodyMedium,
                        color = Theme.colors.textSecondary,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (strings.myQrCodeTitle.contains("二维码")) "扫一扫上面的二维码，加我为好友" else "Scan to add me as friend",
                    style = Typography.BodySmall,
                    color = Theme.colors.textSecondary,
                )
            }
        }
    }
}
