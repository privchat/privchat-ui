package com.netonstream.privchat.ui.runtime

import com.netonstream.privchat.ui.common.base.currentTimeMillis
import com.netonstream.privchat.ui.i18n.PrivChatStrings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 客户端运行时可靠性层（CLIENT_GLOBAL_STATE_AND_IDENTITY_STORE_SPEC §17.1–17.3 P4）。
 *
 * 三条稳定性主链的**统一真源**：连接（Connectivity）/ 同步（Sync）/ 发送队列（SendQueue）。
 * 事件由 app 层 `PrivChatSDKManager.handleSdkEvent` 喂入（SDK 事件：`connection_state_changed` /
 * `network_hint_changed` / `resume_sync_*` / `sync_entities_applied` / `message_send_status_changed` /
 * `outbound_queue_updated` / `forced_logout`），UI 只订阅 [ClientRuntime] 的 StateFlow——
 * **页面不得再各自判断 SDK 连接态 / 同步中 / 发送态**（spec §8 红线在运行态维度的展开）。
 *
 * 登出/切号调 [ClientRuntime.reset]（spec §14 防串号）。
 */

// ========== 统一错误模型（spec §17 / 用户错误提示 i18n 规范） ==========

/**
 * 客户端运行时错误（结构化，替代裸字符串在层间传递）。
 * UI 展示一律经 [userFacingMessage] 出**本地化文案**，禁止直出原始 reason / 异常串。
 */
sealed class ClientRuntimeError {
    data object NetworkUnavailable : ClientRuntimeError()
    data object GatewayDisconnected : ClientRuntimeError()
    data object AuthExpired : ClientRuntimeError()
    /** 服务端繁忙/限流（ErrorCode SystemBusy=2 / RateLimitExceeded=10300，或 reason 文本匹配）。 */
    data object ServerBusy : ClientRuntimeError()
    /** 同步失败（终态，transient 已在 app 层过滤不进这里）。rawReason 仅日志用，不给 UI。 */
    data class SyncFailed(val rawReason: String) : ClientRuntimeError()
    data class Unknown(val rawReason: String) : ClientRuntimeError()

    /** 本地化用户可见文案（错误提示 i18n 规范：禁泄露原始异常）。 */
    fun userFacingMessage(strings: PrivChatStrings): String = when (this) {
        NetworkUnavailable -> strings.bannerDisconnected
        GatewayDisconnected -> strings.bannerReconnecting
        AuthExpired -> strings.loginExpired
        ServerBusy -> strings.bannerServerBusy
        is SyncFailed -> strings.syncFailedRetry
        is Unknown -> strings.networkError
    }

    companion object {
        /**
         * 服务端繁忙判定（唯一入口）：protocol `ErrorCode.SystemBusy=2` / `RateLimitExceeded=10300`，
         * 或 reason 文本含 busy / rate limit / too many requests。
         */
        fun isServerBusySignal(errorCode: UInt?, reason: String?): Boolean {
            if (errorCode == 2u || errorCode == 10300u) return true
            val m = reason?.lowercase() ?: return false
            return m.contains("system busy") || m.contains("server busy") ||
                m.contains("rate limit") || m.contains("too many requests")
        }
    }
}

// ========== Connectivity（§17.2） ==========

data class ConnectivityState(
    /** 设备网络是否可达（系统网络监听，独立于 gateway）。 */
    val networkReachable: Boolean = true,
    /** TCP/WS gateway 是否已连接（未必已认证）。 */
    val gatewayConnected: Boolean = false,
    /** SDK 会话是否已认证（AUTHENTICATED）。 */
    val authenticated: Boolean = false,
    /** 是否处于重连周期（曾建立过会话，当前掉线/握手中）。 */
    val reconnecting: Boolean = false,
    /** 本轮重连尝试次数（认证成功清零）。 */
    val reconnectAttempt: Int = 0,
    /**
     * 服务端繁忙/限流（[ClientRuntimeError.isServerBusySignal]：SystemBusy=2 / RateLimitExceeded=10300
     * / reason 文本）。置位后由下一个成功信号自动清除（认证成功 / resume sync 完成 / 消息发送成功）。
     */
    val serverBusy: Boolean = false,
    val lastConnectedAt: Long? = null,
    val lastDisconnectedAt: Long? = null,
    val lastError: ClientRuntimeError? = null,
)

// ========== Sync（§17.1） ==========

enum class SyncEntityKind { CONVERSATIONS, MESSAGES, CONTACTS, GROUPS, PRESENCE, OTHER }

enum class EntitySyncStatus { IDLE, LOADING, FRESH, FAILED }

data class EntitySyncState(
    val status: EntitySyncStatus = EntitySyncStatus.IDLE,
    val lastCompletedAt: Long? = null,
)

data class SyncState(
    /** 首次（登录后）同步是否完成——完成前 UI 显示本地 snapshot + sync indicator。 */
    val initialSyncCompleted: Boolean = false,
    /** resume sync（断线重连差量恢复）是否进行中。 */
    val resumeSyncRunning: Boolean = false,
    val lastSyncAt: Long? = null,
    /** 终态同步错误（transient 已过滤）；UI 经 userFacingMessage 展示。 */
    val globalError: ClientRuntimeError? = null,
    val entities: Map<SyncEntityKind, EntitySyncState> = emptyMap(),
)

// ========== Send queue（§17.3） ==========

/**
 * 发送队列**运行时摘要**（summary facade）——**不替代**既有持久化发送链
 * （SQLite Pending/Failed 状态、ack 回填 serverMessageId/messageSeq、气泡失败重试 `retryMessage`、
 * 重启恢复），那条链是发送状态真源；这里只聚合全局可见的队列深度与最近失败线索。
 * 完整 MessageSendStore（per-message 聚合 facade）留后续，只做聚合不重写发送链。
 */
data class SendQueueState(
    /** SDK outbound 队列深度（`outbound_queue_updated.queued`，断网排队的真实来源）。 */
    val outboundQueued: Long = 0,
    /** 本会话内最近一次发送失败（气泡级重试仍走消息列表；这里供全局提示/诊断）。 */
    val lastFailureAt: Long? = null,
    val lastFailureMessageId: ULong? = null,
)

// ========== 全局对象 ==========

object ClientRuntime {
    private val _connectivity = MutableStateFlow(ConnectivityState())
    val connectivity: StateFlow<ConnectivityState> = _connectivity.asStateFlow()

    private val _sync = MutableStateFlow(SyncState())
    val sync: StateFlow<SyncState> = _sync.asStateFlow()

    private val _send = MutableStateFlow(SendQueueState())
    val send: StateFlow<SendQueueState> = _send.asStateFlow()

    /** 是否建立过认证会话（区分「首次连接中」与「重连中」）。 */
    private var hadSession = false

    // ---- Connectivity 喂入口（app handleSdkEvent） ----

    /** `connection_state_changed`：直接吃 SDK raw 态（authenticated/connected/connecting/disconnected/shutdown/new）。 */
    fun onConnectionStateChanged(toStateRaw: String) {
        val now = currentTimeMillis()
        val cur = _connectivity.value
        _connectivity.value = when (toStateRaw.lowercase()) {
            "authenticated", "loggedin" -> {
                hadSession = true
                cur.copy(
                    gatewayConnected = true,
                    authenticated = true,
                    reconnecting = false,
                    reconnectAttempt = 0,
                    serverBusy = false, // 成功信号清 busy
                    lastConnectedAt = now,
                    lastError = null,
                )
            }
            "connected" -> cur.copy(gatewayConnected = true, authenticated = false, reconnecting = hadSession)
            "connecting" -> cur.copy(
                gatewayConnected = false,
                authenticated = false,
                reconnecting = hadSession,
                reconnectAttempt = if (hadSession) cur.reconnectAttempt + 1 else cur.reconnectAttempt,
            )
            else -> cur.copy( // disconnected / shutdown / new
                gatewayConnected = false,
                authenticated = false,
                reconnecting = hadSession,
                lastDisconnectedAt = now,
                lastError = if (cur.networkReachable) ClientRuntimeError.GatewayDisconnected else ClientRuntimeError.NetworkUnavailable,
            )
        }
    }

    /** `network_hint_changed`（SDK 侧探测）+ 系统网络监听：设备可达性。 */
    fun onNetworkReachableChanged(reachable: Boolean) {
        val cur = _connectivity.value
        _connectivity.value = cur.copy(
            networkReachable = reachable,
            lastError = if (!reachable) ClientRuntimeError.NetworkUnavailable else cur.lastError,
        )
    }

    /** `forced_logout` / token 终态失效。 */
    fun onAuthExpired() {
        _connectivity.value = _connectivity.value.copy(
            authenticated = false,
            reconnecting = false,
            lastError = ClientRuntimeError.AuthExpired,
        )
    }

    /**
     * 服务端繁忙/限流信号（来源：resume_sync_failed / message send failed 的
     * [ClientRuntimeError.isServerBusySignal] 命中）。由下一个成功信号自动清除。
     */
    fun onServerBusySignal() {
        _connectivity.value = _connectivity.value.copy(
            serverBusy = true,
            lastError = ClientRuntimeError.ServerBusy,
        )
    }

    // ---- Sync 喂入口 ----

    fun onResumeSyncStarted() {
        _sync.value = _sync.value.copy(resumeSyncRunning = true, globalError = null)
    }

    fun onResumeSyncCompleted() {
        _sync.value = _sync.value.copy(
            resumeSyncRunning = false,
            initialSyncCompleted = true,
            lastSyncAt = currentTimeMillis(),
            globalError = null,
        )
        clearServerBusy() // 成功信号清 busy
    }

    /** 终态失败才进（transient 由 app 层 `isTransientSyncReason` 过滤，与 dialog 分级一致）。 */
    fun onResumeSyncFailed(rawReason: String) {
        _sync.value = _sync.value.copy(
            resumeSyncRunning = false,
            globalError = ClientRuntimeError.SyncFailed(rawReason),
        )
    }

    /** 首次登录链路（SDKLoginState → SYNC_READY）也标记初始同步完成。 */
    fun markInitialSyncCompleted() {
        if (_sync.value.initialSyncCompleted) return
        _sync.value = _sync.value.copy(initialSyncCompleted = true, lastSyncAt = currentTimeMillis())
    }

    /** `sync_entities_applied` / `sync_entity_changed`：per-entity 新鲜度。 */
    fun onEntitySynced(entityTypeRaw: String) {
        val kind = mapEntityKind(entityTypeRaw)
        val cur = _sync.value
        _sync.value = cur.copy(
            entities = cur.entities + (kind to EntitySyncState(EntitySyncStatus.FRESH, currentTimeMillis())),
        )
    }

    // ---- Send 喂入口 ----

    /** `outbound_queue_updated.queued`：SDK 出站队列深度（断网排队真实来源）。 */
    fun onOutboundQueueUpdated(queued: Long) {
        _send.value = _send.value.copy(outboundQueued = queued)
    }

    /** `message_send_status_changed`：仅记全局失败线索；单条气泡状态仍由消息列表（SQLite 持久化）驱动。 */
    fun onMessageSendFailed(messageId: ULong?) {
        _send.value = _send.value.copy(
            lastFailureAt = currentTimeMillis(),
            lastFailureMessageId = messageId,
        )
    }

    /** 消息发送成功（status=Sent）：成功信号清 serverBusy。 */
    fun onMessageSendSucceeded() {
        clearServerBusy()
    }

    private fun clearServerBusy() {
        val cur = _connectivity.value
        if (!cur.serverBusy && cur.lastError !is ClientRuntimeError.ServerBusy) return
        _connectivity.value = cur.copy(
            serverBusy = false,
            lastError = if (cur.lastError is ClientRuntimeError.ServerBusy) null else cur.lastError,
        )
    }

    // ---- 生命周期 ----

    /** 登出/切号：清空（spec §14 防串号）。 */
    fun reset() {
        hadSession = false
        _connectivity.value = ConnectivityState(networkReachable = _connectivity.value.networkReachable)
        _sync.value = SyncState()
        _send.value = SendQueueState()
    }

    private fun mapEntityKind(raw: String): SyncEntityKind = when (raw.lowercase()) {
        "channel", "channel_extra", "channel_unread", "channel_read_cursor" -> SyncEntityKind.CONVERSATIONS
        "message", "message_send", "message_read", "message_reaction", "message_revoke", "message_extra" -> SyncEntityKind.MESSAGES
        "friend", "user", "user_block", "friend_request" -> SyncEntityKind.CONTACTS
        "group", "group_member", "channel_member" -> SyncEntityKind.GROUPS
        "presence" -> SyncEntityKind.PRESENCE
        else -> SyncEntityKind.OTHER
    }
}

// ========== 运行时状态条（优先级固定，勿改序） ==========

enum class RuntimeBannerKind { AUTH_EXPIRED, OFFLINE, RECONNECTING, CONNECTING, SERVER_BUSY, SYNCING, CONNECTED, HIDDEN }

/**
 * 状态条唯一决策函数（纯函数，可单测）。**优先级固定（P4.1 拍板，改序须过评审）**：
 *
 *   AuthExpired/ForcedLogout > NetworkUnavailable(设备断网) > Reconnecting(曾有会话掉线/握手)
 *   > Connecting(首次连接) > ServerBusy > ResumeSyncRunning > Connected(短暂) > Hidden
 *
 * 说明：
 * - AuthExpired 最高：被踢下线绝不能显示「连接中/同步中」。
 * - 设备断网压过一切连接语义：断网时绝不显示「同步中」。
 * - ServerBusy 在已认证会话内提示（未认证时连接语义优先）。
 * - [hasStartedConnectionFlow] 抑制登录前噪声；[showConnectedBanner] 控制绿条短暂显示窗口。
 */
fun resolveRuntimeBanner(
    connectivity: ConnectivityState,
    sync: SyncState,
    hasStartedConnectionFlow: Boolean,
    showConnectedBanner: Boolean,
): RuntimeBannerKind = when {
    connectivity.lastError is ClientRuntimeError.AuthExpired && hasStartedConnectionFlow ->
        RuntimeBannerKind.AUTH_EXPIRED
    !connectivity.networkReachable && hasStartedConnectionFlow -> RuntimeBannerKind.OFFLINE
    connectivity.authenticated -> when {
        connectivity.serverBusy -> RuntimeBannerKind.SERVER_BUSY
        sync.resumeSyncRunning -> RuntimeBannerKind.SYNCING
        showConnectedBanner -> RuntimeBannerKind.CONNECTED
        else -> RuntimeBannerKind.HIDDEN
    }
    connectivity.reconnecting && hasStartedConnectionFlow -> RuntimeBannerKind.RECONNECTING
    connectivity.gatewayConnected -> RuntimeBannerKind.CONNECTING
    // 首次连接尚未成功（本会话从未认证过 → lastConnectedAt==null）：显示「连接中」而非
    // 「网络已断开」。用户刚打开 app、连接流程仍在进行，还没连过就提示断网是误导。
    // 真正的设备离线由上面 networkReachable=false 分支覆盖；曾连上后掉线由 reconnecting 覆盖。
    hasStartedConnectionFlow && connectivity.lastConnectedAt == null -> RuntimeBannerKind.CONNECTING
    hasStartedConnectionFlow -> RuntimeBannerKind.OFFLINE
    else -> RuntimeBannerKind.HIDDEN
}
