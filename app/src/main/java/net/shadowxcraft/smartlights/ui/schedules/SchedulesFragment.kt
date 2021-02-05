package net.shadowxcraft.smartlights.ui.schedules

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.shadowxcraft.smartlights.*
import net.shadowxcraft.smartlights.ui.edit_scheduled_change.ScheduledChangeEditorFragment
import org.joda.time.format.DateTimeFormat
import java.util.*

class SchedulesFragment(private val strip: LEDStrip?): Fragment(), ClickListener {
    private var adapter: SchedulesListListAdapter? = null

    constructor() : this(null) {
        // Get out of here. Schedules must be associated with an LED Strip
        if (parentFragmentManager.backStackEntryCount > 0)
            parentFragmentManager.popBackStackImmediate()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val currentView: View = inflater.inflate(R.layout.fragment_schedules_list, container, false)
        if (strip != null) {
            val addButton: View = currentView.findViewById(R.id.add_schedule)
            addButton.setOnClickListener {
                // Open the color editor for a new color
                Utils.replaceFragment(
                    ScheduledChangeEditorFragment(
                        ScheduledChange(UUID.randomUUID().toString(), "", strip)
                    ), parentFragmentManager
                )
            }
            // Create adapter passing in the led strip components
            adapter = SchedulesListListAdapter(strip, this)

            // Lookup the recyclerview in activity layout
            val rvControllers = currentView.findViewById(R.id.list_schedules) as RecyclerView
            rvControllers.setHasFixedSize(true)
            val itemDecoration: RecyclerView.ItemDecoration =
                DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            rvControllers.addItemDecoration(itemDecoration)


            // Attach the adapter to the recyclerview to populate items
            rvControllers.adapter = adapter

            // Set layout manager to position the items
            rvControllers.layoutManager = LinearLayoutManager(context)
        } else {
            activity?.supportFragmentManager?.popBackStack();
        }

        return currentView
    }

    override fun onPositionClicked(position: Int) {
        // Open the editor for an existing schedule
        val selectedScheduledChange = strip!!.scheduledChanges.values.toTypedArray()[position]

        Utils.replaceFragment(ScheduledChangeEditorFragment(selectedScheduledChange), parentFragmentManager)
    }
}



// Create the basic adapter extending from RecyclerView.Adapter
// Note that we specify the custom ViewHolder which gives us access to our views
class SchedulesListListAdapter(val strip: LEDStrip?, val clickListener: ClickListener)
    : RecyclerView.Adapter<SchedulesListListAdapter.ViewHolder?>()
{

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        var nameView: TextView = itemView.findViewById(R.id.item_scheduled_change_name)
        var hourView: TextView = itemView.findViewById(R.id.item_scheduled_hour)
        var daysView: TextView = itemView.findViewById(R.id.item_scheduled_days_text)
        var changesView: TextView = itemView.findViewById(R.id.item_scheduled_changes_text)
        override fun onClick(v: View?) {
            clickListener.onPositionClicked(adapterPosition)
        }

        init {
            itemView.setOnClickListener(this)
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context: Context = parent.context
        val inflater = LayoutInflater.from(context)

        // Inflate the custom layout
        val deviceView: View = inflater.inflate(R.layout.item_scheduled_change, parent, false)

        // Return a new holder instance
        return ViewHolder(deviceView)
    }

    override fun getItemCount(): Int {
        return strip!!.scheduledChanges.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Get the data model based on position
        val scheduledChange: ScheduledChange = strip!!.scheduledChanges.values.toTypedArray()[position]

        // Set item views based on your views and data model
        holder.nameView.text = scheduledChange.name

        // TODO: Get the pattern from the resource.
        val hourFormatter = DateTimeFormat.forPattern("hh:mm a")
        val dateFormatter = DateTimeFormat.forPattern("EEE yyyy/MM/dd")
        holder.hourView.text = scheduledChange.getTimeLocal().toString(hourFormatter)

        holder.daysView.text = when {
            scheduledChange.isSpecificDate -> {
                scheduledChange.getTimeLocal().toString(dateFormatter)
            }
            scheduledChange.isEveryDay() -> {
                "Daily"
            }
            else -> {
                scheduledChange.getDaysOfWeekInitials()
            }
        }
        var description = ""
        if (scheduledChange.turnOn)
            description += "Turns On\n"
        if (scheduledChange.newColor != null)
            description += "Color change to:\n" + scheduledChange.newColor.toString()
        else if (scheduledChange.newColorSequenceID != null)
            description += "Color Sequence change to " +
                    (SharedData.colorsSequences[scheduledChange.newColorSequenceID!!]?.name)

        if (!scheduledChange.turnOn && scheduledChange.newColorSequenceID == null &&
            scheduledChange.newColor == null)
                description += "Turns off"

        holder.changesView.text = description
        /*holder.setOnClickListener {
            buttonClickListener.onButtonClicked(position, R.id.item_color_sequence_edit_button)
        }*/
    }
}