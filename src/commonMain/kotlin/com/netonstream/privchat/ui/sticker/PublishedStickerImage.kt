package com.netonstream.privchat.ui.sticker

import androidx.compose.runtime.*
import com.gearui.foundation.primitives.Text
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.tencent.kuikly.compose.coil3.AsyncImagePainter
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.layout.ContentScale

@Composable
internal fun PublishedStickerImage(url: String, label: String, modifier: Modifier, onRetry: () -> Unit = {}) {
    val strings = PrivChatI18n.strings
    val painter = rememberAsyncImagePainter(url)
    val state by painter.state.collectAsState()
    Box(modifier, contentAlignment = Alignment.Center) {
        Image(painter, contentDescription = label, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        when (state) {
            is AsyncImagePainter.State.Error -> Text(strings.retry, modifier = Modifier.clickable {
                onRetry()
                painter.restart()
            })
            is AsyncImagePainter.State.Empty, is AsyncImagePainter.State.Loading -> Text(strings.loading)
            else -> Unit
        }
    }
}
