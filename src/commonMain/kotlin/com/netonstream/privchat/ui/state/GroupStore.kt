package com.netonstream.privchat.ui.state

import androidx.compose.runtime.mutableStateMapOf
import com.netonstream.privchat.sdk.dto.GroupMemberEntry
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.models.SystemUser

/**
 * 群成员单一真源（P6-1，CLIENT_GLOBAL_STATE §22）。
 *
 * 收口此前 App 群成员的 **3 套互不同步来源**：
 * - `PrivChat.groupMembers`（扁平单列表，非 channel-keyed，消费者自 filter）
 * - `selectedGroupMembers`（导航宿主 remember，3 页共享）
 * - `GroupCollageAvatar` 私有 `GroupMemberPreviewCache`（九宫格/成员数，**不过滤系统用户**）
 *
 * 本 store 是 **channel-keyed + 系统用户已过滤** 的唯一权威：
 * - 会话列表九宫格 / 成员数 / 群成员页 都读这里，口径一致；
 * - 系统账号（user_type==1）绝不进入九宫格、成员数、成员列表（GPT 红线）。
 *
 * 写入两条路径：
 * 1. App `loadGroupMembers` 拉取 canonical 投影并过滤后 `setMembers`（成员页/主动刷新）；
 * 2. `ensureMembers` 只读本地投影（会话列表九宫格不得触发网络）。
 *
 * 放在 ui 层（与 PrivChat/ConversationPage/GroupCollageAvatar 同层）：truth 在 ui，
 * app 层只写不另存，避免双真源（对齐 presence 既有模式）。
 */
object GroupStore {
    // mutableStateMapOf：写入后引用它的 Composable 自动重组。value 已过滤系统用户。
    private val membersByChannel = mutableStateMapOf<ULong, List<GroupMemberEntry>>()
    private val inFlight = mutableSetOf<ULong>()

    /** 已过滤系统用户的群成员（未加载过 → 空）。 */
    fun members(channelId: ULong): List<GroupMemberEntry> = membersByChannel[channelId].orEmpty()

    /** 九宫格前 9（role 权重 owner<admin<member → userId），已过滤系统用户。 */
    fun preview(channelId: ULong): List<GroupMemberEntry> = topNine(members(channelId))

    /** 成员数（已过滤系统用户；0 = 尚未加载）。群标题「名称 (人数)」的数据源。 */
    fun memberCount(channelId: ULong): Int = membersByChannel[channelId]?.size ?: 0

    /**
     * App `loadGroupMembers` hydrate+过滤后回写（成员页/主动刷新路径）。
     * 传入的 [members] 应已由调用方过滤系统用户；这里作为该 channel 的权威覆盖。
     */
    fun setMembers(channelId: ULong, members: List<GroupMemberEntry>) {
        membersByChannel[channelId] = members
    }

    /**
     * 惰性读取（九宫格 / 会话列表未打开过的群）：只读本地 canonical 投影。
     *
     * 会话列表可能同时渲染几十或几百个群。这里发 `syncGroupMembers`
     * 会把启动和打开聊天退化成 O(group_count) 个全量 roster RPC。完整成员
     * 同步只能由成员/设置页面的显式入口发起。
     */
    suspend fun ensureMembers(channelId: ULong) {
        if (membersByChannel.containsKey(channelId) || !inFlight.add(channelId)) return
        try {
            // 只取九宫格需要的那一页。协议支持分页后仍传 null 等于要整份花名册——
            // 750 人的群一次 126 KB，而这里只会用到前 9 个。
            //
            // 多取一些余量（TILE_PAGE_SIZE > 9）：topNine 会先滤掉系统用户再排序，
            // 正好取 9 个的话滤完可能不够铺满九宫格。
            // 参数是 (limit, offset)，不是 (page, size)——写反就是「从第 20 个开始只取 1 个」，
            // 九宫格会静默变成一个头像，而且没有任何报错。
            val raw = PrivChat.client
                .getGroupMembers(channelId, TILE_PAGE_SIZE, 0u)
                .getOrNull().orEmpty()
            if (raw.isNotEmpty()) {
                membersByChannel[channelId] = raw.filterNot { SystemUser.isSystemType(it.userType) }
            }
        } catch (_: Throwable) {
            // 失败静默：inFlight 已释放，下次进入会话列表重试
        } finally {
            inFlight.remove(channelId)
        }
    }

    /**
     * 九宫格惰性拉取的页大小。九宫格用 9 个，这里多取一截给系统用户过滤留余量。
     * 完整名单由成员页显式拉，不走这条路径。
     */
    private const val TILE_PAGE_SIZE: UInt = 20u

    /** 登出/切号清空，防串号。 */
    fun clear() {
        membersByChannel.clear()
        inFlight.clear()
    }

    // 群九宫格取「最早入群」的 9 人(微信规则:按 joinedAt 升序,与活跃度无关)。
    // joinedAt 未知(0)排最后,不让缺时间戳的成员抢占前排;同刻用 userId 定序稳定。
    private fun topNine(members: List<GroupMemberEntry>): List<GroupMemberEntry> =
        members
            .sortedWith(
                compareBy(
                    { if (it.joinedAt <= 0L) Long.MAX_VALUE else it.joinedAt },
                    { it.userId },
                )
            )
            .take(9)
}
