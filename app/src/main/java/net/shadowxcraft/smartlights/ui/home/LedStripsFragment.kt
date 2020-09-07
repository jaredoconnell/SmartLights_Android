package net.shadowxcraft.smartlights.ui.home

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.shadowxcraft.smartlights.*
import net.shadowxcraft.smartlights.ui.colors.ColorsFragment
import net.shadowxcraft.smartlights.ui.controllers.ControllersFragment


class LedStripsFragment : Fragment(), ButtonClickListener {

    private var adapter: LEDStripListAdapter? = null

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

        val floatingButton: View = root.findViewById(R.id.led_floating_action_button)
        floatingButton.setOnClickListener {
            // Show menu to add LED Strip
            Utils.replaceFragment(ControllersFragment(), parentFragmentManager)
        }

        // Create adapter that uses the list of LED strips.
        adapter = LEDStripListAdapter(this)

        // Lookup the recyclerview in activity layout
        val rvControllers = root.findViewById(R.id.list_led_strip) as RecyclerView
        rvControllers.setHasFixedSize(true)
        val itemDecoration: RecyclerView.ItemDecoration =
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        rvControllers.addItemDecoration(itemDecoration)


        // Attach the adapter to the recyclerview to populate items
        rvControllers.adapter = adapter

        // Set layout manager to position the items
        rvControllers.layoutManager = LinearLayoutManager(context)

        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        //inflater.inflate(R.menu.led_strip_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onButtonClicked(position: Int, itemId: Int) {
        val ledStrip = adapter!!.getNthLEDStrip(position)
        Utils.replaceFragment(ColorsFragment(ledStrip), parentFragmentManager)
    }

    /*override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.open_controller_menu -> {
                Utils.replaceFragment(Bluetooth, fragmentManager)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }*/
}


// Create the basic adapter extending from RecyclerView.Adapter
// Note that we specify the custom ViewHolder which gives us access to our views
class LEDStripListAdapter(
    private val setColorClickListener: ButtonClickListener)
    : RecyclerView.Adapter<LEDStripListAdapter.ViewHolder?>()
{

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        var nameView: TextView = itemView.findViewById(R.id.led_strip_name)
        var colorsButtonView: Button = itemView.findViewById(R.id.set_colors_button)
        override fun onClick(v: View?) {
            //val component = componentList[adapterPosition]

        }

        init {
            itemView.setOnClickListener(this)
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context: Context = parent.context
        val inflater = LayoutInflater.from(context)

        // Inflate the custom layout
        val deviceView: View = inflater.inflate(R.layout.item_led_strip, parent, false)

        // Return a new holder instance
        return ViewHolder(deviceView)
    }

    override fun getItemCount(): Int {
        var size = 0;
        for (controller in ControllerManager.controllers)
            size += controller.ledStrips.size()
        return size
    }

    fun getNthLEDStrip(index: Int) : LEDStrip {
        var controllerIndex = 0
        var positionsRemaining = index
        while (ControllerManager.controllers[controllerIndex].ledStrips.size() < positionsRemaining) {
            positionsRemaining -= ControllerManager.controllers[controllerIndex].ledStrips.size()
            controllerIndex++
        }
        val ledStrips = ControllerManager.controllers[controllerIndex].ledStrips
        return ledStrips[ledStrips.keyAt(positionsRemaining)]
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Get the data model based on position
        // Find the applicable controller
        val component: LEDStrip = getNthLEDStrip(position)
            // Set item views based on your views and data model
        holder.nameView.text = component.name
        holder.colorsButtonView.setOnClickListener {
            setColorClickListener.onButtonClicked(position, R.id.set_colors_button)
        }
        /*holder.colorView.setBackgroundColor(android.graphics.Color.argb(255, color.red, color.green, color.blue))
        holder.driverView.text = component.driver.i2cAddress.toString()
        holder.pinView.text = component.driverPin.toString()*/
    }
}