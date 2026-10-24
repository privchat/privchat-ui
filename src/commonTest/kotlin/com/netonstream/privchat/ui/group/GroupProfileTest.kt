package com.netonstream.privchat.ui.group

import com.netonstream.privchat.sdk.dto.GroupPinnedMessageView
import com.netonstream.privchat.sdk.dto.MessageEntry
import com.netonstream.privchat.sdk.dto.MessageStatus
import com.netonstream.privchat.ui.components.MessageActionKind
import com.netonstream.privchat.ui.components.MessageActionPolicy
import com.netonstream.privchat.ui.i18n.PrivChatI18n
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class GroupProfileTest {
    @Test fun memberOrderIsOwnerThenAdminThenMemberNotNumericDescending() {
        assertEquals(listOf(1, 2, 0), listOf(0, 2, 1).sortedBy(GroupProfilePolicy::memberRoleOrder))
        assertEquals(GroupProfilePolicy.memberRoleOrder(0), GroupProfilePolicy.memberRoleOrder(99))
    }

    @Test fun profilePermissionMatrix() {
        assertTrue(GroupProfilePolicy.canEdit(true, true, false))
        assertTrue(GroupProfilePolicy.canEdit(true, false, true))
        assertFalse(GroupProfilePolicy.canEdit(true, false, false))
        assertFalse(GroupProfilePolicy.canEdit(false, true, true))
    }

    @Test fun namesUseScalarNotUtf16Length() {
        assertTrue(GroupProfilePolicy.validName("😀".repeat(30)))
        assertEquals(30, GroupProfilePolicy.scalarCount("😀".repeat(30)))
        assertFalse(GroupProfilePolicy.validName("😀".repeat(31)))
    }

    @Test fun emptyAndControlsCannotSave() {
        for (name in listOf("", "   ", "a\nb", "a\rb", "a\tb", "a\u0000b", "a\u0085b")) {
            assertFalse(GroupProfilePolicy.validName(name), name)
        }
        assertTrue(GroupProfilePolicy.validName("  Group  "))
    }

    @Test fun adminCannotUnpinOwnerOrUnknownPinner() {
        assertFalse(GroupPinPolicy.canUnpin(2u, 2, 1u, 1))
        assertFalse(GroupPinPolicy.canUnpin(2u, 2, 1u, null))
        assertTrue(GroupPinPolicy.canUnpin(2u, 2, 3u, 2))
        assertTrue(GroupPinPolicy.canUnpin(2u, 2, 3u, 0))
        assertTrue(GroupPinPolicy.canUnpin(2u, 2, 2u, null))
        assertTrue(GroupPinPolicy.canUnpin(1u, 1, 3u, null))
        assertFalse(GroupPinPolicy.canUnpin(3u, 0, 3u, 2))
        assertFalse(GroupPinPolicy.canUnpin(null, 1, 3u, 2))
    }

    @Test fun pinRolesFollowFrozenProtocolWireValues() {
        assertTrue(GroupPinPolicy.canPin(1))
        assertTrue(GroupPinPolicy.canPin(2))
        for (role in listOf(null, 0, -1, 3)) {
            assertFalse(GroupPinPolicy.canPin(role))
            assertFalse(GroupPinPolicy.canUnpin(3u, role, 3u, 0))
        }
    }

    @Test fun ownerPinUnpinGateDoesNotRemoveAdminRecallOrNewPin() {
        val message = MessageEntry(1u, 99u, null, 4u, 2, 1u, "text", status = MessageStatus.Sent, timestamp = 1u)
        val pinned = MessageActionPolicy.Context(message, false, 0, canPin = true, isPinned = true, canUnpin = false)
        val actions = MessageActionPolicy.menuActions(pinned)
        assertFalse(MessageActionKind.Unpin in actions)
        assertTrue(MessageActionKind.Recall in actions)
        assertTrue(MessageActionKind.Pin in MessageActionPolicy.menuActions(pinned.copy(isPinned = false)))
    }

    @Test fun jumpFindsServerIdentityNotCoincidentLocalIdOrWrongChannel() {
        val wrongLocal = MessageEntry(99u, 100u, null, 4u, 2, 1u, "wrong", status = MessageStatus.Sent, timestamp = 1u)
        val right = wrongLocal.copy(id = 7u, serverMessageId = 99u)
        assertEquals(right, GroupPinPolicy.sourceAnchor(4u, 99u, listOf(wrongLocal, right)))
        assertNull(GroupPinPolicy.sourceAnchor(5u, 99u, listOf(right)))
        assertNull(GroupPinPolicy.sourceAnchor(4u, 99u, null))
    }

    @Test fun permissionAndValidationErrorsAreLocalizedNotRaw() {
        assertEquals(PrivChatI18n.current.platformErrPermissionDenied,
            GroupUiError.message(Exception("reason_code=10004 message=private server detail"), "fallback"))
        assertEquals(PrivChatI18n.current.platformErrProfileFieldInvalid,
            GroupUiError.message(Exception("reason_code=10100 message=raw validation"), "fallback"))
        assertEquals("fallback", GroupUiError.message(Exception("private server detail"), "fallback"))
    }

    @Test fun networkFailureUsesExistingMapper() {
        assertEquals(PrivChatI18n.current.networkError,
            GroupUiError.message(Exception("connection refused 127.0.0.1"), "fallback"))
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GroupDescriptionEditorTest {
    private suspend fun loaded() = GroupDescriptionEditor().also { it.refresh { Result.success("original") } }

    @Test fun membersCannotOpenOrWrite() = runTest {
        val editor = loaded()
        editor.begin(false)
        var writes = 0
        assertNull(editor.save(false) { writes++; Result.success(true) })
        assertFalse(editor.editing)
        assertEquals(0, writes)
        assertEquals("original", editor.description)
    }

    @Test fun saveFailurePreservesDraftForRetry() = runTest {
        val editor = loaded()
        editor.begin(true)
        editor.change("draft")
        editor.save(true) { Result.failure(Exception("connection refused")) }
        assertTrue(editor.editing)
        assertFalse(editor.saving)
        assertEquals("draft", editor.draft)
        assertEquals("original", editor.description)
        assertNotNull(editor.error)
        editor.save(true) { assertEquals("draft", it); Result.success(true) }
        assertFalse(editor.editing)
        assertEquals("draft", editor.description)
    }

    @Test fun falseSuccessDoesNotCloseOrMutateDescription() = runTest {
        val editor = loaded()
        editor.begin(true)
        editor.change("draft")
        assertTrue(editor.save(true) { Result.success(false) }!!.isFailure)
        assertTrue(editor.editing)
        assertEquals("original", editor.description)
    }

    @Test fun emptyDescriptionIsAnExplicitClearAndCanReread() = runTest {
        val editor = loaded()
        editor.begin(true)
        editor.change("")
        editor.save(true) { assertEquals("", it); Result.success(true) }
        editor.refresh { Result.success("") }
        assertEquals("", editor.description)
    }

    @Test fun saveIsSingleFlightAndLocksDraftAndDismiss() = runTest {
        val editor = loaded()
        editor.begin(true)
        editor.change("draft")
        val response = CompletableDeferred<Result<Boolean>>()
        var writes = 0
        val save = async { editor.save(true) { writes++; response.await() } }
        runCurrent()
        editor.change("lost")
        editor.dismiss()
        assertNull(editor.save(true) { writes++; Result.success(true) })
        assertEquals("draft", editor.draft)
        assertTrue(editor.editing)
        assertEquals(1, writes)
        response.complete(Result.success(true))
        save.await()
    }

    @Test fun projectionRefreshDoesNotOverwriteDraft() = runTest {
        val editor = loaded()
        editor.begin(true)
        editor.change("local draft")
        editor.refresh { Result.success("remote change") }
        assertEquals("remote change", editor.description)
        assertEquals("local draft", editor.draft)
    }

    @Test fun readBeforeSaveCannotOverwriteSuccessfulWrite() = runTest {
        val editor = loaded()
        val stale = CompletableDeferred<Result<String>>()
        val read = launch { editor.refresh { stale.await() } }
        runCurrent()
        editor.begin(true)
        editor.change("saved")
        editor.save(true) { Result.success(true) }
        stale.complete(Result.success("stale"))
        read.join()
        assertEquals("saved", editor.description)
    }

    @Test fun latestRefreshWinsAndRevokedEditDoesNotWrite() = runTest {
        val editor = loaded()
        val stale = CompletableDeferred<Result<String>>()
        val read = launch { editor.refresh { stale.await() } }
        runCurrent()
        editor.refresh { Result.success("new") }
        stale.complete(Result.success("old"))
        read.join()
        assertEquals("new", editor.description)
        editor.begin(true)
        assertNull(editor.save(false) { error("Permission revoked: must not call write") })
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GroupPinsStateTest {
    private fun pin(id: ULong, channel: ULong = 4u) = GroupPinnedMessageView(id, channel, 1u, id)

    @Test fun allMembersReadMultiplePinsWithoutAnInventedLimit() = runTest {
        val state = GroupPinsState(4u)
        state.refresh { Result.success((1..60).map { pin(it.toULong()) }) }
        assertEquals(60, state.messages.size)
    }

    @Test fun hideIsLocalAndSurvivesAuthoritativeRefresh() = runTest {
        val state = GroupPinsState(4u)
        state.refresh { Result.success(listOf(pin(1u))) }
        state.hide()
        state.refresh { Result.success(listOf(pin(2u), pin(1u))) }
        assertTrue(state.hidden)
        assertEquals(listOf(2uL, 1uL), state.messages.map { it.messageId })
    }

    @Test fun staleRefreshCannotResurrectRemovedPin() = runTest {
        val state = GroupPinsState(4u)
        val old = CompletableDeferred<Result<List<GroupPinnedMessageView>>>()
        val read = launch { state.refresh { old.await() } }
        runCurrent()
        state.refresh { Result.success(emptyList()) }
        old.complete(Result.success(listOf(pin(1u))))
        read.join()
        assertTrue(state.messages.isEmpty())
    }

    @Test fun openedSelectionKeepsOriginalServerIdWhenListChanges() = runTest {
        val state = GroupPinsState(4u)
        state.refresh { Result.success(listOf(pin(1u), pin(2u))) }
        val opened = state.selectionSnapshot()
        state.refresh { Result.success(listOf(pin(3u), pin(1u), pin(2u))) }
        assertEquals(2uL, opened[1].messageId)
        assertEquals(1uL, state.messages[1].messageId)
    }

    @Test fun failureRetainsLastReadAndRejectsWrongChannelOrZeroServerId() = runTest {
        val state = GroupPinsState(4u)
        state.refresh { Result.success(listOf(pin(1u), pin(0u), pin(2u, 5u))) }
        state.refresh { Result.failure(Exception("offline")) }
        assertEquals(listOf(1uL), state.messages.map { it.messageId })
    }

    @Test fun closedPageCannotApplyCancelledRefresh() = runTest {
        val state = GroupPinsState(4u)
        val read = launch {
            state.refresh {
                kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]!!.cancel()
                Result.success(listOf(pin(1u)))
            }
        }
        read.join()
        assertTrue(state.messages.isEmpty())
    }
}
