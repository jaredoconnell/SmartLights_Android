package net.shadowxcraft.smartlights.ui.home

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import net.shadowxcraft.smartlights.MainActivity
import net.shadowxcraft.smartlights.ui.controllers.ControllersFragment
import net.shadowxcraft.smartlights.R
import net.shadowxcraft.smartlights.Utils


class LedStripsFragment : Fragment() {

    private lateinit var ledStripsViewModel: LedStripViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        ledStripsViewModel =
            ViewModelProviders.of(this).get(LedStripViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_led_strips, container, false)
        val textView: TextView = root.findViewById(R.id.text_led_strips)
        ledStripsViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })

        setHasOptionsMenu(true)

        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.led_strip_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.open_controller_menu -> {
                Utils.replaceFragment(ControllersFragment(), fragmentManager)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


}