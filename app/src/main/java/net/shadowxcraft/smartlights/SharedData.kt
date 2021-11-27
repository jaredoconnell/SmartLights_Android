package net.shadowxcraft.smartlights

import net.shadowxcraft.smartlights.ui.home.LedStripsFragment
import net.shadowxcraft.smartlights.ui.home.LedStripGroupsFragment
import java.util.*

object SharedData {
    val colorsSequences: TreeMap<String, ColorSequence> = TreeMap()

    var ledStripsFragment: LedStripsFragment? = null
    var ledStripGroupsFragment: LedStripGroupsFragment? = null

    var loaded = false

    fun notifyDataChanged() {
        ledStripsFragment?.adapter?.notifyDataSetChanged()
        ledStripGroupsFragment?.adapter?.notifyDataSetChanged()
    }
}