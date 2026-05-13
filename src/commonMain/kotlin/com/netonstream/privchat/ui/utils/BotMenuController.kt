package com.netonstream.privchat.ui.utils

import com.netonstream.privchat.sdk.dto.BotMenu
import com.netonstream.privchat.sdk.dto.TransferReply
import com.netonstream.privchat.sdk.dto.decodeBotMenu
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 进程内 bot 菜单缓存与拉取协调器。每个 channel 一份 menu state。
 *
 * 状态机（BOT_INTERACTION_SPEC §3.2）：
 *  - Idle / Loaded(menu) / Empty 由调用方读 [getCached] 判断
 *  - Loading 由 [loadOrCached] 内部并发去重
 *  - Failed 通过 [Result.failure] 抛回，调用方 toast 后允许重试
 *
 * 失败**不**缓存——下次点击重新拉。
 * v1 不做窗口失效；进程退出即失效。
 */
object BotMenuController {
    private const val MENU_ROUTE = "bot/menu/get"
    private const val MENU_TIMEOUT_MS: ULong = 5_000u

    private val cache = mutableMapOf<ULong, BotMenu>()
    private val inflight = mutableMapOf<ULong, Deferred<Result<BotMenu>>>()
    private val mutex = Mutex()

    /** 仅读缓存，不发请求；用于秒开命中分支。 */
    fun getCached(channelId: ULong): BotMenu? = cache[channelId]

    /** 业务侧主动失效（service 重新发布菜单 / 测试场景）。 */
    suspend fun evict(channelId: ULong) {
        mutex.withLock {
            cache.remove(channelId)
            inflight.remove(channelId)
        }
    }

    /**
     * 拉菜单：命中缓存直接返回；并发请求自动去重共享同一个 in-flight Deferred。
     * `transferCall` 由调用方注入（典型 `{ PrivChat.client.transfer(channelId, route, body, timeoutMs) }`），
     * 让本对象不直接耦合 PrivchatClient 单例与 import 顺序。
     */
    suspend fun loadOrCached(
        channelId: ULong,
        scope: CoroutineScope,
        transferCall: suspend (route: String, body: ByteArray, timeoutMs: ULong) -> Result<TransferReply>,
    ): Result<BotMenu> {
        cache[channelId]?.let { return Result.success(it) }
        val deferred = mutex.withLock {
            inflight[channelId]?.let { return@withLock it }
            val job = scope.async {
                val reply = transferCall(MENU_ROUTE, ByteArray(0), MENU_TIMEOUT_MS)
                    .getOrElse { return@async Result.failure<BotMenu>(it) }
                if (reply.code != 0) {
                    return@async Result.failure(
                        IllegalStateException("bot/menu/get failed: code=${reply.code} msg=${reply.message}")
                    )
                }
                val menu = decodeBotMenu(reply.data)
                    ?: return@async Result.failure(
                        IllegalStateException("bot/menu/get returned malformed menu schema")
                    )
                Result.success(menu)
            }
            inflight[channelId] = job
            job
        }
        val result = deferred.await()
        mutex.withLock {
            inflight.remove(channelId)
            result.getOrNull()?.let { cache[channelId] = it }
        }
        return result
    }
}
