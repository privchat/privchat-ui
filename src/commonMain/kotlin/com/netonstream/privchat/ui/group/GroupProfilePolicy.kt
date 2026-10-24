package com.netonstream.privchat.ui.group

object GroupProfilePolicy {
    fun memberRoleOrder(role: Int): Int = when (role) {
        1 -> 0
        2 -> 1
        else -> 2
    }

    fun canEdit(isGroup: Boolean, isOwner: Boolean, isAdmin: Boolean): Boolean =
        isGroup && (isOwner || isAdmin)

    fun scalarCount(value: String): Int {
        var count = 0
        var index = 0
        while (index < value.length) {
            val c = value[index++]
            if (c.isHighSurrogate() && index < value.length && value[index].isLowSurrogate()) index++
            count++
        }
        return count
    }

    fun validName(value: String): Boolean {
        val name = value.trim()
        return name.isNotEmpty() && scalarCount(name) <= 30 &&
            name.none { it.code in 0..31 || it.code in 127..159 }
    }
}

object GroupProfileTestTags {
    const val NameRow = "group-profile-name"
    const val NameInput = "group-profile-name-input"
    const val NameSave = "group-profile-name-save"
    const val DescriptionRow = "group-profile-description"
    const val DescriptionInput = "group-profile-description-input"
    const val DescriptionError = "group-profile-description-error"
}
