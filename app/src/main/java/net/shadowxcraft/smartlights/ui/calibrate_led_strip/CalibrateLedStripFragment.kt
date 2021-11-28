package net.shadowxcraft.smartlights.ui.calibrate_led_strip

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.NumberPicker
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import net.shadowxcraft.smartlights.LEDStrip
import net.shadowxcraft.smartlights.LEDStripComponent
import net.shadowxcraft.smartlights.R
import net.shadowxcraft.smartlights.R.layout
import net.shadowxcraft.smartlights.packets.SetLEDStripCalibrationMode
import net.shadowxcraft.smartlights.packets.SetLEDStripCalibrationValue


class CalibrateLedStripFragment(private val ledStrip: LEDStrip)
    : Fragment() {
    private lateinit var currentView: View
    private lateinit var adapter: LEDStripComponentCalibrationListAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        currentView = inflater.inflate(layout.fragment_calibrate_led_strip, container, false)

        // Create adapter passing in the led strip components

        // Lookup the recyclerview in activity layout
        val rvComponents = currentView.findViewById(R.id.list_components) as RecyclerView
        (currentView.findViewById(R.id.led_strip_name) as TextView).text = "Calibrating " + ledStrip.name
        rvComponents.setHasFixedSize(true)
        val itemDecoration: RecyclerView.ItemDecoration =
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        rvComponents.addItemDecoration(itemDecoration)

        adapter = LEDStripComponentCalibrationListAdapter(ledStrip, ledStrip.components)

        // Attach the adapter to the recyclerview to populate items
        rvComponents.adapter = adapter

        return currentView
    }

    override fun onStart() {
        super.onStart()
        SetLEDStripCalibrationMode(ledStrip, true, -1).send()
    }

    override fun onStop() {
        super.onStop()
        SetLEDStripCalibrationMode(ledStrip, false, -1).send()
    }
}

// Create the basic adapter extending from RecyclerView.Adapter
// Note that we specify the custom ViewHolder which gives us access to our views
class LEDStripComponentCalibrationListAdapter(
    private val ledStrip: LEDStrip,
    private val componentList: ArrayList<LEDStripComponent>
)
    : RecyclerView.Adapter<LEDStripComponentCalibrationListAdapter.ViewHolder?>()
{

    private var selectedIndex = -1

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        var colorView: View = itemView.findViewById(R.id.led_strip_component_color)
        var driverView: TextView = itemView.findViewById(R.id.led_strip_component_driver)
        var pinView: TextView = itemView.findViewById(R.id.led_strip_component_pin)
        var checkBox: CheckBox = itemView.findViewById(R.id.led_strip_component_selected_checkbox)
        var numberPicker: NumberPicker = itemView.findViewById(R.id.calibration_value_number_picker)
        var seekBar: SeekBar = itemView.findViewById(R.id.calibration_value_seek_bar)
        override fun onClick(v: View?) {
            val component = componentList[adapterPosition]
            // TODO
        }

        init {
            itemView.setOnClickListener(this)
            numberPicker.maxValue = 4095
            numberPicker.value = 4095
            seekBar.max = 4095
            seekBar.progress = 4095
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        // Inflate the custom layout
        val deviceView: View = inflater.inflate(
            R.layout.item_led_strip_calibration_component,
            parent,
            false
        )

        // Return a new holder instance
        return ViewHolder(deviceView)
    }

    override fun getItemCount(): Int {
        return componentList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Get the data model based on position
        val component: LEDStripComponent = componentList[position]

        // Set item views based on your views and data model
        val color = component.color
        holder.colorView.setBackgroundColor(color.toArgb())
        holder.driverView.text = component.driver.toString()
        holder.pinView.text = component.driverPin.toString()
        holder.checkBox.isEnabled = component.color.hasWhite()
        holder.checkBox.isChecked = position == selectedIndex
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (selectedIndex != position) {
                    // New position selected
                    selectedIndex = position
                    Log.println(
                        Log.INFO, "CalibrateLedStripFrag", "New position selected: $position"
                    )
                    SetLEDStripCalibrationMode(ledStrip, true, position).send()
                    this.notifyDataSetChanged()
                }
            } else {
                // Unchecked
                if (selectedIndex == position) {
                    // Unchecked one that was selected
                    Log.println(
                        Log.INFO, "CalibrateLedStripFrag", "Position unselected: $position"
                    )
                    SetLEDStripCalibrationMode(ledStrip, true, -1).send()
                }
                // Else means it was selected elsewhere. Do nothing
            }
        }
        holder.numberPicker.setOnValueChangedListener { _, _, newVal ->
            SetLEDStripCalibrationValue(ledStrip, position, newVal).queue()
            holder.seekBar.progress = newVal
        }
        holder.seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    SetLEDStripCalibrationValue(ledStrip, position, progress).queue()
                    holder.numberPicker.value = progress
                }
            }
        })
    }
}