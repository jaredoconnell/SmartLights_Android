package net.shadowxcraft.smartlights

import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import net.shadowxcraft.smartlights.ui.home.LedStripsFragment
import net.shadowxcraft.smartlights.ui.home.LedStripGroupsFragment
import java.util.*

object SharedData {
    val colorsSequences: TreeMap<String, ColorSequence> = TreeMap()

    var navFragment: Fragment? = null

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

    fun notifyDataChanged() {
        ledStripsFragment?.adapter?.notifyDataSetChanged()
        ledStripGroupsFragment?.adapter?.notifyDataSetChanged()
    }
}