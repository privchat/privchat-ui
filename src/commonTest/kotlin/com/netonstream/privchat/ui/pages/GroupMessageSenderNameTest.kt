package com.netonstream.privchat.ui.pages

import com.netonstream.privchat.sdk.dto.GroupMemberEntry
import com.netonstream.privchat.ui.PrivChat
import com.netonstream.privchat.ui.models.UserProfileSnapshot
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 群消息发送者名字 = IDENTITY_STORE_SPEC §5.2 的展示名链：
 *
 * ```
 * 群名片 remark > 昵称 nickname > username > uid
 * ```
 *
 * 第三、四条以前测不到——旧实现只查传进来的成员列表，链条根本没有那两级。
 */
class GroupMessageSenderNameTest {

    private val channelId: ULong = 100u

    private fun member(userId: ULong, name: String, remark: String = "") = GroupMemberEntry(
        userId = userId,
        channelId = channelId,
        channelType = 2,
        name = name,
        remark = remark,
        avatar = "",
        role = 0,
        status = 1,
        inviteUserId = 0u,
    )

    private fun profile(userId: ULong, nickname: String = "", username: String? = null) =
        UserProfileSnapshot(
            userId = userId,
            nickname = nickname,
            username = username,
            avatar = "",
            userType = 0,
        )

    @BeforeTest
    fun setUp() = reset()

    @AfterTest
    fun tearDown() = reset()

    private fun reset() {
        PrivChat.updateGroupMembers(emptyList())
        PrivChat.resetKnownUsers()
    }

    @Test
    fun groupRemarkTakesPrecedenceOverMemberName() {
        PrivChat.updateGroupMembers(listOf(member(42u, "用户昵称", "群内备注")))
        assertEquals("群内备注", resolveGroupMessageSenderName(42u, channelId))
    }

    @Test
    fun memberNameIsShownWhenRemarkIsBlank() {
        PrivChat.updateGroupMembers(listOf(member(42u, "用户昵称")))
        assertEquals("用户昵称", resolveGroupMessageSenderName(42u, channelId))
    }

    /**
     * 生产上的真实形状：589 人的群，成员列表只加载了一部分，发言人不在其中——
     * 但 `user` 实体同步早就把他的昵称放进本地 user 表了。旧实现在这里显示 uid。
     */
    @Test
    fun senderMissingFromLoadedMemberListStillResolvesFromUserTable() {
        PrivChat.updateGroupMembers(listOf(member(42u, "另一个人")))
        PrivChat.updateKnownUsers(mapOf(100000884uL to profile(100000884u, nickname = "张改婷")))
        assertEquals("张改婷", resolveGroupMessageSenderName(100000884u, channelId))
    }

    /** 昵称未设置时降到 username，而不是直接跳到 uid。 */
    @Test
    fun usernameIsUsedWhenNicknameIsMissing() {
        PrivChat.updateKnownUsers(mapOf(77uL to profile(77u, nickname = "", username = "mk011")))
        assertEquals("mk011", resolveGroupMessageSenderName(77u, channelId))
    }

    /** 群名片优先于 user 表昵称——同一个人在不同群可以有不同称呼。 */
    @Test
    fun groupRemarkStillWinsOverUserTableNickname() {
        PrivChat.updateGroupMembers(listOf(member(55u, "", "群里叫我老王")))
        PrivChat.updateKnownUsers(mapOf(55uL to profile(55u, nickname = "王琼芬")))
        assertEquals("群里叫我老王", resolveGroupMessageSenderName(55u, channelId))
    }

    /** 别的群的成员条目不能串到本群来。 */
    @Test
    fun memberEntryFromAnotherChannelIsIgnored() {
        PrivChat.updateGroupMembers(
            listOf(member(42u, "别的群的名字").copy(channelId = 999u)),
        )
        PrivChat.updateKnownUsers(mapOf(42uL to profile(42u, nickname = "本人昵称")))
        assertEquals("本人昵称", resolveGroupMessageSenderName(42u, channelId))
    }

    /** 全都没有才是 uid——它是兜底，不是正常展示形态。 */
    @Test
    fun unknownUserFallsBackToUid() {
        PrivChat.updateGroupMembers(listOf(member(42u, "用户昵称")))
        assertEquals("99", resolveGroupMessageSenderName(99u, channelId))
    }
}
