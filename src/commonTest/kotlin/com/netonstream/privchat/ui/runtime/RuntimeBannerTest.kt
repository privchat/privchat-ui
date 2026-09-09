package com.netonstream.privchat.ui.runtime

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 「网络已断开」横幅永不自愈的架构回归(用户实测,第 3 次复发)。
 *
 * 根因:横幅曾把 [ConnectivityState.networkReachable](宿主 reachability 镜像,来源
 * 系统 reachability + SDK network_hint,任一漏发恢复回调即卡死)压过真实的
 * [ConnectivityState.authenticated],于是「一边收着消息一边提示断网」。
 * 规则冻结:已认证的活连接是比 reachability 镜像更强的真值,authenticated=true 时
 * 绝不显示 OFFLINE。
 */
class RuntimeBannerTest {

    private fun banner(
        connectivity: ConnectivityState,
        sync: SyncState = SyncState(),
        started: Boolean = true,
        showConnected: Boolean = false,
    ): RuntimeBannerKind =
        resolveRuntimeBanner(connectivity, sync, hasStartedConnectionFlow = started, showConnectedBanner = showConnected)

    @Test
    fun authenticated_overrides_stale_unreachable_mirror() {
        // reachability 卡在 false,但会话已认证 → 绝不能显示「网络已断开」。
        val kind = banner(
            ConnectivityState(networkReachable = false, authenticated = true),
        )
        assertEquals(RuntimeBannerKind.HIDDEN, kind)
    }

    @Test
    fun authenticated_while_unreachable_and_syncing_shows_syncing_not_offline() {
        val kind = banner(
            ConnectivityState(networkReachable = false, authenticated = true),
            sync = SyncState(resumeSyncRunning = true),
        )
        assertEquals(RuntimeBannerKind.SYNCING, kind)
    }

    @Test
    fun unreachable_without_auth_still_shows_offline() {
        // 未认证 + 设备确实不可达 → 保留「网络已断开」(不能误伤真离线提示)。
        val kind = banner(
            ConnectivityState(networkReachable = false, authenticated = false),
        )
        assertEquals(RuntimeBannerKind.OFFLINE, kind)
    }

    @Test
    fun authenticated_event_heals_reachable_mirror() {
        // 认证事件是 proof-of-network:即便进入时镜像为 unreachable,也会被校正回 true。
        // ClientRuntime 是单例,先 reset 再打断可达性,避免与其他用例串状态。
        ClientRuntime.reset()
        ClientRuntime.onNetworkReachableChanged(false)
        ClientRuntime.onConnectionStateChanged("authenticated")
        assertEquals(true, ClientRuntime.connectivity.value.networkReachable)
        assertEquals(true, ClientRuntime.connectivity.value.authenticated)
        ClientRuntime.reset()
    }

    @Test
    fun background_disconnect_then_foreground_shows_connecting() {
        // 后台超时主动断开后回到前台：技术上是重连（我们连过），但**用户没看见那次断开**——
        // 它发生在 App 不可见的时候。他只是把 App 切回来，显示「重连中」凭空制造故障感。
        // 所以带 onBackgroundDisconnect 标记的这次掉线按「连接中」呈现，与首连一致。
        //
        // 这条测试原来断言的是 RECONNECTING，那是之前"语义更准"的取向；按用户反馈改判：
        // 状态条是说给用户听的，不是描述传输层事实的。
        ClientRuntime.reset()
        ClientRuntime.onConnectionStateChanged("authenticated")
        ClientRuntime.onBackgroundDisconnect()
        ClientRuntime.onConnectionStateChanged("disconnected")

        val kind = resolveRuntimeBanner(
            connectivity = ClientRuntime.connectivity.value,
            sync = SyncState(),
            hasStartedConnectionFlow = true,
            showConnectedBanner = false,
        )
        assertEquals(RuntimeBannerKind.CONNECTING, kind)
        ClientRuntime.reset()
    }

    @Test
    fun visible_mid_session_drop_still_shows_reconnecting() {
        // 对照组：会话中途真掉线（隧道、切网），用户是看得见的，仍然是「重连中」。
        // 少了这条，上面那条一改就等于把 RECONNECTING 整个废掉了。
        ClientRuntime.reset()
        ClientRuntime.onConnectionStateChanged("authenticated")
        ClientRuntime.onConnectionStateChanged("disconnected")

        val kind = resolveRuntimeBanner(
            connectivity = ClientRuntime.connectivity.value,
            sync = SyncState(),
            hasStartedConnectionFlow = true,
            showConnectedBanner = false,
        )
        assertEquals(RuntimeBannerKind.RECONNECTING, kind)
        ClientRuntime.reset()
    }

    @Test
    fun reauthentication_clears_the_background_flag() {
        // 标记必须在认证成功时清掉：否则这一整个会话里之后任何一次真掉线都会被误显示成
        // 「连接中」。
        ClientRuntime.reset()
        ClientRuntime.onConnectionStateChanged("authenticated")
        ClientRuntime.onBackgroundDisconnect()
        ClientRuntime.onConnectionStateChanged("disconnected")
        ClientRuntime.onConnectionStateChanged("authenticated")
        ClientRuntime.onConnectionStateChanged("disconnected")

        val kind = resolveRuntimeBanner(
            connectivity = ClientRuntime.connectivity.value,
            sync = SyncState(),
            hasStartedConnectionFlow = true,
            showConnectedBanner = false,
        )
        assertEquals(RuntimeBannerKind.RECONNECTING, kind)
        ClientRuntime.reset()
    }
}
