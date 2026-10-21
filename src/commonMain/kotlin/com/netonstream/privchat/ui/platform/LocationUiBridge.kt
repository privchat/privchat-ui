package com.netonstream.privchat.ui.platform

/** Host-owned map presentation. No provider SDK types enter the chat UI. */
data class LocationPresentation(
    val latitude: Double,
    val longitude: Double,
    val coordinateSystem: String?,
    val name: String?,
    val address: String? = null,
)

object LocationUiBridge {
    var pickAndSend: (suspend (ULong, Int) -> Result<Unit>)? = null
    var show: ((LocationPresentation) -> Boolean)? = null
    val available: Boolean get() = pickAndSend != null
}
