package net.shadowxcraft.smartlights

import net.shadowxcraft.smartlights.ui.home.LedStripsFragment
import net.shadowxcraft.smartlights.ui.led_strip_groups.LedStripGroupsFragment

object SharedData {
    var ledStripsFragment: LedStripsFragment? = null
    var ledStripGroupsFragment: LedStripGroupsFragment? = null

    var loaded = false
}