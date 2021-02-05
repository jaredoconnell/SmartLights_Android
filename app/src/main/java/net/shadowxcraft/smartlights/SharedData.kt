package net.shadowxcraft.smartlights

import net.shadowxcraft.smartlights.ui.home.LedStripsFragment
import net.shadowxcraft.smartlights.ui.led_strip_groups.LedStripGroupsFragment
import java.util.*

object SharedData {
    val colorsSequences: TreeMap<String, ColorSequence> = TreeMap()

    var ledStripsFragment: LedStripsFragment? = null
    var ledStripGroupsFragment: LedStripGroupsFragment? = null

    var loaded = false
}