package com.netonstream.privchat.ui.sticker

import com.tencent.kuikly.compose.ui.layout.LayoutCoordinates
import com.tencent.kuikly.compose.ui.layout.findRootCoordinates

/** Kuikly does not implement localToWindow; stay in the clipped root coordinate space. */
internal fun LayoutCoordinates.hasVisibleStickerBounds(): Boolean {
    if (!isAttached) return false
    val root = findRootCoordinates()
    if (!root.isAttached) return false
    val bounds = root.localBoundingBoxOf(this, clipBounds = true)
    return bounds.left.isFinite() && bounds.top.isFinite() && bounds.right.isFinite() && bounds.bottom.isFinite() &&
        maxOf(0f, bounds.left) < minOf(root.size.width.toFloat(), bounds.right) &&
        maxOf(0f, bounds.top) < minOf(root.size.height.toFloat(), bounds.bottom)
}
