package com.netonstream.privchat.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gearui.Spacing
import com.gearui.foundation.primitives.Icon
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.typography.Typography
import com.gearui.overlay.GearOverlayOptions
import com.gearui.overlay.GearOverlayPlacement
import com.gearui.overlay.OverlayDismissPolicy
import com.gearui.overlay.rememberGearOverlay
import com.gearui.runtime.LocalGearRuntimeEnvironment
import com.gearui.theme.Theme
import com.tencent.kuikly.compose.animation.core.animateFloatAsState
import com.tencent.kuikly.compose.animation.core.animateIntAsState
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.gestures.detectTapGestures
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.widthIn
import com.tencent.kuikly.compose.foundation.layout.wrapContentWidth
import com.tencent.kuikly.compose.foundation.shape.CircleShape
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.alpha
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.draw.shadow
import com.tencent.kuikly.compose.ui.geometry.Rect
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.layout.boundsInRoot
import com.tencent.kuikly.compose.ui.layout.onGloballyPositioned
import com.tencent.kuikly.compose.ui.layout.onSizeChanged
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** 默认快捷 reactions（调用方可自定义） */
val DefaultMessageReactions: List<String> = listOf("👍", "❤️", "😂", "🎉", "🔥", "👀")

/**
 * 长按消息时弹出的 action list 单项。
 *
 * @param label 文本
 * @param icon 左侧图标名（使用 [com.gearui.components.icon.Icons] 常量）
 * @param disabled 是否禁用
 * @param danger 是否以语义 danger 色着色
 * @param onClick 点击回调
 */
data class MessageAction(
    val label: String,
    val icon: String,
    val disabled: Boolean = false,
    val danger: Boolean = false,
    val onClick: () -> Unit,
)

private const val BubbleEnterDelayMs: Long = 0L
private const val MaskFadeMs: Int = 30
private const val MenuFadeMs: Int = 30
private const val SlideMs: Int = 40

/**
 * 消息长按动作菜单（Telegram 风格）
 *
 * 交互分阶段：
 * 1. 长按 [bubble]，overlay 打开，**先**在原位置渲染气泡副本（尺寸被钉死为原气泡）。
 * 2. 遮罩淡入。
 * 3. Reaction bar / action list 淡入（此时仍位于原气泡上下方）。
 * 4. 气泡副本连同 reaction bar / action list 一起滑动到屏幕内的合适位置
 *    （上方 reaction bar 不够 / 下方 action list 不够时整体上下平移）。
 *
 * 业务规则由调用方动态拼装 [actions]（撤回时间窗、是否显示复制、Pin 等均在外层决定）。
 *
 * @param actions 下方功能菜单项
 * @param modifier 外层容器 Modifier（控制气泡的最大宽度等外观约束）
 * @param reactions 上方 reaction 列表，默认 [DefaultMessageReactions]；空列表且 [onMoreReactions] 为 null 时不渲染 reaction bar
 * @param onReaction 点击 reaction 的回调
 * @param onMoreReactions 非空时在 reaction bar 末尾追加 "+"
 * @param isSelf 自己发的消息。用于决定 reaction bar / action list 水平对齐（右对齐 vs 左对齐）
 * @param bubble 消息气泡内容；同一块 composable 会被原地渲染一次，并在长按弹出时在 overlay 中再次渲染一份副本
 */
@Composable
fun MessageActionsMenu(
    actions: List<MessageAction>,
    modifier: Modifier = Modifier,
    reactions: List<String> = DefaultMessageReactions,
    onReaction: ((String) -> Unit)? = null,
    onMoreReactions: (() -> Unit)? = null,
    isSelf: Boolean = false,
    bubble: @Composable () -> Unit,
) {
    val overlay = rememberGearOverlay()

    var visible by remember { mutableStateOf(false) }
    var anchorBounds by remember { mutableStateOf<Rect?>(null) }
    var pressedActionIndex by remember { mutableStateOf<Int?>(null) }

    val bounds = anchorBounds
    val showReactionBar = reactions.isNotEmpty() || onMoreReactions != null

    if (visible && bounds != null) {
        DisposableEffect(bounds) {
            val id = overlay.show(
                anchorBounds = null, // Fullscreen 不依赖 anchor
                options = GearOverlayOptions(
                    placement = GearOverlayPlacement.Fullscreen,
                    modal = true,
                    // 自绘遮罩，用于做淡入；这里把 overlay 自带遮罩关掉。
                    maskColor = Color.Transparent,
                    dismissPolicy = OverlayDismissPolicy(
                        outsideClick = true,
                        backPress = true,
                    ),
                    // safeArea=false：让 Fullscreen 内容盒子与 boundsInRoot() 坐标系对齐
                    // （content 盒子 = overlay host 根 = compose 根），
                    // 自绘遮罩 fillMaxSize 即可覆盖状态栏与底部导航栏；
                    // 气泡/菜单的安全区规避由本组件在 clamp 中处理。
                    safeAreaTop = false,
                    safeAreaBottom = false,
                ),
                onDismiss = { visible = false },
            ) {
                MessageActionsOverlayContent(
                    anchor = bounds,
                    isSelf = isSelf,
                    showReactionBar = showReactionBar,
                    reactions = reactions,
                    onReaction = { emoji ->
                        onReaction?.invoke(emoji)
                        visible = false
                    },
                    onMoreReactions = onMoreReactions?.let {
                        {
                            it()
                            visible = false
                        }
                    },
                    actions = actions,
                    pressedActionIndex = pressedActionIndex,
                    onPressChange = { pressedActionIndex = it },
                    onActionClick = { item ->
                        item.onClick()
                        visible = false
                    },
                    bubble = bubble,
                )
            }
            onDispose {
                pressedActionIndex = null
                overlay.dismiss(id)
            }
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                anchorBounds = coordinates.boundsInRoot()
            }
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { if (!visible) visible = true })
            },
    ) {
        bubble()
    }
}

@Composable
private fun MessageActionsOverlayContent(
    anchor: Rect,
    isSelf: Boolean,
    showReactionBar: Boolean,
    reactions: List<String>,
    onReaction: (String) -> Unit,
    onMoreReactions: (() -> Unit)?,
    actions: List<MessageAction>,
    pressedActionIndex: Int?,
    onPressChange: (Int?) -> Unit,
    onActionClick: (MessageAction) -> Unit,
    bubble: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val gapPx = with(density) { Spacing.spacer8.dp.roundToPx() }
    val edgePaddingPx = with(density) { 16.dp.roundToPx() }

    // overlay 的 Fullscreen 内容盒子被 safeArea 内缩过，遮罩需要向外反扩回去。
    val safeArea = LocalGearRuntimeEnvironment.current.safeArea
    val safeTopPx = with(density) { safeArea.top.roundToPx() }
    val safeBottomPx = with(density) { safeArea.bottom.roundToPx() }
    val safeLeftPx = with(density) { safeArea.left.roundToPx() }
    val safeRightPx = with(density) { safeArea.right.roundToPx() }

    // 气泡副本锁定在原气泡的像素尺寸，避免在 overlay 下重新布局时宽高变化。
    val bubbleWidthPx = anchor.width.toInt().coerceAtLeast(0)
    val bubbleHeightPx = anchor.height.toInt().coerceAtLeast(0)
    val bubbleWidthDp = with(density) { bubbleWidthPx.toDp() }
    val bubbleHeightDp = with(density) { bubbleHeightPx.toDp() }

    var overlaySize by remember { mutableStateOf(IntSize.Zero) }
    var reactionSize by remember { mutableStateOf(IntSize.Zero) }
    var actionSize by remember { mutableStateOf(IntSize.Zero) }

    val needsReaction = showReactionBar
    val ready = overlaySize != IntSize.Zero &&
        (!needsReaction || reactionSize != IntSize.Zero) &&
        actionSize != IntSize.Zero

    val desiredY = anchor.top.toInt()

    // 最终 Y 位置：默认与原锚点同位，若上下空间不够则钳制到可见范围内。
    // overlay 内容盒已与 compose 根坐标一致，clamp 要自行避开状态栏 / 底部导航栏。
    val finalBubbleY: Int = run {
        if (!ready) return@run desiredY
        val reactionH = if (needsReaction) reactionSize.height else 0
        val reactionGap = if (needsReaction) gapPx else 0
        val minY = safeTopPx + edgePaddingPx + reactionH + reactionGap
        val maxY = overlaySize.height - safeBottomPx - edgePaddingPx - actionSize.height - gapPx - bubbleHeightPx
        if (minY > maxY) minY else desiredY.coerceIn(minY, maxY)
    }

    // 仅在需要平移时才启动 slide；菜单能完整显示时气泡原地不动。
    val needsShift = ready && finalBubbleY != desiredY

    // 分阶段弹出：bubble → mask → menu → slide
    var maskShown by remember { mutableStateOf(false) }
    var menuShown by remember { mutableStateOf(false) }
    var slideStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(BubbleEnterDelayMs)
        maskShown = true
        delay(MaskFadeMs.toLong())
        menuShown = true
        delay(MenuFadeMs.toLong())
        slideStarted = true
    }

    val maskAlpha by animateFloatAsState(
        targetValue = if (maskShown) 0.72f else 0f,
        animationSpec = tween(MaskFadeMs),
    )
    val menuAlpha by animateFloatAsState(
        targetValue = if (menuShown) 1f else 0f,
        animationSpec = tween(MenuFadeMs),
    )
    val animatedBubbleY by animateIntAsState(
        targetValue = if (needsShift && slideStarted) finalBubbleY else desiredY,
        animationSpec = tween(SlideMs),
    )

    val bubbleLeftX: Int = anchor.left.toInt()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { overlaySize = it },
    ) {
        // 自绘遮罩（受 maskAlpha 控制，单独淡入）。
        // overlay 内容盒已是全屏，fillMaxSize 自然覆盖状态栏 / 底部导航栏。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = maskAlpha)),
        )

        if (needsReaction) {
            val reactionX = horizontalAlignX(
                anchor = anchor,
                contentWidth = reactionSize.width,
                overlayWidth = overlaySize.width,
                leftInset = safeLeftPx + edgePaddingPx,
                rightInset = safeRightPx + edgePaddingPx,
                isSelf = isSelf,
            )
            val reactionY = (animatedBubbleY - gapPx - reactionSize.height).coerceAtLeast(safeTopPx + edgePaddingPx)
            Box(
                modifier = Modifier
                    .offset { IntOffset(reactionX, reactionY) }
                    .onSizeChanged { reactionSize = it }
                    .alpha(menuAlpha),
            ) {
                ReactionBar(
                    reactions = reactions,
                    onReaction = onReaction,
                    onMoreReactions = onMoreReactions,
                )
            }
        }

        // 气泡副本：尺寸被锁定为原气泡像素尺寸，始终可见（不参与 menuAlpha 淡入）
        Box(
            modifier = Modifier
                .offset { IntOffset(bubbleLeftX, animatedBubbleY) }
                .size(bubbleWidthDp, bubbleHeightDp),
        ) {
            bubble()
        }

        run {
            val actionX = horizontalAlignX(
                anchor = anchor,
                contentWidth = actionSize.width,
                overlayWidth = overlaySize.width,
                leftInset = safeLeftPx + edgePaddingPx,
                rightInset = safeRightPx + edgePaddingPx,
                isSelf = isSelf,
            )
            val actionY = animatedBubbleY + bubbleHeightPx + gapPx
            Box(
                modifier = Modifier
                    .offset { IntOffset(actionX, actionY) }
                    .onSizeChanged { actionSize = it }
                    .alpha(menuAlpha),
            ) {
                ActionList(
                    actions = actions,
                    pressedIndex = pressedActionIndex,
                    onPressChange = onPressChange,
                    onActionClick = onActionClick,
                )
            }
        }
    }
}

/** 水平对齐：自己发的消息右对齐到气泡右边；对方消息左对齐到气泡左边。contentWidth=0（首帧未测量）时临时左对齐。 */
private fun horizontalAlignX(
    anchor: Rect,
    contentWidth: Int,
    overlayWidth: Int,
    leftInset: Int,
    rightInset: Int,
    isSelf: Boolean,
): Int {
    if (contentWidth == 0) return anchor.left.toInt()
    val raw = if (isSelf) {
        (anchor.right.toInt() - contentWidth)
    } else {
        anchor.left.toInt()
    }
    val maxX = (overlayWidth - rightInset - contentWidth).coerceAtLeast(leftInset)
    return raw.coerceIn(leftInset, maxX)
}

@Composable
private fun ReactionBar(
    reactions: List<String>,
    onReaction: (String) -> Unit,
    onMoreReactions: (() -> Unit)?,
) {
    val colors = Theme.colors
    // 使用胶囊形状（与圆形 emoji 视觉统一）
    val pillShape = CircleShape
    Row(
        modifier = Modifier
            .shadow(Spacing.spacer4.dp, pillShape)
            .clip(pillShape)
            .background(colors.surface)
            .padding(horizontal = Spacing.spacer8.dp, vertical = Spacing.spacer4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.spacer4.dp),
    ) {
        reactions.forEach { emoji ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onReaction(emoji) },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, style = Typography.TitleExtraLarge)
            }
        }
        if (onMoreReactions != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceVariant)
                    .clickable { onMoreReactions() },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "+", style = Typography.TitleExtraLarge, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun ActionList(
    actions: List<MessageAction>,
    pressedIndex: Int?,
    onPressChange: (Int?) -> Unit,
    onActionClick: (MessageAction) -> Unit,
) {
    val colors = Theme.colors
    val shapes = Theme.shapes
    Column(
        modifier = Modifier
            .widthIn(min = 140.dp, max = 200.dp)
            .wrapContentWidth()
            .shadow(Spacing.spacer4.dp, shapes.default)
            .clip(shapes.default)
            .background(colors.surface)
            .border(1.dp, colors.border, shapes.default)
            .padding(Spacing.spacer4.dp),
    ) {
        actions.forEachIndexed { index, item ->
            val tint = when {
                item.disabled -> colors.textDisabled
                item.danger -> colors.danger
                else -> colors.textPrimary
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.small)
                    .background(
                        if (pressedIndex == index) colors.surfaceVariant else colors.surface,
                    )
                    .clickable(enabled = !item.disabled) {
                        onPressChange(index)
                        onActionClick(item)
                        onPressChange(null)
                    }
                    .padding(horizontal = Spacing.spacer8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.spacer8.dp),
            ) {
                Icon(name = item.icon, size = 18.dp, tint = tint)
                Text(
                    text = item.label,
                    style = Typography.BodySmall,
                    color = tint,
                )
            }
        }
    }
}
