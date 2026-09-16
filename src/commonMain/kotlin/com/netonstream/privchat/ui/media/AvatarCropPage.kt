package com.netonstream.privchat.ui.media

import androidx.compose.runtime.*
import com.gearui.components.navbar.NavBar
import com.gearui.foundation.primitives.Text
import com.gearui.runtime.LocalRuntimeEnvironment
import com.gearui.theme.Theme
import com.netonstream.privchat.ui.components.rememberPendingImageSize
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.gestures.detectTransformGestures
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.graphicsLayer
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.platform.LocalDensity
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
 * 整张图都看得见，中间一个正方形取景框，框外压暗；框住的那块就是头像。
 *
 * 🔴 **框外必须看得见**。第一版把图片按 Crop 塞进正方形框里，框外的部分根本不渲染——
 * 一张横构图的照片进去，左右两边被直接切掉，而用户完全不知道自己裁掉了什么，也没法
 * 判断要往哪边拖。裁剪界面的价值就在于"让人看见取舍"，看不见就只是个自动裁剪。
 *
 * 取景框**不动**、图片动——相册类裁剪的通行做法，用户对"框住的就是结果"有直觉；
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
    val density = LocalDensity.current

    // 源图的展示方向尺寸（已 applyExif）。用来算"图片铺满取景框"那一档的基准缩放。
    // 不走 painter.intrinsicSize：Kuikly 下那是 native 调用，图未加载会抛。
    val sourceSize = rememberPendingImageSize(imagePath)

    var scale by remember(imagePath) { mutableStateOf(1f) }
    var offsetX by remember(imagePath) { mutableStateOf(0f) }
    var offsetY by remember(imagePath) { mutableStateOf(0f) }
    // 取景框边长（px），布局后才知道；换算裁剪比例要用它。
    var framePx by remember { mutableStateOf(0f) }

    // 外层单独一个铺满的黑底盒子：内容列自己的背景在 Kuikly 下未必覆盖到每一个像素，
    // 而这个页面盖在别的 overlay 上，漏一条缝就会把下面那层的旧画面透上来。
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
    Column(
        modifier = Modifier.fillMaxSize(),
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

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            val areaW = with(density) { maxWidth.toPx() }
            val areaH = with(density) { maxHeight.toPx() }
            // 取景框：正方形，边长取可用区短边再留一点边距，好让框外的图露出来。
            val frameSide = minOf(areaW, areaH) * 0.82f
            val frameDp = with(density) { frameSide.toDp() }
            LaunchedEffect(frameSide) { framePx = frameSide }

            // 图片按"刚好铺满取景框"为基准（= Crop 到框），再叠加用户的缩放。
            // 铺满框需要的显示尺寸：短边对齐框边长。
            val srcW = (sourceSize?.first ?: 1).toFloat()
            val srcH = (sourceSize?.second ?: 1).toFloat()
            val coverScale = frameSide / minOf(srcW, srcH)
            val shownW = with(density) { (srcW * coverScale).toDp() }
            val shownH = with(density) { (srcH * coverScale).toDp() }

            // 取景框：**裁剪**到方框，图片只在框内绘制。
            //
            // 🔴 必须 clip。图片经 graphicsLayer 放大后会画到布局边界之外——溢出到上面的
            // NavBar 和下面的按钮行，实测状态栏后面透出图像、"取消/使用"被盖住。
            // 之前试过用四块黑矩形遮住框外，但那些矩形只在中间内容区里，够不到 NavBar
            // 和按钮行那两块，治标不治本。裁掉就没有溢出可言了。
            Box(
                modifier = Modifier
                    .size(frameDp)
                    .clip(com.tencent.kuikly.compose.ui.graphics.RectangleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (sourceSize != null) {
                    com.tencent.kuikly.compose.foundation.Image(
                        painter = com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter(
                            model = if (imagePath.startsWith("/")) "file://$imagePath" else imagePath,
                        ),
                        contentDescription = "",
                        modifier = Modifier
                            .size(shownW, shownH)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY,
                            ),
                        // 尺寸已按真实宽高比给定，Fit 即原样显示、不再二次裁切。
                        contentScale = ContentScale.Fit,
                    )
                }
            }

            // 边框四线：画在 clip 之外，否则会被一起裁掉。
            //
            // 不用 `border()`：即使显式传 RectangleShape，Kuikly 画出来仍是圆角
            // （实测边线两端各短约 26px，正是圆角切掉的部分）。
            val edge = Color.White.copy(alpha = 0.9f)
            Box(Modifier.size(frameDp)) {
                Box(Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopStart).background(edge))
                Box(Modifier.fillMaxWidth().height(1.dp).align(Alignment.BottomStart).background(edge))
                Box(Modifier.width(1.dp).fillMaxHeight().align(Alignment.TopStart).background(edge))
                Box(Modifier.width(1.dp).fillMaxHeight().align(Alignment.TopEnd).background(edge))
            }

            // 手势层盖在最上面且铺满：手指落在框外也能拖，不必非得压在图上。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(imagePath) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            // 下限 1f：缩到比取景框还小会露出黑边，那时裁出来的头像
                            // 会带一圈背景，不是用户框的内容。
                            scale = (scale * zoom).coerceIn(1f, 6f)
                            offsetX += pan.x
                            offsetY += pan.y
                            // 位移钳在"取景框不越出图片"的范围里，同样是为了不露黑边。
                            val maxX = (srcW * coverScale * scale - frameSide).coerceAtLeast(0f) / 2f
                            val maxY = (srcH * coverScale * scale - frameSide).coerceAtLeast(0f) / 2f
                            offsetX = offsetX.coerceIn(-maxX, maxX)
                            offsetY = offsetY.coerceIn(-maxY, maxY)
                        }
                    },
            )
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
                    onConfirm(computeCropRect(scale, offsetX, offsetY, framePx))
                },
            )
        }
        Spacer(modifier = Modifier.height(safeArea.bottom))
    }
    }
}

/**
 * 把手势状态换算成归一化裁剪矩形。
 *
 * 图片以"短边铺满取景框"为基准显示，再整体放大 [scale] 倍、位移
 * ([offsetX], [offsetY]) 像素。所以取景框在图片短边上占的比例就是 `1/scale`，
 * 而位移换算成比例要除以「放大后的短边长度」= `framePx * scale`。
 *
 * 结果相对的是源图短边——与 FFI 契约里「size 相对短边」一致。
 */
internal fun computeCropRect(
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    framePx: Float,
): AvatarCropRect {
    if (framePx <= 0f || scale <= 0f) {
        // 还没布局完就确认：退回整图中心（等价于不传裁剪矩形）。
        return AvatarCropRect(x = 0f, y = 0f, size = 1f)
    }
    val size = (1f / scale).coerceIn(0f, 1f)
    val scaledShortEdge = framePx * scale
    // offset 为正 = 图片右移 = 取景框相对图片左移，所以取负。
    val left = (1f - size) / 2f - offsetX / scaledShortEdge
    val top = (1f - size) / 2f - offsetY / scaledShortEdge
    return AvatarCropRect(
        x = left.coerceIn(0f, 1f - size),
        y = top.coerceIn(0f, 1f - size),
        size = size,
    )
}
