package com.netonstream.privchat.ui.i18n

/**
 * 按顺序把文案里的 `%d` / `%s` 占位符替换成实参。
 *
 * 语言包里已经在用 `%d`（如 `presenceOfflineMinutesAgo`），这里只是把那份
 * 手写 `replace("%d", …)` 收成一个能吃多参数的入口——两个占位符的文案用
 * `replace` 会把两个位置替换成同一个值。
 *
 * 占位符按**出现顺序**吃参数，所以译文调换语序时必须连带调换实参含义；需要
 * 换位的文案请拆成两条 key，不要在这里发明 `%1$d` 语法。
 */
fun String.withArgs(vararg args: Any?): String {
    if (args.isEmpty()) return this
    val out = StringBuilder(length)
    var i = 0
    var next = 0
    while (i < length) {
        val c = this[i]
        if (c == '%' && i + 1 < length && (this[i + 1] == 'd' || this[i + 1] == 's') && next < args.size) {
            out.append(args[next].toString())
            next++
            i += 2
        } else {
            out.append(c)
            i++
        }
    }
    return out.toString()
}
