package com.netonstream.privchat.ui.runtime

import com.netonstream.privchat.sdk.SessionPhase
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

    /**
     * 上一份被接受的会话快照身份（账号 uid + 会话世代），由 SDK 权威给出。
     *
     * 为什么不能由本层自己发号：本层看不到「同一个 client 原地切号」，也看不到
     * 「同一账号被强制登出后重新登录」——后者 uid 一模一样，但那是新会话。世代必须
     * 来自 Rust actor，它是唯一知道会话何时真正重建的地方。
     */
    private var lastAccountUid: String? = null
    private var lastSessionEpoch: Long = -1L

    /**
     * 用 SDK 的权威会话快照对账连接状态。
     *
     * 存在的理由：`reconnecting` 原本只由 `connection_state_changed → authenticated`
     * 这个**边沿**清除。连接若在宿主监听建立之前就恢复、或恢复期间根本没产生状态跃迁，
     * 那个边沿就永远不会再来——横幅一旦亮起就再也熄不掉，而 SDK 其实在正常收发。
     * 边沿是通知，状态才是事实；这里让事实兜底。
     *
     * 三条规则：
     *  1. **只有 [SessionPhase.Authenticated] 能清 `reconnecting`。** Connected /
     *     LoggedIn 都可能跑在服务端尚未授权的通道上，据此撤横幅等于谎报就绪。
     *  2. **陈旧快照丢弃。** 世代比已接受的小 = 上一次会话在途的采样，不许覆盖当前会话。
     *  3. **终态 sticky。** 强制登出写下的 [ClientRuntimeError.AuthExpired] 只能被
     *     **新会话世代**清除。晚到的 Authenticated 快照属于旧会话，绝不能把它抹掉——
     *     否则用户会看到「已被踢下线」闪一下就变回正常，然后所有请求全是 401。
     */
    fun reconcileSessionSnapshot(phase: SessionPhase, accountUid: String?, sessionEpoch: Long) {
        if (sessionEpoch < lastSessionEpoch) return // 规则 2：陈旧快照
        val isNewSession = sessionEpoch > lastSessionEpoch || accountUid != lastAccountUid
        lastSessionEpoch = sessionEpoch
        lastAccountUid = accountUid
        _connectivity.value = reduceSessionPhase(_connectivity.value, phase, isNewSession)
        if (phase == SessionPhase.Authenticated) hadSession = true
    }

    /**
     * 完整对账 reducer：把 [ConnectivityState] 中**所有**由会话阶段决定的字段一次算清。
     *
     * 只修 `authenticated` 是不够的——那样 `gatewayConnected` 之类的残留会永久留在
     * 状态里（阶段回到 New 却仍标着 gateway 已连）。对账的意思是「以权威状态为准重算」，
     * 不是「挑一个字段修一下」。
     *
     * `networkReachable` 不在此列：它是宿主的系统 reachability 镜像，会话阶段不是它的
     * 真源。唯一例外是 Authenticated——收发都通了，任何说「没网」的镜像都已被事实证伪。
     */
    internal fun reduceSessionPhase(
        cur: ConnectivityState,
        phase: SessionPhase,
        isNewSession: Boolean,
    ): ConnectivityState {
        // 规则 3：终态只有新会话能清。
        val terminal = cur.lastError is ClientRuntimeError.AuthExpired && !isNewSession
        return when (phase) {
            SessionPhase.Authenticated -> {
                if (terminal) cur else cur.copy(
                    gatewayConnected = true,
                    authenticated = true,
                    networkReachable = true,
                    reconnecting = false,
                    reconnectAttempt = 0,
                    serverBusy = false,
                    lastConnectedAt = currentTimeMillis(),
                    lastError = null,
                )
            }
            // transport 通了但未获授权：gateway 为真、authenticated 必须为假。
            // `reconnecting` 不动——「是否在重连」由事件与 hadSession 决定，这里不越权。
            SessionPhase.Connected, SessionPhase.LoggedIn ->
                cur.copy(gatewayConnected = true, authenticated = false)
            // 无会话 / 已终止 / 已关停：连接维度全部归零。
            SessionPhase.New, SessionPhase.Terminated, SessionPhase.Shutdown ->
                cur.copy(gatewayConnected = false, authenticated = false)
        }
    }

    // ---- Connectivity 喂入口（app handleSdkEvent） ----

    /** `connection_state_changed`：直接吃 SDK raw 态（authenticated/connected/connecting/disconnected/shutdown/new）。 */
    fun onConnectionStateChanged(toStateRaw: String) {
        val now = currentTimeMillis()
        val cur = _connectivity.value
        _connectivity.value = when (toStateRaw.lowercase()) {
            // ⚠️ `loggedin` **不是**已认证。Rust 侧写死了这条：`Connected/LoggedIn` 都可能
            // 跑在服务端尚未授权的通道上（重连刚握好 TCP、ConnAuth 还没回），此时发业务
            // RPC 只会拿到 10000。把它并进这里会让横幅在「连上但没鉴权」的窗口里提前撤下，
            // 用户以为能发消息，实际发不出去。
            "authenticated" -> {
                hadSession = true
                cur.copy(
                    gatewayConnected = true,
                    authenticated = true,
                    // 认证成功是「网络确实可达」的铁证——iOS reachability 可能漏发恢复回调而
                    // 卡在 unreachable(模拟器/真机挂起后常见),若不校正,横幅会在收着消息时仍
                    // 显示「网络已断开」。用真实连接结果复位镜像,断了自会由后续 disconnected 置回。
                    networkReachable = true,
                    reconnecting = false,
                    reconnectAttempt = 0,
                    serverBusy = false, // 成功信号清 busy
                    lastConnectedAt = now,
                    lastError = null,
                )
            }
            "connected", "loggedin" -> cur.copy(
                gatewayConnected = true,
                authenticated = false,
                reconnecting = hadSession,
            )
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
        // 身份忘掉，但**不要**伪造世代：世代的真源在 Rust actor。这里只是让下一份
        // 快照必然被判为「新会话」，从而允许它清掉本次登出留下的终态。
        lastAccountUid = null
        lastSessionEpoch = -1L
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
 *   AuthExpired/ForcedLogout > Authenticated(ServerBusy > ResumeSyncRunning > Connected > Hidden)
 *   > NetworkUnavailable(设备断网) > Reconnecting(曾有会话掉线/握手) > Connecting(首次连接)
 *   > Offline(无会话) > Hidden
 *
 * 说明：
 * - AuthExpired 最高：被踢下线绝不能显示「连接中/同步中」。
 * - **已认证优先于宿主 reachability 镜像**：镜像会永久卡 unreachable，认证态才是「连接活着」
 *   的真值；否则会出现「一边收消息一边提示断网」。真断线由 SDK 事件把 authenticated 置回 false。
 * - 未认证时才看设备断网：此时绝不显示「同步中」。
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
    // 已认证 = 存在活的、能收发的连接,这是比宿主 reachability 镜像更强的真值:即便系统
    // reachability 卡在 unreachable(漏发恢复回调),也绝不显示「网络已断开」——否则会出现
    // 「一边收着消息一边提示断网」的自相矛盾(用户实测的架构 bug)。故 authenticated 分支
    // 前置于 networkReachable 分支;真断线由 SDK 的 disconnected 事件把 authenticated 置回 false。
    connectivity.authenticated -> when {
        connectivity.serverBusy -> RuntimeBannerKind.SERVER_BUSY
        sync.resumeSyncRunning -> RuntimeBannerKind.SYNCING
        showConnectedBanner -> RuntimeBannerKind.CONNECTED
        else -> RuntimeBannerKind.HIDDEN
    }
    !connectivity.networkReachable && hasStartedConnectionFlow -> RuntimeBannerKind.OFFLINE
    connectivity.reconnecting && hasStartedConnectionFlow -> RuntimeBannerKind.RECONNECTING
    connectivity.gatewayConnected -> RuntimeBannerKind.CONNECTING
    // 首次连接尚未成功（本会话从未认证过 → lastConnectedAt==null）：显示「连接中」而非
    // 「网络已断开」。用户刚打开 app、连接流程仍在进行，还没连过就提示断网是误导。
    // 真正的设备离线由上面 networkReachable=false 分支覆盖；曾连上后掉线由 reconnecting 覆盖。
    hasStartedConnectionFlow && connectivity.lastConnectedAt == null -> RuntimeBannerKind.CONNECTING
    hasStartedConnectionFlow -> RuntimeBannerKind.OFFLINE
    else -> RuntimeBannerKind.HIDDEN
}
