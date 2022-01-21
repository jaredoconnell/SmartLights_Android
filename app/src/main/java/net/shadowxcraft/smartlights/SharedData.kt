package net.shadowxcraft.smartlights

import androidx.fragment.app.Fragment
import net.shadowxcraft.smartlights.ui.edit_color_sequence.ColorSequenceEditorFragment
import net.shadowxcraft.smartlights.ui.home.LedStripGroupsFragment
import net.shadowxcraft.smartlights.ui.home.LedStripsFragment
import java.util.*

object SharedData {
    val colorsSequences: TreeMap<String, ColorSequence> = TreeMap()

    var navFragment: Fragment? = null

    var editColorSequenceFragment: ColorSequenceEditorFragment? = null

    var ledStripsFragment: LedStripsFragment? = null
        get() {
            if (navFragment != null) {
                navFragment?.let { navFragment ->
                    navFragment.childFragmentManager.primaryNavigationFragment?.let {fragment->
                        if (fragment is LedStripsFragment)
                            return fragment
                    }
                }
            }
            return field
        }
    var ledStripGroupsFragment: LedStripGroupsFragment? = null
        get() {
            if (navFragment != null) {
                navFragment?.let { navFragment ->
                    navFragment.childFragmentManager.primaryNavigationFragment?.let {fragment->
                        if (fragment is LedStripGroupsFragment)
                            return fragment
                    }
                }
            }
            return field
        }

    var loaded = false

    fun notifyDataChanged(activity: MainActivity) {
        activity.runOnUiThread {
            ledStripsFragment?.adapter?.notifyDataSetChanged()
            ledStripGroupsFragment?.adapter?.notifyDataSetChanged()
        }
    }
}