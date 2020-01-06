package net.shadowxcraft.smartlights

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

object Utils {
    fun replaceFragment(someFragment: Fragment?, fragmentManager: FragmentManager?) {
        val transaction: FragmentTransaction = fragmentManager!!.beginTransaction()
        transaction.replace(R.id.nav_host_fragment, someFragment!!)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}