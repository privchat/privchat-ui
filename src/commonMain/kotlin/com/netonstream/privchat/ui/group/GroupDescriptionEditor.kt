package com.netonstream.privchat.ui.group

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** One page's draft; refreshes never replace an in-progress edit. */
class GroupDescriptionEditor {
    var description: String? by mutableStateOf(null)
        private set
    var draft by mutableStateOf("")
        private set
    var editing by mutableStateOf(false)
        private set
    var saving by mutableStateOf(false)
        private set
    var error: Throwable? by mutableStateOf(null)
        private set
    private var readEpoch = 0L

    suspend fun refresh(read: suspend () -> Result<String>) {
        val epoch = ++readEpoch
        val result = read()
        currentCoroutineContext().ensureActive()
        if (epoch == readEpoch) result.onSuccess { description = it }
    }

    fun begin(canEdit: Boolean) {
        if (!canEdit || saving || description == null) return
        draft = description.orEmpty()
        error = null
        editing = true
    }

    fun change(value: String) { if (!saving) draft = value }
    fun dismiss() { if (!saving) editing = false }

    suspend fun save(canEdit: Boolean, write: suspend (String) -> Result<Boolean>): Result<Boolean>? {
        if (!canEdit || !editing || saving) return null
        saving = true
        error = null
        ++readEpoch // Ignore reads started before this write.
        val submitted = draft
        return try {
            val result = write(submitted).mapCatching {
                check(it) { "Group profile update was not applied" }
                it
            }
            currentCoroutineContext().ensureActive()
            ++readEpoch // Also ignore reads that raced the write.
            result.onSuccess {
                description = submitted
                editing = false
            }.onFailure { error = it }
            result
        } finally {
            saving = false
        }
    }
}
