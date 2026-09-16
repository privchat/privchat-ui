package com.netonstream.privchat.ui.media

import androidx.compose.runtime.*
import com.gearui.components.navbar.NavBar
import com.gearui.foundation.primitives.Text
import com.gearui.runtime.LocalRuntimeEnvironment
import com.gearui.theme.Theme
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.gestures.detectTransformGestures
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.graphicsLayer
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.layout.onGloballyPositioned
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 用户在裁剪界面框定的区域，归一化到 0..1，相对 **方向校正之后**的源图。
 *
 * 与 `AVATAR_CACHE_SPEC §8.1` 的 FFI 契约一一对应。用比例而不是像素，是为了让这个
 * 组件不必知道源图的原始尺寸——那又会逼 UI 自己读 EXIF 判断宽高是否交换，而方向
 * 这件事应该只在 SDK 里处理一次。
 */
data class AvatarCropRect(
    val x: Float,
    val y: Float,
    val size: Float,
)

/**
 * 头像裁剪页（AVATAR_CACHE_SPEC §8.2）。
 *
 * 固定居中的正方形取景框，图片可缩放 + 拖动；确认时把取景框对应的源图比例交给
 * `prepareAvatarImage`。
 *
 * 取景框**不动**、图片动——这是相册类裁剪的通行做法，用户对"框住的就是结果"有直觉；
 * 反过来让框在图上游走，缩放时框和图的相对关系就要靠想象。
 */
@Composable
fun AvatarCropPage(
    imagePath: String,
    onCancel: () -> Unit,
    onConfirm: (AvatarCropRect) -> Unit,
    uploading: Boolean = false,
) {
    val strings = PrivChatI18n.strings
    val safeArea = LocalRuntimeEnvironment.current.safeArea

    // 手势状态：scale 是相对"图片刚好铺满取景框"那一档的倍数。
    var scale by remember(imagePath) { mutableStateOf(1f) }
    var offsetX by remember(imagePath) { mutableStateOf(0f) }
    var offsetY by remember(imagePath) { mutableStateOf(0f) }
    // 取景框边长（px），布局后才知道。换算裁剪比例要用它。
    var viewportPx by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black),
    ) {
        Spacer(modifier = Modifier.height(safeArea.top))
        NavBar(
            useDefaultBack = true,
            onBackClick = onCancel,
            title = strings.avatarCropTitle,
            backgroundColor = Color.Black,
            titleColor = Color.White,
            showBottomDivider = false,
        )

        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            // 取景框：正方形，边长取宽度。图片在它下面被变换。
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .onGloballyPositioned { viewportPx = it.size.width.toFloat() }
                    .pointerInput(imagePath) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            // 下限 1f：缩到比取景框还小会露出黑边，那时裁出来的头像
                            // 会带一圈背景色，不是用户框的内容。
                            scale = (scale * zoom).coerceIn(1f, 6f)
                            offsetX += pan.x
                            offsetY += pan.y
                            // 位移钳在"图片边缘不进框内"的范围里，同样是为了不露黑边。
                            val maxOffset = viewportPx * (scale - 1f) / 2f
                            offsetX = offsetX.coerceIn(-maxOffset, maxOffset)
                            offsetY = offsetY.coerceIn(-maxOffset, maxOffset)
                        }
                    },
            ) {
                com.tencent.kuikly.compose.foundation.Image(
                    painter = com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter(
                        model = if (imagePath.startsWith("/")) "file://$imagePath" else imagePath,
                    ),
                    contentDescription = "",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY,
                        ),
                    // Crop 而不是 Fit：取景框是正方形，Fit 会在框内留白，而留白区域
                    // 会被当成头像的一部分裁进去。
                    contentScale = ContentScale.Crop,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = strings.cancel,
                color = Color.White,
                style = Theme.typography.bodyMedium,
                modifier = Modifier.clickable(enabled = !uploading) { onCancel() },
            )
            Text(
                text = if (uploading) strings.avatarCropUploading else strings.avatarCropConfirm,
                color = if (uploading) Color.Gray else Theme.colors.primary,
                style = Theme.typography.bodyMedium,
                modifier = Modifier.clickable(enabled = !uploading) {
                    onConfirm(computeCropRect(scale, offsetX, offsetY, viewportPx))
                },
            )
        }
        Spacer(modifier = Modifier.height(safeArea.bottom))
    }
}

/**
 * 把手势状态换算成归一化裁剪矩形。
 *
 * 图片以 `ContentScale.Crop` 铺满正方形取景框，再整体放大 [scale] 倍、位移
 * ([offsetX], [offsetY]) 像素。所以取景框在"图片自身坐标系"里占的比例就是 `1/scale`，
 * 而位移换算成比例要除以「放大后的图片边长」= `viewportPx * scale`。
 *
 * 结果相对的是**铺满取景框后的那张图**，也就是源图的短边——与 FFI 契约里
 * 「size 相对短边」一致。
 */
internal fun computeCropRect(
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    viewportPx: Float,
): AvatarCropRect {
    if (viewportPx <= 0f || scale <= 0f) {
        // 还没布局完就确认：退回整图中心（等价于不传裁剪矩形）。
        return AvatarCropRect(x = 0f, y = 0f, size = 1f)
    }
    val size = (1f / scale).coerceIn(0f, 1f)
    val scaledEdge = viewportPx * scale
    // offset 为正 = 图片右移 = 取景框相对图片左移，所以取负。
    val left = (1f - size) / 2f - offsetX / scaledEdge
    val top = (1f - size) / 2f - offsetY / scaledEdge
    return AvatarCropRect(
        x = left.coerceIn(0f, 1f - size),
        y = top.coerceIn(0f, 1f - size),
        size = size,
    )
}
