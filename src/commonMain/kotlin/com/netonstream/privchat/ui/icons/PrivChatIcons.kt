package com.netonstream.privchat.ui.icons

import com.gearui.components.icon.IconSource
import com.gearui.components.icon.ImageIcon

/**
 * Icons of PrivChat's own, drawn where GearUI's Phosphor set does not fit. The PNGs ship
 * with the host app under `assets/icons/` (privchat-app's `src/commonMain/assets/icons`).
 */
object PrivChatIcons {
    /**
     * Two eyes and a mouth, without Phosphor smiley's face outline: inside the round
     * composer button the two circles blur together at 20dp.
     */
    val emojiFace: IconSource get() = ImageIcon("emoji_face", "assets://icons/emoji_face.png")
}
