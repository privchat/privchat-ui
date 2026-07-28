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
    fun background_disconnect_then_foreground_shows_reconnecting() {
        // 后台超时主动断开后回到前台：我们**连过**，所以这是重连，不是首连。
        // app 主动 disconnect 时若不喂给运行时层，connectivity 会停在 authenticated=true，
        // 状态条落到「服务器连接中」——语义错了。
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
}
