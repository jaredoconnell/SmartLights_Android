package net.shadowxcraft.smartlights.ui.home

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.shadowxcraft.smartlights.*
import net.shadowxcraft.smartlights.BLEControllerManager.activity
import net.shadowxcraft.smartlights.packets.SetColorForLEDStripPacket
import net.shadowxcraft.smartlights.ui.color_editor.ColorEditorDialog
import net.shadowxcraft.smartlights.ui.colors.ColorsFragment
import net.shadowxcraft.smartlights.ui.controllers.ControllersFragment
import net.shadowxcraft.smartlights.ui.schedules.SchedulesFragment


class LedStripsFragment : Fragment(), ButtonClickListener, ColorEditorDialog.ColorSelectedListener {

    var adapter: LEDStripListAdapter? = null

    var dialog: ColorEditorDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_led_strips, container, false)

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

        (activity as MainActivity).ledStripsFragment = this

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as MainActivity).ledStripsFragment = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        //inflater.inflate(R.menu.led_strip_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onButtonClicked(position: Int, itemId: Int) {
        val ledStrip = adapter!!.getNthLEDStrip(position)
        when (itemId) {
            R.id.set_colors_button -> {
                Utils.replaceFragment(ColorsFragment(ledStrip), parentFragmentManager)
            }
            R.id.set_color_button -> {
                dialog = ColorEditorDialog(BLEControllerManager.activity!!, ledStrip!!.simpleColor, ledStrip!!)
                dialog!!.listener = this
                dialog!!.display()
            }
            R.id.edit_schedules_button -> {
                Utils.replaceFragment(SchedulesFragment(ledStrip), parentFragmentManager)
            }
        }
    }

    override fun onColorSelected(color: Color) {
        dialog!!.ledStrip!!.simpleColor = color
        dialog!!.ledStrip!!.currentSeq = null
        // Clear the preview that likely built up.
        dialog!!.ledStrip!!.controller.clearQueueForPacketID(19)
        SetColorForLEDStripPacket(dialog!!.ledStrip!!, color, 0).send()// indefinitely
        adapter?.notifyDataSetChanged()
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
    inner class ViewHolder(itemView: View)
        : RecyclerView.ViewHolder(itemView)
    {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        private val nameView: TextView = itemView.findViewById(R.id.led_strip_name)
        private val colorsButtonView: ImageView = itemView.findViewById(R.id.set_colors_button)
        private val colorButtonView: ImageView = itemView.findViewById(R.id.set_color_button)
        private val editSchedulesView: ImageView = itemView.findViewById(R.id.edit_schedules_button)
        private val offOnStateToggle: Switch = itemView.findViewById(R.id.on_off_switch)
        val brightnessBar: SeekBar = itemView.findViewById(R.id.brightness_bar)
        private var ledStrip: LEDStrip? = null


        fun setLEDStrip(ledStrip: LEDStrip) {
            this.ledStrip = ledStrip
            nameView.text = ledStrip.name
            offOnStateToggle.isChecked = ledStrip.onState
            offOnStateToggle.setOnCheckedChangeListener { _, isChecked ->
                ledStrip.onState = isChecked
                ledStrip.sendBrightnessPacket()
                if (isChecked && ledStrip.brightness < 300
                    && (activity as MainActivity).getLuxVal() > 100)
                {
                    Toast.makeText(activity,
                        "The LED is dim.",
                        Toast.LENGTH_SHORT).show()
                }
            }
            brightnessBar.max = MAX_BRIGHTNESS
            brightnessBar.progress = ledStrip.getBrightnessExponential()
            brightnessBar.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seek: SeekBar,
                                               progress: Int, fromUser: Boolean)
                {
                    ledStrip.setBrightnessExponential(progress)
                    if (ledStrip.brightness == 0) {
                        // It's low enough that it rounds down to 0
                        brightnessBar.progress = 0
                    }
                    ledStrip.sendBrightnessPacket()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }
            })
            colorsButtonView.setOnClickListener {
                setColorClickListener.onButtonClicked(adapterPosition, R.id.set_colors_button)
            }
            colorButtonView.setOnClickListener {
                setColorClickListener.onButtonClicked(adapterPosition, R.id.set_color_button)
            }
            editSchedulesView.setOnClickListener {
                setColorClickListener.onButtonClicked(adapterPosition, R.id.edit_schedules_button)
            }
            colorButtonView.drawable.mutate().setTint(ledStrip.simpleColor.toArgb())
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
            size += controller.ledStrips.size
        return size
    }

    fun getNthLEDStrip(index: Int) : LEDStrip {
        var controllerIndex = 0
        var positionsRemaining = index
        while (ControllerManager.controllers[controllerIndex].ledStrips.size < positionsRemaining) {
            positionsRemaining -= ControllerManager.controllers[controllerIndex].ledStrips.size
            controllerIndex++
        }
        val ledStrips = ControllerManager.controllers[controllerIndex].ledStrips
        return ledStrips.values.toTypedArray()[positionsRemaining]
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Get the data model based on position
        // Find the applicable controller
        val component: LEDStrip = getNthLEDStrip(position)
            // Set item views based on your views and data model
        holder.setLEDStrip(component)

        /*holder.colorView.setBackgroundColor(android.graphics.Color.argb(255, color.red, color.green, color.blue))
        holder.driverView.text = component.driver.i2cAddress.toString()
        holder.pinView.text = component.driverPin.toString()*/
    }
}