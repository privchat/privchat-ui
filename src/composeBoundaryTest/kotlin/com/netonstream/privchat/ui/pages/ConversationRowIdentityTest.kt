package com.netonstream.privchat.ui.pages

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.*
import kotlin.test.*

private class RowApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(index: Int, instance: Unit) = Unit
    override fun insertBottomUp(index: Int, instance: Unit) = Unit
    override fun move(from: Int, to: Int, count: Int) = Unit
    override fun remove(index: Int, count: Int) = Unit
    override fun onClear() = Unit
}

@Composable
private fun RememberedRow(id: String, rendered: MutableMap<String, String>) {
    val owner = remember { id }
    SideEffect { rendered[id] = owner }
}

/** Real Compose state identity regression, not proof of native AX hit-test geometry. */
class ConversationRowIdentityTest {
    private fun ownersAfterReorder(keyed: Boolean): Map<String, String> = runBlocking {
        withTimeout(10_000) {
            val clock = BroadcastFrameClock()
            val recomposer = Recomposer(coroutineContext + clock)
            val job = launch(clock) { recomposer.runRecomposeAndApplyChanges() }
            val composition = Composition(RowApplier(), recomposer)
            var ids by mutableStateOf(listOf("direct", "group"))
            val rendered = mutableMapOf<String, String>()
            suspend fun settle() {
                repeat(12) {
                    Snapshot.sendApplyNotifications()
                    yield(); clock.sendFrame(System.nanoTime()); yield()
                }
                recomposer.awaitIdle()
            }
            try {
                composition.setContent {
                    for (id in ids) {
                        if (keyed) key(id) { RememberedRow(id, rendered) }
                        else RememberedRow(id, rendered)
                    }
                }
                settle()
                assertEquals(mapOf("direct" to "direct", "group" to "group"), rendered)
                ids = listOf("group", "direct")
                settle()
                rendered.toMap()
            } finally {
                composition.dispose(); recomposer.cancel(); job.cancelAndJoin()
            }
        }
    }

    @Test fun positionalRowsRetainTheWrongOwnerAfterReorder() {
        assertEquals(mapOf("group" to "direct", "direct" to "group"), ownersAfterReorder(false))
    }

    @Test fun channelKeysPreserveTheirOwnRememberedState() {
        assertEquals(mapOf("group" to "group", "direct" to "direct"), ownersAfterReorder(true))
    }
}
