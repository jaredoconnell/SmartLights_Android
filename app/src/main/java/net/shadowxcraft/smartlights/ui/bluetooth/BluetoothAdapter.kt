package net.shadowxcraft.smartlights.ui.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.shadowxcraft.smartlights.ClickListener
import net.shadowxcraft.smartlights.R


// Create the basic adapter extending from RecyclerView.Adapter
// Note that we specify the custom ViewHolder which gives us access to our views
class BluetoothAdapter(deviceList: ArrayList<BluetoothDevice>, clickListener: ClickListener) : RecyclerView.Adapter<BluetoothAdapter.ViewHolder?>() {
    private val devicesList: ArrayList<BluetoothDevice> = deviceList
    private val clickListener = clickListener

    // Provide a direct reference to each of the views within a data item
// Used to cache the views within the item layout for fast access
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        // Your holder should contain a member variable
// for any view that will be set as you render a row
        var nameTextView: TextView = itemView.findViewById(R.id.bluetooth_device_name)
        var addressTextView: TextView = itemView.findViewById(R.id.bluetooth_device_address)
        var connectButton: Button =
            itemView.findViewById(R.id.bluetooth_device_connect_button) as Button

        init {
            connectButton.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            clickListener.onPositionClicked(adapterPosition)
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context: Context = parent.context
        val inflater = LayoutInflater.from(context)

        // Inflate the custom layout
        val deviceView: View = inflater.inflate(R.layout.item_bluetooth, parent, false)

        // Return a new holder instance
        return ViewHolder(deviceView)
    }

    override fun getItemCount(): Int {
        return devicesList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Get the data model based on position
        val device: BluetoothDevice = devicesList[position]

        // Set item views based on your views and data model
        val nameTextView: TextView = holder.nameTextView
        nameTextView.text = device.name
        val addressTextView: TextView = holder.addressTextView
        addressTextView.text = device.address
        //val button: Button = holder.connectButton
    }
}