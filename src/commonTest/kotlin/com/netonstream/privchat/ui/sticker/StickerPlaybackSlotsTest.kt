package com.netonstream.privchat.ui.sticker

import kotlin.test.*

class StickerPlaybackSlotsTest {
    @Test fun staticCandidatesCannotStarveLaterAnimationsAndEveryTurnIsBounded() {
        val slots = StickerPlaybackSlots()
        val owners = List(13) { Any() }
        val seen = mutableSetOf<Any>()
        owners.forEach(slots::enter)
        repeat(4) {
            val active = owners.filter(slots::canAnimate)
            assertEquals(4, active.size)
            seen.addAll(active)
            slots.rotate()
        }
        assertEquals(owners.toSet(), seen)
        owners.forEach(slots::leave)
        assertTrue(slots.isEmpty)
        slots.rotate()
    }

    @Test fun aggregateCapPromotesStaticFallbackWhenVisibleOwnerLeaves() {
        val slots = StickerPlaybackSlots()
        val owners = List(20) { Any() }
        owners.forEach(slots::enter)
        assertEquals(4, owners.count(slots::canAnimate))
        assertFalse(slots.canAnimate(owners[4]))
        slots.enter(owners[0])
        slots.leave(owners[0])
        assertTrue(slots.canAnimate(owners[4]))
        assertEquals(4, owners.count(slots::canAnimate))
        owners.forEach(slots::leave)
        assertEquals(0, owners.count(slots::canAnimate))
        slots.enter(owners.last())
        assertTrue(slots.canAnimate(owners.last()))
    }
}
