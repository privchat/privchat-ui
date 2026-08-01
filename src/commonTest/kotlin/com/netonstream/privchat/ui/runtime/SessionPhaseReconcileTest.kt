package com.netonstream.privchat.ui.runtime

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 「服务器重连中」横幅永不熄灭的回归。
 *
 * 根因是两条：
 *  1. `reconnecting` 只由 `connection_state_changed → authenticated` 这个**边沿**清除。
 *     连接若在事件流挂上之前就恢复、或恢复期间没产生状态跃迁，边沿就再也不会来，
 *     横幅永远亮着而 SDK 其实在正常收发。修法是让**状态**兜底（[ClientRuntime.reconcileSessionPhase]）。
 *  2. 事件消费把 `loggedin` 当成已认证。Rust 侧写死了 `Connected/LoggedIn` 都可能跑在
 *     服务端尚未授权的通道上，据此撤横幅等于谎报就绪。
 */
class SessionPhaseReconcileTest {

    @BeforeTest fun setUp() = ClientRuntime.reset()
    @AfterTest fun tearDown() = ClientRuntime.reset()

    private fun gen() = ClientRuntime.currentSessionGeneration()

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
        // 建立过会话 → 掉线 → 横幅亮起
        ClientRuntime.onConnectionStateChanged("authenticated")
        ClientRuntime.onConnectionStateChanged("disconnected")
        assertTrue(ClientRuntime.connectivity.value.reconnecting, "前提：横幅确实亮着")

        // 连接其实已经恢复，但 authenticated 事件永远不来（边沿丢失）。
        // 只靠事件的旧实现会一直卡在 reconnecting=true。
        ClientRuntime.reconcileSessionPhase("Authenticated", gen())

        val s = ClientRuntime.connectivity.value
        assertFalse(s.reconnecting, "状态对账必须能在边沿丢失时熄灭横幅")
        assertTrue(s.authenticated)
        assertEquals(null, s.lastError)
    }

    @Test
    fun reconcile_with_connected_never_clears_reconnecting() {
        ClientRuntime.onConnectionStateChanged("authenticated")
        ClientRuntime.onConnectionStateChanged("disconnected")

        ClientRuntime.reconcileSessionPhase("Connected", gen())
        assertTrue(ClientRuntime.connectivity.value.reconnecting, "Connected 未鉴权，不得撤横幅")

        ClientRuntime.reconcileSessionPhase("LoggedIn", gen())
        assertTrue(ClientRuntime.connectivity.value.reconnecting, "LoggedIn 同样未鉴权")

        // 只有真正鉴权完成才熄灭
        ClientRuntime.reconcileSessionPhase("Authenticated", gen())
        assertFalse(ClientRuntime.connectivity.value.reconnecting)
    }

    @Test
    fun reconcile_pulls_authenticated_down_when_phase_regresses() {
        ClientRuntime.onConnectionStateChanged("authenticated")
        assertTrue(ClientRuntime.connectivity.value.authenticated)

        ClientRuntime.reconcileSessionPhase("New", gen())
        assertFalse(ClientRuntime.connectivity.value.authenticated, "Core 已回到 New，不能继续宣称已认证")
    }

    // ---- 防串号 ----

    @Test
    fun stale_generation_snapshot_is_discarded() {
        ClientRuntime.onConnectionStateChanged("authenticated")
        val staleGeneration = gen()

        ClientRuntime.reset() // 登出/切号：世代自增
        assertFalse(ClientRuntime.connectivity.value.authenticated)

        // 旧账号在途的采样迟到抵达，绝不能把新账号标成已认证。
        ClientRuntime.reconcileSessionPhase("Authenticated", staleGeneration)
        assertFalse(
            ClientRuntime.connectivity.value.authenticated,
            "上一个账号的快照不许覆盖当前账号的连接态",
        )

        // 当前世代的同一份快照则正常生效。
        ClientRuntime.reconcileSessionPhase("Authenticated", gen())
        assertTrue(ClientRuntime.connectivity.value.authenticated)
    }
}
