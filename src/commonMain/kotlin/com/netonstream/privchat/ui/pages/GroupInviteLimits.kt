package com.netonstream.privchat.ui.pages

/**
 * 单次邀请的人数上限（CHANNEL_SPEC §7.1.1）。
 *
 * 这是**批次**上限，不是群容量上限——容量由服务端 `max_members` 决定（默认 500），
 * 500 人的群可以分 25 次邀请建起来。
 *
 * 必须与 protocol 的 `GROUP_INVITE_BATCH_LIMIT` 同值：建群路径超限服务端返
 * `20314 GroupInviteBatchTooLarge`。
 *
 * 放在这里是因为建群页和邀请页原来各写死了一份 49。同一条规则散成两个字面量，
 * 改的时候必然漏一个——而漏掉的那个不会报错，只会让某一条路径悄悄比另一条更宽松。
 */
internal const val GROUP_INVITE_BATCH_LIMIT = 20
