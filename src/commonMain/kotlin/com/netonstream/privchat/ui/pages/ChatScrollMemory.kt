package com.netonstream.privchat.ui.pages

/**
 * 会话列表的滚动位置记忆（按 channel）。
 *
 * 为什么需要它：从会话页打开图片预览再返回，页面会被重新组合，`remember` 的
 * 列表状态和「是否已完成首次定位」一起归零，于是「首次进入滚到底」又跑了一遍——
 * 用户往上翻了几十页看历史，点开一张图再关掉，直接被扔回最底部。
 *
 * 位置属于「这个会话被浏览到哪儿了」，不属于某一次页面组合的生命周期，所以它得
 * 活在组合之外。按 channel 存，切走再回来仍是原处。
 */
object ChatScrollMemory {

    data class Position(val index: Int, val offset: Int)

    private val positions = mutableMapOf<ULong, Position>()

    /** 记住某个会话当前停在哪儿。 */
    fun remember(channelId: ULong, index: Int, offset: Int) {
        positions[channelId] = Position(index, offset)
    }

    /** 取回上次的位置；没有记录返回 null（调用方按「首次进入」处理，定位到底部）。 */
    fun restore(channelId: ULong): Position? = positions[channelId]

    /**
     * 丢弃某个会话的记忆。
     *
     * 真正意义上重新进入会话（从会话列表点进来）时调用，语义是「这次要按最新消息
     * 定位」；而从预览页返回不该调它。
     */
    fun forget(channelId: ULong) {
        positions.remove(channelId)
    }

    /** 退出登录 / 切换账号：位置属于上一个账号，不能带到下一个账号。 */
    fun clear() {
        positions.clear()
    }
}
