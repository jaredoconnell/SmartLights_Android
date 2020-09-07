package net.shadowxcraft.smartlights.ui.add_led_strip

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.shadowxcraft.smartlights.LEDStripComponent
import net.shadowxcraft.smartlights.R


// Create the basic adapter extending from RecyclerView.Adapter
// Note that we specify the custom ViewHolder which gives us access to our views
class LEDStripComponentListAdapter(private val fragment: LEDStripComponentFragment, private val componentList: ArrayList<LEDStripComponent>)
    : RecyclerView.Adapter<LEDStripComponentListAdapter.ViewHolder?>()
{

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        var colorView: View = itemView.findViewById(R.id.led_strip_component_color)
        var driverView: TextView = itemView.findViewById(R.id.led_strip_component_driver)
        var pinView: TextView = itemView.findViewById(R.id.led_strip_component_pin)
        override fun onClick(v: View?) {
            val component = componentList[adapterPosition]
            EditComponentDialog(fragment, fragment.requireActivity(), component)
        }

        init {
            itemView.setOnClickListener(this)
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context: Context = parent.context
        val inflater = LayoutInflater.from(context)

        // Inflate the custom layout
        val deviceView: View = inflater.inflate(R.layout.item_led_strip_component, parent, false)

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
    }
}