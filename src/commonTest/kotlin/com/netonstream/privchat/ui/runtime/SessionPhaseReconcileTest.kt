package com.netonstream.privchat.ui.runtime

import com.netonstream.privchat.sdk.SessionPhase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 「服务器重连中」横幅永不熄灭的回归，以及对账本身的正确性。
 *
 * 原始缺陷是两条：
 *  1. `reconnecting` 只由 `connection_state_changed → authenticated` 这个**边沿**清除。
 *     连接若在事件流挂上之前就恢复、或恢复期间没产生状态跃迁，边沿就再也不会来，
 *     横幅永远亮着而 SDK 其实在正常收发。修法是让**状态**兜底。
 *  2. 事件消费把 `loggedin` 当成已认证。Rust 侧写死了 `Connected/LoggedIn` 都可能跑在
 *     服务端尚未授权的通道上，据此撤横幅等于谎报就绪。
 *
 * 对账本身又有三个必须守住的性质，各自对应一个真实的失败模式：
 *  - **完整 reducer**：只修 `authenticated` 会把 `gatewayConnected` 之类的残留永久留下。
 *  - **终态 sticky**：晚到的 Authenticated 快照不能抹掉强制登出。
 *  - **世代防串号**：世代来自 Rust actor，同一个 client 原地切号也能识别。
 */
class SessionPhaseReconcileTest {

    @BeforeTest fun setUp() = ClientRuntime.reset()
    @AfterTest fun tearDown() = ClientRuntime.reset()

    /** 模拟 SDK 推来的一帧快照。uid/epoch 由 Rust actor 生成，这里照搬其语义。 */
    private fun push(phase: SessionPhase, uid: String? = "1001", epoch: Long = 1) =
        ClientRuntime.reconcileSessionSnapshot(phase, uid, epoch)

    // ---- 缺陷 2：loggedin ≠ authenticated ----

    @Test
    fun loggedin_event_does_not_mark_authenticated() {
        ClientRuntime.onConnectionStateChanged("loggedin")
        val s = ClientRuntime.connectivity.value
        assertFalse(s.authenticated, "LoggedIn 尚未鉴权，此时发业务 RPC 只会拿到 10000")
        assertTrue(s.gatewayConnected, "但 transport 确实通了")
    }

    @Test
    fun authenticated_event_still_marks_authenticated() {
        ClientRuntime.onConnectionStateChanged("authenticated")
        assertTrue(ClientRuntime.connectivity.value.authenticated)
    }

    // ---- 缺陷 1：边沿丢失后由状态兜底 ----

    @Test
    fun reconcile_clears_reconnecting_when_edge_never_arrives() {
        ClientRuntime.onConnectionStateChanged("authenticated")
        ClientRuntime.onConnectionStateChanged("disconnected")
        assertTrue(ClientRuntime.connectivity.value.reconnecting, "前提：横幅确实亮着")

        // 连接其实已经恢复，但 authenticated 事件永远不来（边沿丢失）。
        push(SessionPhase.Authenticated)

        val s = ClientRuntime.connectivity.value
        assertFalse(s.reconnecting, "状态对账必须能在边沿丢失时熄灭横幅")
        assertTrue(s.authenticated)
        assertEquals(null, s.lastError)
    }

    @Test
    fun unauthenticated_phases_never_clear_reconnecting() {
        ClientRuntime.onConnectionStateChanged("authenticated")
        ClientRuntime.onConnectionStateChanged("disconnected")

        push(SessionPhase.Connected)
        assertTrue(ClientRuntime.connectivity.value.reconnecting, "Connected 未鉴权，不得撤横幅")

        push(SessionPhase.LoggedIn)
        assertTrue(ClientRuntime.connectivity.value.reconnecting, "LoggedIn 同样未鉴权")

        push(SessionPhase.Authenticated)
        assertFalse(ClientRuntime.connectivity.value.reconnecting)
    }

    // ---- 完整 reducer ----

    @Test
    fun reducer_clears_gateway_when_phase_returns_to_new() {
        push(SessionPhase.Authenticated)
        assertTrue(ClientRuntime.connectivity.value.gatewayConnected)

        push(SessionPhase.New)
        val s = ClientRuntime.connectivity.value
        assertFalse(s.authenticated)
        assertFalse(s.gatewayConnected, "只修 authenticated 会把 gatewayConnected 的残留永久留下")
    }

    @Test
    fun reducer_asserts_gateway_true_while_connected_but_unauthorised() {
        push(SessionPhase.New)
        push(SessionPhase.Connected)
        val s = ClientRuntime.connectivity.value
        assertTrue(s.gatewayConnected, "transport 通了，gateway 必须为真")
        assertFalse(s.authenticated, "但尚未获授权")
    }

    @Test
    fun reducer_clears_gateway_on_terminated_and_shutdown() {
        for (terminal in listOf(SessionPhase.Terminated, SessionPhase.Shutdown)) {
            ClientRuntime.reset()
            push(SessionPhase.Authenticated)
            push(terminal)
            val s = ClientRuntime.connectivity.value
            assertFalse(s.gatewayConnected, "$terminal 之后连接维度必须归零")
            assertFalse(s.authenticated)
        }
    }

    // ---- 终态 sticky ----

    @Test
    fun late_authenticated_snapshot_cannot_erase_forced_logout() {
        push(SessionPhase.Authenticated, uid = "1001", epoch = 5)
        ClientRuntime.onAuthExpired()
        assertEquals(ClientRuntimeError.AuthExpired, ClientRuntime.connectivity.value.lastError)

        // 强制登出之前采样、之后才到达的那一帧：同 client、同账号、同世代。
        push(SessionPhase.Authenticated, uid = "1001", epoch = 5)
        val s = ClientRuntime.connectivity.value
        assertEquals(
            ClientRuntimeError.AuthExpired, s.lastError,
            "晚到的 Authenticated 属于旧会话，不能把「已被踢下线」抹成正常",
        )
        assertFalse(s.authenticated, "更不能重新标成已认证——后续请求全是 401")
    }

    @Test
    fun terminal_state_survives_a_later_terminated_snapshot() {
        push(SessionPhase.Authenticated, epoch = 5)
        ClientRuntime.onAuthExpired()
        push(SessionPhase.Terminated, epoch = 5)
        assertEquals(
            ClientRuntimeError.AuthExpired, ClientRuntime.connectivity.value.lastError,
            "Terminated 只拉低布尔值，不得顺手把终态清掉",
        )
    }

    @Test
    fun a_new_session_epoch_clears_the_terminal_state() {
        push(SessionPhase.Authenticated, epoch = 5)
        ClientRuntime.onAuthExpired()

        // 用户重新登录同一个账号：uid 没变，但 Rust actor 自增了世代 —— 这是新会话。
        push(SessionPhase.Authenticated, uid = "1001", epoch = 6)
        val s = ClientRuntime.connectivity.value
        assertEquals(null, s.lastError, "显式新会话是唯一能清掉终态的事件")
        assertTrue(s.authenticated)
    }

    // ---- 防串号 ----

    @Test
    fun stale_epoch_snapshot_is_discarded() {
        push(SessionPhase.Authenticated, uid = "1001", epoch = 7)
        push(SessionPhase.New, uid = "1001", epoch = 7)

        // 上一次会话在途的采样迟到抵达。
        push(SessionPhase.Authenticated, uid = "1001", epoch = 6)
        assertFalse(
            ClientRuntime.connectivity.value.authenticated,
            "旧世代的快照不许覆盖当前会话的连接态",
        )
    }

    @Test
    fun in_place_account_switch_is_recognised_as_a_new_session() {
        // 同一个 PrivchatClient 原地切号：client 身份不变，账号变了。
        push(SessionPhase.Authenticated, uid = "1001", epoch = 3)
        ClientRuntime.onAuthExpired()

        push(SessionPhase.Authenticated, uid = "2002", epoch = 4)
        val s = ClientRuntime.connectivity.value
        assertEquals(null, s.lastError, "切到另一个账号是新会话，旧账号的终态与它无关")
        assertTrue(s.authenticated)
    }
}
