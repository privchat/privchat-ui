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
    // 源图像素尺寸：裁剪换算的分母（x 按宽、y 按高），确认按钮在取景区之外，所以在页面级取。
    val srcPxW = (sourceSize?.first ?: 0).toFloat()
    val srcPxH = (sourceSize?.second ?: 0).toFloat()

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

            // 图片按"刚好铺满取景框"为基准（fillMaxSize + ContentScale.Crop = cover），
            // 再叠加用户的缩放/位移。
            //
            // 🔴 不要自己算显示尺寸再套 `size()`/`requiredSize()`，也不要再乘一个 cover 系数。
            // 前者会被父约束（取景框）夹回去，后者会让预览比实际裁剪多放大一倍多。
            // 判断"有没有铺满"别只看截图里的黑边——源图本身就可能是带黑色区域的截图，
            // 我就是这么误判了一轮。要量就量图片边界，或者直接和最终产物对比。
            val srcW = srcPxW.coerceAtLeast(1f)
            val srcH = srcPxH.coerceAtLeast(1f)
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
                com.tencent.kuikly.compose.foundation.Image(
                    painter = com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter(
                        model = if (imagePath.startsWith("/")) "file://$imagePath" else imagePath,
                    ),
                    contentDescription = "",
                    modifier = Modifier
                        // fillMaxSize 而不是 size(frameDp)：父 Box 就是取景框，填满它即可，
                        // 不要再走一遍 px→dp 换算（实测同样的 frameDp 给 Image，画出来只有
                        // 框的 0.82 倍，四周留黑边）。
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY,
                        ),
                    // Crop = cover：短边铺满取景框、长边溢出被上面的 clip 裁掉，正是所见即所得。
                    contentScale = ContentScale.Crop,
                )
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
                            // Crop 之后短边正好 = frameSide，长边按原始宽高比溢出。
                            val shortEdge = minOf(srcW, srcH).coerceAtLeast(1f)
                            val maxX = (frameSide * scale * (srcW / shortEdge) - frameSide)
                                .coerceAtLeast(0f) / 2f
                            val maxY = (frameSide * scale * (srcH / shortEdge) - frameSide)
                                .coerceAtLeast(0f) / 2f
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
                    onConfirm(computeCropRect(scale, offsetX, offsetY, framePx, srcPxW, srcPxH))
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
 * 契约（AVATAR_CACHE_SPEC §8.1 / FFI `AvatarCrop`）：`x` 是**占宽度**的比例、`y` 是
 * **占高度**的比例、`size` 是占**短边**的比例。三个值的分母不一样。
 *
 * 🔴 分母搞混会让裁剪区跑到别处。最初 x/y 都按短边归一化，一张 1440x3200 的竖图不缩放
 * 直接确认，裁出来的是**图片顶部**那块正方形，而取景框里明明是正中间——预览和结果对不上，
 * 而且只有长宽比越悬殊才越明显，方图完全看不出来。
 *
 * 几何：图片以"短边铺满取景框"为基准显示（cover），再整体放大 [scale] 倍、位移
 * ([offsetX], [offsetY]) 像素。于是取景框对应的图内正方形边长 = 短边 / scale，
 * 屏幕位移换算回图内像素要除以总缩放系数 `framePx * scale / 短边`。
 */
internal fun computeCropRect(
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    framePx: Float,
    srcWidth: Float,
    srcHeight: Float,
): AvatarCropRect {
    val shortEdge = minOf(srcWidth, srcHeight)
    if (framePx <= 0f || scale <= 0f || shortEdge <= 0f) {
        // 还没布局完 / 源图尺寸未知就点确认：退回"整图居中正方形"，与不传裁剪矩形等价。
        return centeredSquare(srcWidth, srcHeight)
    }
    val sidePx = shortEdge / scale
    // 屏幕像素 → 图内像素的换算系数。
    val k = framePx * scale / shortEdge
    // offset 为正 = 图片右移 = 取景框相对图片左移，所以取负。
    val leftPx = (srcWidth - sidePx) / 2f - offsetX / k
    val topPx = (srcHeight - sidePx) / 2f - offsetY / k
    return AvatarCropRect(
        x = (leftPx.coerceIn(0f, (srcWidth - sidePx).coerceAtLeast(0f))) / srcWidth,
        y = (topPx.coerceIn(0f, (srcHeight - sidePx).coerceAtLeast(0f))) / srcHeight,
        size = (sidePx / shortEdge).coerceIn(0f, 1f),
    )
}

/** 整图居中的正方形（等价于不裁剪）。 */
private fun centeredSquare(srcWidth: Float, srcHeight: Float): AvatarCropRect {
    if (srcWidth <= 0f || srcHeight <= 0f) return AvatarCropRect(0f, 0f, 1f)
    val shortEdge = minOf(srcWidth, srcHeight)
    return AvatarCropRect(
        x = (srcWidth - shortEdge) / 2f / srcWidth,
        y = (srcHeight - shortEdge) / 2f / srcHeight,
        size = 1f,
    )
}
