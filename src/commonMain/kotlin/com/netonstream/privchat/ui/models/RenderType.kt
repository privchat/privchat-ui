package com.netonstream.privchat.ui.models

import om.netonstream.privchat.sdk.dto.ContentMessageType
import om.netonstream.privchat.sdk.dto.MessageEntry
import om.netonstream.privchat.sdk.dto.contentType

/**
 * 本地 UI 显示类型。协议/服务端不感知，仅用于渲染分派。
 * 对齐 Rust 侧 `MessageRenderType`：
 * - `REVOKED`：`isRevoked` 状态 overlay 的归一化
 * - `SYSTEM`：协议 `ContentMessageType.SYSTEM`
 * - `BUBBLE`：其余走常规气泡（内部按 `contentType()` 再次细分）
 *
 * 注意：撤回是状态，不是内容类型——因此 `REVOKED` 不进 `ContentMessageType`。
 */
enum class RenderType {
    REVOKED,
    SYSTEM,
    BUBBLE;
}

/**
 * 显示类型分派。与 Rust 侧 `bubbles::render_type()` 同构：
 * 1. 撤回状态优先：`isRevoked` → `REVOKED`
 * 2. 然后看协议内容类型：`SYSTEM` → `SYSTEM`
 * 3. 其余归 `BUBBLE`
 *
 * 严格脱离旧 `MessageType` 枚举——仅消费 `isRevoked` + `contentType()`。
 */
fun MessageEntry.renderType(): RenderType = when {
    isRevoked -> RenderType.REVOKED
    contentType() == ContentMessageType.SYSTEM -> RenderType.SYSTEM
    else -> RenderType.BUBBLE
}
