package com.netonstream.privchat.ui.pages

import com.netonstream.privchat.sdk.dto.GroupMemberEntry
import com.netonstream.privchat.ui.models.UserProfileSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 群消息发送者名字 = IDENTITY_STORE_SPEC §5.2 的展示名链：
 *
 * ```
 * 群名片 remark > 昵称 nickname > username > uid
 * ```
 *
 * 后两级以前测不到——旧实现只查传进来的成员列表，链条里根本没有那两级。
 *
 * 两个快照都作为参数传入（而不是写全局再断言）：这是被测函数的真实签名，
 * 也是「资料异步后到时 Compose 能追踪」的前提。
 */
class GroupMessageSenderNameTest {

    private val channelId: ULong = 100u

    private fun member(
        userId: ULong,
        name: String,
        remark: String = "",
        inChannel: ULong = channelId,
    ) = GroupMemberEntry(
        userId = userId,
        channelId = inChannel,
        channelType = 2,
        name = name,
        remark = remark,
        avatar = "",
        role = 0,
        status = 1,
        inviteUserId = 0u,
    )

    private fun profile(
        userId: ULong,
        nickname: String = "",
        username: String? = null,
        userType: Int = 0,
    ) = UserProfileSnapshot(
        userId = userId,
        nickname = nickname,
        username = username,
        avatar = "",
        userType = userType,
    )

    @Test
    fun groupRemarkTakesPrecedenceOverMemberName() {
        assertEquals(
            "群内备注",
            resolveGroupMessageSenderName(
                42u, channelId, listOf(member(42u, "用户昵称", "群内备注")), emptyMap(),
            ),
        )
    }

    @Test
    fun memberNameIsShownWhenRemarkIsBlank() {
        assertEquals(
            "用户昵称",
            resolveGroupMessageSenderName(42u, channelId, listOf(member(42u, "用户昵称")), emptyMap()),
        )
    }

    /**
     * 生产上的真实形状：589 人的群，成员列表只加载了一部分，发言人不在其中——
     * 但 `user` 实体同步早就把他的昵称放进本地 user 表了。旧实现在这里显示 uid。
     */
    @Test
    fun senderMissingFromLoadedMemberListStillResolvesFromUserTable() {
        assertEquals(
            "张改婷",
            resolveGroupMessageSenderName(
                100000884u,
                channelId,
                listOf(member(42u, "另一个人")),
                mapOf(100000884uL to profile(100000884u, nickname = "张改婷")),
            ),
        )
    }

    /** 昵称未设置时降到 username，而不是直接跳到 uid。 */
    @Test
    fun usernameIsUsedWhenNicknameIsMissing() {
        assertEquals(
            "mk011",
            resolveGroupMessageSenderName(
                77u, channelId, emptyList(), mapOf(77uL to profile(77u, username = "mk011")),
            ),
        )
    }

    /** 群名片优先于 user 表昵称——同一个人在不同群可以有不同称呼。 */
    @Test
    fun groupRemarkStillWinsOverUserTableNickname() {
        assertEquals(
            "群里叫我老王",
            resolveGroupMessageSenderName(
                55u,
                channelId,
                listOf(member(55u, "", "群里叫我老王")),
                mapOf(55uL to profile(55u, nickname = "王琼芬")),
            ),
        )
    }

    /** 别的群的成员条目不能串到本群来。 */
    @Test
    fun memberEntryFromAnotherChannelIsIgnored() {
        assertEquals(
            "本人昵称",
            resolveGroupMessageSenderName(
                42u,
                channelId,
                listOf(member(42u, "别的群的名字", inChannel = 999u)),
                mapOf(42uL to profile(42u, nickname = "本人昵称")),
            ),
        )
    }

    /** 系统账号走本地化文案，绝不显示 uid 或原始昵称（UserDisplay 既有规则，不许绕过）。 */
    @Test
    fun systemUserIsLocalisedRatherThanShownAsUid() {
        val name = resolveGroupMessageSenderName(
            1u, channelId, emptyList(), mapOf(1uL to profile(1u, nickname = "sys", userType = 1)),
        )
        assertTrue(name != "1" && name != "sys", "system user rendered as '$name'")
    }

    /** 全都没有才是 uid——它是兜底，不是正常展示形态。 */
    @Test
    fun unknownUserFallsBackToUid() {
        assertEquals(
            "99",
            resolveGroupMessageSenderName(99u, channelId, listOf(member(42u, "用户昵称")), emptyMap()),
        )
    }
}
