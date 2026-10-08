@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.netonstream.privchat.ui.avatar

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.posix.F_OK
import platform.posix.access
import platform.posix.closedir
import platform.posix.opendir
import platform.posix.readdir
import platform.posix.stat

actual object AvatarBitmapRenderer {
    // Rasterising text and images needs the ArkTS drawing APIs, not bridged yet. Returning
    // false keeps the caller on its non-bitmap path (the same as a failed render elsewhere).
    actual suspend fun renderInitials(
        initials: String,
        bgArgb: Int,
        fgArgb: Int,
        sizePx: Int,
        outPath: String,
    ): Boolean = false

    actual suspend fun renderCollage(
        cells: List<CollageCell>,
        sizePx: Int,
        outPath: String,
    ): Boolean = false

    actual fun fileExists(path: String): Boolean = access(path, F_OK) == 0

    actual fun listFileNames(dir: String): List<String> {
        val d = opendir(dir) ?: return emptyList()
        val out = ArrayList<String>()
        try {
            while (true) {
                val entry = readdir(d) ?: break
                val name = entry.pointed.d_name.toKString()
                if (name != "." && name != "..") out += name
            }
        } finally {
            closedir(d)
        }
        return out
    }

    actual fun lastModifiedMillis(path: String): Long = memScoped {
        val st = alloc<stat>()
        if (stat(path, st.ptr) != 0) return 0L
        st.st_mtim.tv_sec * 1000L + st.st_mtim.tv_nsec / 1_000_000L
    }
}
