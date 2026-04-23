package com.netonstream.privchat.ui.utils

/**
 * 消息内联实体识别（UX-3 / UX-4）。
 *
 * 客户端渲染时进行正则识别：链接 / 电话 / 邮箱。结果仅用于 UI 高亮与点击动作，
 * 不持久化到数据库。若服务端已下发 `inline_refs`，调用方应跳过本检测。
 */
object MessageEntityDetector {

    enum class Type { URL, PHONE, EMAIL }

    data class Entity(val type: Type, val start: Int, val end: Int, val text: String)

    // https?://xxx，允许常见 URL 字符；遇到中文/空白/闭合标点停止。
    private val urlRegex = Regex("""https?://[A-Za-z0-9\-._~:/?#\[\]@!${'$'}&'()*+,;=%]+""")

    private val emailRegex = Regex("""[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}""")

    // 电话：简易国际/本地格式——以可选 + 开头，然后 7–14 位数字（允许中间空格/连字符）。
    // 这里不引入 libphonenumber，避免客户端体积膨胀；作为基础版本，过度识别由 ActionSheet 的
    // 「拨号 / 复制」动作兜底（用户主动触发，不会自动拨出）。
    private val phoneRegex = Regex("""(?<![\w@])\+?\d[\d\s\-]{6,13}\d(?!\w)""")

    /**
     * 返回按起始位置排序的实体数组；冲突区间优先级 URL > EMAIL > PHONE。
     */
    fun detect(text: String): List<Entity> {
        if (text.isEmpty()) return emptyList()
        val entities = mutableListOf<Entity>()
        urlRegex.findAll(text).forEach {
            entities += Entity(Type.URL, it.range.first, it.range.last + 1, it.value)
        }
        emailRegex.findAll(text).forEach {
            val start = it.range.first
            val end = it.range.last + 1
            if (entities.none { e -> overlaps(e.start, e.end, start, end) }) {
                entities += Entity(Type.EMAIL, start, end, it.value)
            }
        }
        phoneRegex.findAll(text).forEach {
            val raw = it.value.trim()
            // 至少包含 7 位数字才当成电话，避免把流水号误判。
            val digits = raw.count { c -> c.isDigit() }
            if (digits < 7) return@forEach
            val start = it.range.first
            val end = it.range.last + 1
            if (entities.none { e -> overlaps(e.start, e.end, start, end) }) {
                entities += Entity(Type.PHONE, start, end, raw)
            }
        }
        entities.sortBy { it.start }
        return entities
    }

    private fun overlaps(a0: Int, a1: Int, b0: Int, b1: Int): Boolean =
        a0 < b1 && b0 < a1
}
