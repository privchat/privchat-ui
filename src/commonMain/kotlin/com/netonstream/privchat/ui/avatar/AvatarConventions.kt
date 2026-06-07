/**
 * # PrivChat 头像约束（package-level 规范）
 *
 * 本包是 PrivChat 业务头像的**唯一**入口与规则中心。新增页面或调整既有页面前，
 * 请确认以下四条不变式都成立。
 *
 * ## 1. 业务头像渲染必须经过 [PrivChatAvatar]
 *
 * - 业务代码（pages/ / setting/ / qr/ / app/ 等）**不允许**直接 `import com.gearui.primitives.Avatar`
 * - [com.netonstream.privchat.ui.components.ChatAvatar] / [com.netonstream.privchat.ui.components.GroupAvatar]
 *   保留作为历史名 wrapper，内部完全 delegate 到 [PrivChatAvatar]，**不允许**再加 initials / 配色逻辑
 *
 * ## 2. fallback 字母规则只能从 [AvatarText.initialsOf] 取
 *
 * - 业务代码**不允许**自己写 `name.firstOrNull()?.uppercase()` / `name.take(2).uppercase()`
 * - 唯一例外：[com.netonstream.privchat.ui.pages.ContactPage] 的 sticky index header（按字母 A/B/C 分组），
 *   那是分组键不是头像 initials
 *
 * ## 3. fallback 配色只能从 [AvatarPalette] / Theme.colors 取
 *
 * - Compose 端通过 [rememberAvatarResolved] 拿主题敏感色（当前固定走 muted / mutedForeground）
 * - bitmap 端（QR 中心头像）通过 [AvatarPalette] 常量取，**不允许**硬编码 `0xFFF4F4F5` / `0xFF52525B`
 *
 * ## 4. QR / bitmap 等非 Composable 渲染场景必须复用 [AvatarText] + [AvatarPalette]
 *
 * - 当前消费方：`com.netonstream.privchat.app.qr.QrPngRenderer`
 * - 新增任何 bitmap / native 头像产物（启动屏、分享卡片等）都必须从这两个 helper 取
 *
 * ## 远程图加载（Phase 2）
 *
 * 当前阶段**不接**远程图加载。[PrivChatAvatar] 的 `avatarUrl` 参数已透传，但内部仅作占位；
 * 下一阶段只动 [PrivChatAvatar] / [rememberAvatarResolved]，业务调用方零改动。
 */
package com.netonstream.privchat.ui.avatar
