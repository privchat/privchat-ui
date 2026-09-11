package com.netonstream.privchat.ui.platform

/**
 * 轻触反馈。
 *
 * 只做一种强度：索引条划过字母、拨轮跨过一格这类"位置变了"的提示。
 * 强弱两档、成功/失败语义那套等真有第二个场景再加——现在多一档就是多一个没人能
 * 说清何时该用哪个的选项。
 *
 * 静默失败是有意的：设备没有马达、系统关了触感、或宿主没注册 Context，都不该让
 * 一次列表滚动抛异常。
 */
expect object HapticBridge {
    fun selectionChanged()
}
