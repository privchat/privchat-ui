package com.netonstream.privchat.ui.effect

import com.netonstream.privchat.ui.sticker.hasVisibleStickerBounds
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.geometry.Rect
import com.tencent.kuikly.compose.ui.layout.AlignmentLine
import com.tencent.kuikly.compose.ui.layout.LayoutCoordinates
import com.tencent.kuikly.compose.ui.unit.IntSize
import kotlin.test.*

class StickerVisibilityFocusedTest {
    private class Coordinates(val parent: Coordinates? = null) : LayoutCoordinates {
        override val size = IntSize(100, 100)
        override val providedAlignmentLines = emptySet<AlignmentLine>()
        override val parentLayoutCoordinates get() = parent
        override val parentCoordinates get() = parent
        override var isAttached = true
        var clipped = Rect(10f, 10f, 40f, 40f)
        var reads = 0
        override fun get(alignmentLine: AlignmentLine): Int = 0
        override fun localToWindow(relativeToLocal: Offset): Offset = error("Unsupported Kuikly window conversion")
        override fun windowToLocal(relativeToWindow: Offset): Offset = error("Unsupported Kuikly window conversion")
        override fun localToRoot(relativeToLocal: Offset): Offset = relativeToLocal
        override fun localPositionOf(sourceCoordinates: LayoutCoordinates, relativeToSource: Offset): Offset = relativeToSource
        override fun localBoundingBoxOf(sourceCoordinates: LayoutCoordinates, clipBounds: Boolean): Rect {
            assertTrue(clipBounds)
            reads++
            return clipped
        }
    }

    @Test fun clippedRootGeometryNeverUsesUnimplementedWindowConversion() {
        val root = Coordinates()
        val child = Coordinates(root)
        assertTrue(child.hasVisibleStickerBounds())
        root.clipped = Rect(-10f, 10f, 1f, 40f)
        assertTrue(child.hasVisibleStickerBounds())
        for (rect in listOf(Rect(101f, 0f, 120f, 20f), Rect(-20f, 0f, -1f, 20f), Rect.Zero, Rect(0f, 101f, 20f, 120f), Rect(Float.NaN, 0f, 20f, 20f))) {
            root.clipped = rect
            assertFalse(child.hasVisibleStickerBounds())
        }
        assertEquals(7, root.reads)
    }

    @Test fun detachedCoordinatesNeverReadGeometry() {
        val root = Coordinates()
        val child = Coordinates(root)
        child.isAttached = false
        assertFalse(child.hasVisibleStickerBounds())
        child.isAttached = true
        root.isAttached = false
        assertFalse(child.hasVisibleStickerBounds())
        assertEquals(0, root.reads)
    }
}
