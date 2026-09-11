package com.netonstream.privchat.ui.search

import com.netonstream.privchat.sdk.dto.FriendEntry
import com.netonstream.privchat.sdk.dto.GroupMemberEntry
import com.netonstream.privchat.ui.models.displayName

/**
 * "在一堆人里找人"的唯一入口：联系人、创建群、邀请进群、@ 选人都调它。
 *
 * 🔴 每个页面各写一遍 `contains(query)` 是这次要终结的事。
 *
 * 那样每个页面搜的字段都不一样（有的漏了备注，有的把 username 也搜了却没权限显示），
 * 没有一个能搜拼音，也没有一个说得清"为什么这条会出现"。搜索规则属于一个地方。
 */
object PeopleSearch {

    /**
     * 好友的可搜字段，按优先级。
     *
     * username 一并参与：它是**本人加好友时输入的凭证**，搜得到才合理；
     * 而 [FriendEntry] 里有值本身就意味着当前用户有权看到它（PROFILE_VISIBILITY 由
     * 服务端投影决定下不下发），客户端不为了搜索去额外拉取。
     */
    fun fieldsOf(friend: FriendEntry): List<Pair<SearchField, String>> = listOfNotNull(
        SearchField.DisplayName to friend.displayName,
        friend.remark?.takeIf { it.isNotBlank() }?.let { SearchField.Alias to it },
        friend.username.takeIf { it.isNotBlank() }?.let { SearchField.Username to it },
    )

    /** 群成员：群昵称/备注优先于昵称，与 displayName 的解析口径一致。 */
    fun fieldsOf(member: GroupMemberEntry): List<Pair<SearchField, String>> = listOfNotNull(
        SearchField.DisplayName to member.displayName,
        member.remark.takeIf { it.isNotBlank() }?.let { SearchField.Alias to it },
        member.username?.takeIf { it.isNotBlank() }?.let { SearchField.Username to it },
    )

    /**
     * 过滤 + 排序，并把命中信息一起带出来。
     *
     * 空查询返回原顺序、不带命中——调用方此时通常要按字母分组，而不是按相关性排。
     *
     * 同分时用 [tieBreaker]（一般是 uid）收尾：两条完全同分的记录若不定序，
     * 列表每次刷新都可能互换位置。
     */
    fun <T> search(
        items: List<T>,
        query: String,
        fieldsOf: (T) -> List<Pair<SearchField, String>>,
        nameOf: (T) -> String,
        tieBreaker: (T) -> ULong,
    ): List<Pair<T, FieldHit?>> {
        if (query.isBlank()) return items.map { it to null }
        return items
            .mapNotNull { item -> ContactSearch.bestHit(fieldsOf(item), query)?.let { item to it } }
            .sortedWith(
                compareBy(
                    { ContactSearch.rankOf(it.second) },
                    { nameOf(it.first).lowercase() },
                    { tieBreaker(it.first) },
                ),
            )
    }
}
