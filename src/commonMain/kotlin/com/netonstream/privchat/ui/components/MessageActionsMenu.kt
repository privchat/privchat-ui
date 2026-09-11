package com.netonstream.privchat.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.netonstream.privchat.ui.common.base.currentTimeMillis
import com.gearui.foundation.layout.Spacing
import com.gearui.foundation.primitives.Icon
import com.gearui.foundation.primitives.Text
import com.gearui.foundation.typography.IconSizes
import com.gearui.overlay.OverlayOptions
import com.gearui.overlay.OverlayPlacement
import com.gearui.overlay.OverlayDismissPolicy
import com.gearui.overlay.rememberOverlay
import com.gearui.runtime.LocalRuntimeEnvironment
import com.gearui.theme.Theme
import com.tencent.kuikly.compose.animation.core.animateFloatAsState
import com.tencent.kuikly.compose.animation.core.animateIntAsState
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.gestures.awaitEachGesture
import com.tencent.kuikly.compose.foundation.gestures.awaitFirstDown
import com.tencent.kuikly.compose.foundation.gestures.detectDragGestures
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
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.geometry.Rect
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.layout.boundsInRoot
import com.tencent.kuikly.compose.ui.layout.onGloballyPositioned
import com.tencent.kuikly.compose.ui.layout.onSizeChanged
import com.tencent.kuikly.compose.ui.platform.LocalFocusManager
import com.tencent.kuikly.compose.ui.platform.LocalSoftwareKeyboardController
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.compose.ui.unit.dp
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 当前打开着菜单的消息（[MessageActionsMenu] 的 `pointerInputKey`）。
 *
 * 放在组件外面，是因为消息行会被懒加载列表回收——见 [MessageActionsMenu] 里的说明。
 * 同一时刻只可能开一个菜单，所以这里只需要一个 key。
 */
private object OpenMessageMenu {
    var key by mutableStateOf<Any?>(null)

    /**
     * 这一行被拆掉的时刻。
     *
     * 拆掉有两种：列表回收（下一帧就会有新实例接手，菜单要继续）和真的离开（退出会话页、
     * 消息被删）。这里用时间区分——回收是同一帧的事，隔了一会儿还没人接手，就说明是后者，
     * key 已经过期，不能让下次进来时菜单自己弹出来。
     */
    var detachedAt: Long = 0L

    fun isStale(now: Long): Boolean = detachedAt != 0L && now - detachedAt > STALE_AFTER_MS
}

/** 超过这个时间没有新实例接手，就当作"这一行真的没了"。 */
private const val STALE_AFTER_MS = 1500L

/** 默认快捷 reactions（调用方可自定义） */
val DefaultMessageReactions: List<String> = listOf("👍", "❤️", "😂", "🎉", "🔥", "👀")

/**
 * 由 [MessageActionsMenu] 向 bubble 内部下发的「打开长按菜单」回调。
 *
 * 普通气泡（文本/语音等）直接靠 MessageActionsMenu 外层 `detectTapGestures(onLongPress)` 触发。
 * 但图片 / 视频气泡内部自带 `onTap` 用于大图预览，与外层长按在 Kuikly 的手势分发下会互相冲突
 * （`Modifier.clickable` 在 Main pass 消费 down，外层永远收不到 long-press）。
 *
 * 因此给 bubble 内容透出一个手动触发器：图片/视频内容使用 `detectTapGestures(onTap, onLongPress)`
 * 自行处理两种手势，长按时调用本 local 打开菜单。
 */
val LocalMessageMenuTrigger = staticCompositionLocalOf<(() -> Unit)?> { null }

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

/** 长按判定时长，与平台一致。 */
private const val LONG_PRESS_TIMEOUT_MS: Long = 500L

/** 手指真实滑动多远算"在滚列表"。比系统 touch slop 宽一点：长按时手抖很正常。 */
private val LONG_PRESS_SLOP = 16.dp

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
    /**
     * `pointerInput` 的 key。LazyColumn 回收 item 时默认 `Unit` 会让同一手势块跨消息复用，
     * 滚动后再长按可能拿不到 down 事件。调用方应传入稳定的 message 标识（如 message.id）。
     */
    pointerInputKey: Any = Unit,
    /**
     * 菜单打开时回调一次。
     *
     * 有些菜单项的内容只有打开时才值得去查（例如群消息的已读人数要发一次 RPC）。
     * 在气泡组合时查会让一屏消息各发一个请求，而用户可能一个菜单都不会打开。
     */
    onMenuOpen: (() -> Unit)? = null,
    bubble: @Composable () -> Unit,
) {
    val overlay = rememberOverlay()

    // 🔴 "哪条消息的菜单开着"不能存在这一行里。
    //
    // 按住一条消息会让输入框失焦、键盘收起，列表随之重排——而重排会把这一行从
    // composition 里换掉（懒加载列表回收）。行里 remember 的状态、正在跑的协程一起没了，
    // 于是菜单刚打开就被连根拔掉，屏幕上一次都不出现。真机日志里就是：openMenu 打印了
    // visible=true，紧接着同一个 effect 以 visible=false 重新跑了一遍——那已经是新实例。
    //
    // 存到组件外面：行被换掉，新实例读到的仍然是"我这条开着"，于是原地把菜单接着显示出来。
    // 同一时刻只可能有一个菜单，所以一个模块级的 key 就够，不必为此加一层 CompositionLocal。
    var visible by remember(pointerInputKey) {
        object : MutableState<Boolean> {
            override var value: Boolean
                get() = OpenMessageMenu.key == pointerInputKey
                set(v) {
                    if (v) {
                        OpenMessageMenu.key = pointerInputKey
                    } else if (OpenMessageMenu.key == pointerInputKey) {
                        OpenMessageMenu.key = null
                    }
                }

            override fun component1(): Boolean = value
            override fun component2(): (Boolean) -> Unit = { value = it }
        }
    }
    var anchorBounds by remember { mutableStateOf<Rect?>(null) }
    var pressedActionIndex by remember { mutableStateOf<Int?>(null) }

    val bounds = anchorBounds
    val showReactionBar = reactions.isNotEmpty() || onMoreReactions != null

    // 🔴 菜单项必须能在**菜单已经打开之后**变化。
    //
    // 下面的 DisposableEffect 只以 bounds 为 key，它捕获的是那一刻的 actions 列表；
    // 之后父级再怎么重组，overlay 里都还是旧的那份。于是「N 人已读」这种打开菜单
    // 才去查、查回来才有的条目，第一次长按永远不显示，要长按第二次（那时上一次的
    // 结果已经缓存）才看得到——真机上就是这个症状。
    // rememberUpdatedState 给的是稳定的 State 对象：overlay 的 content 在 host 的
    // composition 里读它，值一变就重组 overlay 自己，不必重新 show。
    val currentActions by rememberUpdatedState(actions)

    // 锚点：**先等它稳定，定下来之后就冻结**。
    //
    // 打开菜单会先收起系统键盘（gearui OverlayHost 的 dismissKeyboardOnShow），列表
    // 随之重排一次，气泡 bounds 立刻变化——所以不能一拿到 bounds 就定位。
    //
    // 但也不能"一直跟着 bounds 走"：那是这一版之前的做法（DisposableEffect(bounds)
    // 每次变化 dispose 再 show），滚动时每帧都触发一次，真机 logcat 上是 60ms 内
    // show/dismiss 五轮，菜单和气泡副本各自贴到屏幕上某个位置，与背景完全对不上，
    // 看起来就是「界面乱了」。
    //
    // 冻结之后锚点再动，只可能是列表滚了 → 关闭菜单。
    // 🔴 不能指望 OverlayDismissPolicy.scroll：它由 GearLazyColumn 的 Compose 手势
    // 回调触发，而菜单是模态 overlay，手指落在遮罩上，列表那层根本收不到指针事件；
    // Kuikly 的列表滚动又是原生滚动视图在做，绕过了 Compose 的手势仲裁。真机实测
    // notifyScroll 一次都没发出来。能观察到滚动的是布局本身——也就是这里的 bounds。
    // DisposableEffect 的拆除与"用户关掉菜单"都会走 overlay 的 onDismiss，用它区分。
    var tearingDown by remember(pointerInputKey) { mutableStateOf(false) }

    // 过期的 key（上一次是真的离开了，不是回收）在这里清掉，否则重新进会话时
    // 菜单会自己弹出来。
    LaunchedEffect(pointerInputKey) {
        if (OpenMessageMenu.key == pointerInputKey && OpenMessageMenu.isStale(currentTimeMillis())) {
            OpenMessageMenu.key = null
        }
        OpenMessageMenu.detachedAt = 0L
    }

    var anchoredBounds by remember { mutableStateOf<Rect?>(null) }
    LaunchedEffect(visible) {
        if (!visible) {
            anchoredBounds = null
            return@LaunchedEffect
        }
        // 连续三帧一致才算稳：收键盘是一段动画，相邻两帧偶尔会量到同一个值，
        // 两帧一致就冻结会锁在动画中途的位置上。
        var previous: Rect? = null
        var stableCount = 0
        while (anchoredBounds == null) {
            val current = anchorBounds
            if (current != null && current == previous) {
                stableCount += 1
                if (stableCount >= 2) {
                    anchoredBounds = current
                    break
                }
            } else {
                stableCount = 0
                previous = current
            }
            delay(16)
        }
    }
    // 锚点定下来之后再动，就跟着动，不关菜单。
    //
    // 🔴 以前是"一动就关"，那是在菜单还会被自己的重定位搞乱的时候写的兜底。
    //
    // 现在 overlay 只 show 一次、锚点走 rememberUpdatedState，位置变化只是内容重组，
    // 气泡副本会跟着真实气泡走，不会再出现"副本和背景对不上"的错位。
    // 而"一动就关"的代价很大：按住消息会让输入框失焦、键盘收起，列表随之重排——于是
    // 键盘开着时长按，菜单刚弹出来就被这次重排关掉，用户看到的是"长按没反应"。
    //
    // 菜单打开期间页面本身是被冻住的（LocalInputBlockedByOverlay），列表不会被用户滚动，
    // 所以这里的位移只会来自布局重排，跟着走就是对的。
    LaunchedEffect(anchoredBounds, bounds) {
        val anchored = anchoredBounds ?: return@LaunchedEffect
        val now = bounds ?: return@LaunchedEffect
        if (abs(now.top - anchored.top) > 2f || abs(now.left - anchored.left) > 2f) {
            anchoredBounds = now
        }
    }

    // 🔴 重新锚定只能更新内容，不能重开 overlay。
    //
    // DisposableEffect 原来以 anchored 为 key：锚点一变就 dispose 再 show，而 dispose 里
    // 的 overlay.dismiss() 会回调 onDismiss → visible = false，于是"跟着布局挪一下"变成了
    // "把菜单关掉"。收键盘期间布局每帧都在动，菜单在淡入完成前就被拆了十几轮，屏幕上
    // 一次都没出现过。
    //
    // 现在只在"可见且锚点已定"这个分支存在期间 show 一次，锚点走 rememberUpdatedState
    // 让 overlay 内容自己重组——与上面 actions 的处理同理。
    val currentAnchor by rememberUpdatedState(anchoredBounds)
    val anchorAtShow = anchoredBounds
    if (visible && anchorAtShow != null) {
        DisposableEffect(Unit) {
            val id = overlay.show(
                anchorBounds = null, // Fullscreen 不依赖 anchor
                options = OverlayOptions(
                    placement = OverlayPlacement.Fullscreen,
                    modal = true,
                    // 自绘遮罩，用于做淡入；这里把 overlay 自带遮罩关掉。
                    maskColor = Color.Transparent,
                    dismissPolicy = OverlayDismissPolicy(
                        outsideClick = true,
                        backPress = true,
                        // 兜底而已，别指望它：这条由 GearLazyColumn 的 Compose 手势
                        // 回调触发，而菜单是模态 overlay，手指落在遮罩上，列表那层收不到
                        // 指针事件——真机实测 notifyScroll 一次都没发出来。真正防错位的是
                        // 上面"锚点冻结 + 一动就关"，这里留着是万一将来有非模态的用法。
                        scroll = true,
                    ),
                    // safeArea=false：让 Fullscreen 内容盒子与 boundsInRoot() 坐标系对齐
                    // （content 盒子 = overlay host 根 = compose 根），
                    // 自绘遮罩 fillMaxSize 即可覆盖状态栏与底部导航栏；
                    // 气泡/菜单的安全区规避由本组件在 clamp 中处理。
                    safeAreaTop = false,
                    safeAreaBottom = false,
                    // openMenu 已经收过键盘了。让 overlay 再收一次的问题是时机：它发生在
                    // show 之后，也就是锚点冻结之后，列表因此重排，菜单会当成"滚动了"而
                    // 自己关掉。
                    dismissKeyboardOnShow = false,
                ),
                onDismiss = { if (!tearingDown) visible = false },
            ) {
                MessageActionsOverlayContent(
                    anchor = currentAnchor ?: anchorAtShow,
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
                    actions = currentActions,
                    pressedActionIndex = pressedActionIndex,
                    onPressChange = { pressedActionIndex = it },
                    onActionClick = { item ->
                        item.onClick()
                        visible = false
                    },
                    onDismiss = { visible = false },
                    bubble = bubble,
                )
            }
            onDispose {
                // 🔴 拆除时不能把"菜单开着"这件事一起清掉。
                //
                // overlay.dismiss 会回调 onDismiss → visible = false。行被列表回收时这就等于
                // 替用户把菜单关了——而用户只是按住了一条消息。所以先立个标记，让那次回调
                // 认得出这是拆除而不是关闭。overlay 本身还是要 dismiss：它里面画的是这一个
                // 实例的气泡副本，留着会停在重排前的位置上。新实例会自己重新弹一次。
                tearingDown = true
                OpenMessageMenu.detachedAt = currentTimeMillis()
                pressedActionIndex = null
                overlay.dismiss(id)
                tearingDown = false
            }
        }
    }

    // 🔴 键盘必须在 overlay 打开**之前**收掉，不能交给 overlay 自己收。
    //
    // 锚点是"等 bounds 稳定下来就冻结、之后一动就关菜单"。而 overlay 的
    // dismissKeyboardOnShow 是在 show 之后才收键盘的：那时锚点已经冻结，键盘一收列表
    // 重排、bounds 跟着变，菜单立刻自己关掉——表现就是"刚回复完再长按，菜单不弹了"，
    // 因为回复态恰好是键盘开着的状态。
    //
    // 所以这里先收键盘、等布局稳定再冻结锚点，overlay 那一侧关掉自动收键盘。
    // 🔴 打开菜单**不收键盘**。
    //
    // 收键盘会让列表重排，而重排会把这一行从 composition 里换掉（列表回收），行里持有的
    // 「菜单开着没有」「锚点在哪」连同协程一起没了——菜单于是一次都不出现。真机日志里
    // 看得很清楚：openMenu 打印了，之后等待键盘收起的协程再也没有下文。
    // 这就是「回复完（键盘正开着）再长按最后几条，菜单不弹」的真正原因。
    //
    // 所以反过来：让键盘留着，一帧都不重排，菜单立刻就能开。代价是菜单要让开键盘那块
    // 区域——见 MessageActionsOverlayContent 的底部内缩。
    //
    // 更彻底的做法是把「哪条消息的菜单开着」提到页面一级，让它不随行的回收而消失；那是
    // 一次对这个组件的结构性改动，先不做。
    val openMenu: () -> Unit = remember(onMenuOpen) {
        {
            if (!visible) {
                visible = true
                onMenuOpen?.invoke()
            }
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                anchorBounds = coordinates.boundsInRoot()
            }
            .pointerInput(pointerInputKey) {
                // 🔴 长按必须按**屏幕坐标**判定，不能用组件内坐标。
                //
                // detectTapGestures 的长按在指针移动超过 touch slop 时取消，而它看的是组件
                // 内坐标：手指没动、**行自己动了**，它一样当成移动。而这正是常态——按在
                // 消息上会让输入框失去焦点、键盘收起、列表整体上移几百像素，于是键盘开着
                // 时长按永远出不来菜单（真机日志：只有 focusChanged，长按回调一次没进）。
                //
                // 这里把手指位置换算回根坐标（行的 boundsInRoot + 组件内坐标）：行移动时
                // 两者同步位移，差值为零，只有手指真的滑动才算移动。
                awaitEachGesture {
                    val slopPx = LONG_PRESS_SLOP.toPx()
                    fun rootOf(local: Offset): Offset =
                        (anchorBounds?.topLeft ?: Offset.Zero) + local

                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startRoot = rootOf(down.position)
                    val startedAt = currentTimeMillis()
                    while (true) {
                        val remaining = LONG_PRESS_TIMEOUT_MS - (currentTimeMillis() - startedAt)
                        if (remaining <= 0) {
                            openMenu()
                            break
                        }
                        val event = withTimeoutOrNull(remaining) { awaitPointerEvent() }
                        if (event == null) {
                            // 一直按着没有新事件 = 长按成立。
                            openMenu()
                            break
                        }
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break // 抬手：是点击，不是长按
                        val moved = rootOf(change.position) - startRoot
                        if (moved.getDistance() > slopPx) break // 真的在滑：交给列表滚动
                    }
                }
            },
    ) {
        CompositionLocalProvider(LocalMessageMenuTrigger provides openMenu) {
            bubble()
        }
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
    onDismiss: () -> Unit,
    bubble: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val gapPx = with(density) { Spacing.sm.roundToPx() }
    val edgePaddingPx = with(density) { 16.dp.roundToPx() }

    // overlay 的 Fullscreen 内容盒子被 safeArea 内缩过，遮罩需要向外反扩回去。
    val runtimeEnv = LocalRuntimeEnvironment.current
    val safeArea = runtimeEnv.safeArea
    val safeTopPx = with(density) { safeArea.top.roundToPx() }
    // 菜单不收键盘（收键盘会重排列表、把这一行连同菜单状态一起回收掉），所以键盘占的
    // 那块要当成底部内缩的一部分，否则动作列表会藏在键盘后面。
    val keyboardPx = with(density) { runtimeEnv.keyboard.height.roundToPx() }
    val safeBottomPx = maxOf(with(density) { safeArea.bottom.roundToPx() }, keyboardPx)
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
        // - 点击遮罩关闭菜单
        // - 拦截拖动手势，避免菜单弹出期间底层页面发生滚动
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = maskAlpha))
                .pointerInput(Unit) {
                    detectDragGestures { _, _ -> /* 消费拖动，阻断穿透 */ }
                }
                .clickable(onClick = onDismiss),
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
            .shadow(Spacing.xs, pillShape)
            .clip(pillShape)
            .background(colors.surface)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        reactions.forEach { emoji ->
            Box(
                modifier = Modifier
                    .size(Spacing.xxxl)
                    .clip(CircleShape)
                    .clickable { onReaction(emoji) },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, style = Theme.typography.titleExtraLarge)
            }
        }
        if (onMoreReactions != null) {
            Box(
                modifier = Modifier
                    .size(Spacing.xxxl)
                    .clip(CircleShape)
                    .background(colors.muted)
                    .clickable { onMoreReactions() },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "+", style = Theme.typography.titleExtraLarge, color = colors.mutedForeground)
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
            .shadow(Spacing.xs, shapes.xl)
            .clip(shapes.xl)
            .background(colors.surface)
            .border(1.dp, colors.border, shapes.xl)
            .padding(Spacing.sm),
    ) {
        actions.forEachIndexed { index, item ->
            val tint = when {
                item.disabled -> colors.mutedForeground
                item.danger -> colors.destructive
                else -> colors.foreground
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.sm)
                    .background(
                        if (pressedIndex == index) colors.muted else colors.surface,
                    )
                    .clickable(enabled = !item.disabled) {
                        onPressChange(index)
                        onActionClick(item)
                        onPressChange(null)
                    }
                    .padding(horizontal = Spacing.md, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Icon(name = item.icon, size = IconSizes.Default.lg, tint = tint)
                Text(
                    text = item.label,
                    style = Theme.typography.bodySmall,
                    color = tint,
                )
            }
        }
    }
}
