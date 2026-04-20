package com.netonstream.privchat.ui.platform

/**
 * 控制系统外壳（状态栏 / 底部导航栏）的可见性。
 *
 * 典型使用场景：弹出全屏遮罩（如长按消息的动作菜单）时临时隐藏系统栏，
 * 避免系统 UI 元素穿透在遮罩之上。
 */
expect object SystemChromeController {
    /** 隐藏或显示系统栏。iOS/其它平台可为 no-op。 */
    fun setSystemBarsHidden(hidden: Boolean)
}
