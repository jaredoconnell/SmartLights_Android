package net.shadowxcraft.smartlights.ui.controllers

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.shadowxcraft.smartlights.*

// TODO: Fix memory leaks caused by things referencing each other.

// Create the basic adapter extending from RecyclerView.Adapter
// Note that we specify the custom ViewHolder which gives us access to our views
class ControllerListAdapter(deviceList: ArrayList<ESP32>,
                            private val pwmButtonClickListener: ButtonClickListener)
    : RecyclerView.Adapter<ControllerListAdapter.ViewHolder?>()
{
    private val devicesList: ArrayList<ESP32> = deviceList

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        var nameTextView: TextView = itemView.findViewById(R.id.controller_name)
        var addPWMDriverButton: Button = itemView.findViewById(R.id.add_pwm_driver_button)
        var addLEDStripButton: Button = itemView.findViewById(R.id.add_led_strip_button)
        var renameControllerButton: ImageView = itemView.findViewById(R.id.rename_controller_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context: Context = parent.context
        val inflater = LayoutInflater.from(context)

        // Inflate the custom layout
        val deviceView: View = inflater.inflate(R.layout.item_controller, parent, false)

        // Return a new holder instance
        return ViewHolder(deviceView)
    }

    override fun getItemCount(): Int {
        return devicesList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Get the data model based on position
        val device: ESP32 = devicesList[position]

        // Set item views based on your views and data model
        val nameTextView: TextView = holder.nameTextView
        nameTextView.text = device.name
        val pwmDriverButton: Button = holder.addPWMDriverButton
        val renameControllerButton: ImageView = holder.renameControllerButton
        val addLEDStripButton: Button = holder.addLEDStripButton
        pwmDriverButton.setOnClickListener {
            pwmButtonClickListener.onButtonClicked(position, R.id.add_pwm_driver_button)
        }
        addLEDStripButton.setOnClickListener {
            pwmButtonClickListener.onButtonClicked(position, R.id.add_led_strip_button)
        }
        renameControllerButton.setOnClickListener {
            pwmButtonClickListener.onButtonClicked(position, R.id.rename_controller_button)
        }

    }
}