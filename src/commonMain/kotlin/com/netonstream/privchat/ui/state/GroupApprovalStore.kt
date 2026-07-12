package com.netonstream.privchat.ui.state

import androidx.compose.runtime.mutableStateMapOf
import com.netonstream.privchat.sdk.dto.GroupApprovalItemView
import com.netonstream.privchat.ui.PrivChat

/**
 * 群入群申请审批状态（P6-3，CLIENT_GLOBAL_STATE §26）。
 *
 * channel-keyed 待处理审批列表 + 总数。第一版拉取式（进页 refresh / 处理后本地移除并可再 refresh），
 * 无推送——server 群审批暂无事件（见 task「群审批推送缺口」）。
 *
 * BadgeState.groupRequests 读 [totalPending]：只统计**已加载过**的群（未访问的群不主动拉，避免 N 次 list）。
 * 放 ui 层（与 PrivChat/GroupStore 同层，UI 直接消费）。
 */
object GroupApprovalStore {
    private val approvalsByGroup = mutableStateMapOf<ULong, List<GroupApprovalItemView>>()
    private val loading = mutableStateMapOf<ULong, Boolean>()

    fun approvals(groupId: ULong): List<GroupApprovalItemView> = approvalsByGroup[groupId].orEmpty()

    fun isLoading(groupId: ULong): Boolean = loading[groupId] == true

    /** 已加载各群待处理审批总数（BadgeState.groupRequests 源）。 */
    fun totalPending(): Int = approvalsByGroup.values.sumOf { it.size }

    /** 拉取某群待处理审批（仅群主/管理员，服务端鉴权）。 */
    suspend fun refresh(groupId: ULong): Result<Unit> {
        loading[groupId] = true
        val result = PrivChat.client.listGroupApprovals(groupId)
        loading[groupId] = false
        return result.map { view -> approvalsByGroup[groupId] = view.approvals }
    }

    /**
     * 处理审批（approve/reject）。[requestId] 是 [GroupApprovalItemView.requestId]（server UUID）。
     * 成功后本地移除该条（乐观），角标随之下降；调用方可再 [refresh] 兜底。
     */
    suspend fun handle(
        groupId: ULong,
        requestId: String,
        approve: Boolean,
        reason: String? = null,
    ): Result<Boolean> =
        PrivChat.client.handleGroupApproval(requestId, approve, reason).onSuccess { ok ->
            if (ok) approvalsByGroup[groupId] = approvals(groupId).filterNot { it.requestId == requestId }
        }

    /** 登出/切号清空，防串号。 */
    fun clear() {
        approvalsByGroup.clear()
        loading.clear()
    }
}
