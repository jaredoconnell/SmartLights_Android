package net.shadowxcraft.smartlights.ui.colors

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.shadowxcraft.smartlights.*
import net.shadowxcraft.smartlights.packets.SetColorSequenceForLEDStripPacket
import net.shadowxcraft.smartlights.ui.edit_color_sequence.ColorSequenceEditorFragment
import java.util.*
import kotlin.math.round

class ColorsFragment(private val strip: LEDStrip? = null,
                     private val scheduledChange: ScheduledChange? = null):
    Fragment(), ButtonClickListener, ClickListener
{
    private var adapter: ColorsListListAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val currentView: View = inflater.inflate(R.layout.fragment_color_sequence_list, container, false)
        if (strip != null) {
            val addButton: View = currentView.findViewById(R.id.add_color_sequence)
            addButton.setOnClickListener {
                // Open the color editor for a new color
                Utils.replaceFragment(
                    ColorSequenceEditorFragment(
                        requireActivity(),
                        ColorSequence(UUID.randomUUID().toString(), ""),
                        strip.controller,
                        strip,
                        scheduledChange
                    ), parentFragmentManager
                )
            }
            // Create adapter passing in the led strip components
            adapter = ColorsListListAdapter(strip.controller, this, this, strip, scheduledChange)

            // Lookup the recyclerview in activity layout
            val rvControllers = currentView.findViewById(R.id.list_color_sequences) as RecyclerView
            rvControllers.setHasFixedSize(true)
            val itemDecoration: RecyclerView.ItemDecoration =
                DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            rvControllers.addItemDecoration(itemDecoration)


            // Attach the adapter to the recyclerview to populate items
            rvControllers.adapter = adapter

            // Set layout manager to position the items
            rvControllers.layoutManager = LinearLayoutManager(context)
        } else {
            activity?.supportFragmentManager?.popBackStack()
        }

        return currentView
    }

    override fun onButtonClicked(position: Int, itemId: Int) {
        // Open the editor for an existing color
        val selectedSequence = strip!!.controller.colorsSequences.values.toTypedArray()[position]

        Utils.replaceFragment(ColorSequenceEditorFragment(requireActivity(),
            selectedSequence, strip.controller, strip, scheduledChange), parentFragmentManager)
    }

    override fun onPositionClicked(position: Int) {
        val correctStrip = strip ?: (scheduledChange?.ledStrip ?: return)
        val selected = correctStrip.controller.colorsSequences.values.toTypedArray()[position]
        if (scheduledChange == null) {
            // Set the color sequence for the LED strip
            if (strip!!.currentSeq == selected) {
                // Already selected, so exit.
                activity?.supportFragmentManager?.popBackStack()
            } else {
                strip.setCurrentSeq(selected, true)
                SetColorSequenceForLEDStripPacket(correctStrip).send()
            }
        } else {
            // Sets the color sequence for the scheduled change
            scheduledChange.newColorSequenceID = selected.id
            activity?.supportFragmentManager?.popBackStack()
        }
        adapter?.notifyDataSetChanged()
    }
}



// Create the basic adapter extending from RecyclerView.Adapter
// Note that we specify the custom ViewHolder which gives us access to our views
class ColorsListListAdapter(val controller: ESP32, val clickListener: ClickListener,
                            private val buttonClickListener: ButtonClickListener,
                            private val strip: LEDStrip?, private val scheduledChange: ScheduledChange?)
    : RecyclerView.Adapter<ColorsListListAdapter.ViewHolder?>()
{

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        var nameView: TextView = itemView.findViewById(R.id.item_color_sequence_name)
        var timeView: TextView = itemView.findViewById(R.id.item_color_sequence_duration)
        var imgView: ImageView = itemView.findViewById(R.id.item_color_sequence_preview)
        var editButton: Button = itemView.findViewById(R.id.item_color_sequence_edit_button)
        var selectedCheckbox: CheckBox = itemView.findViewById(R.id.color_sequence_selected_checkbox)
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
        val deviceView: View = inflater.inflate(R.layout.item_color_sequence, parent, false)

        // Return a new holder instance
        return ViewHolder(deviceView)
    }

    override fun getItemCount(): Int {
        return controller.colorsSequences.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Get the data model based on position
        val sequence: ColorSequence = controller.colorsSequences.values.toTypedArray()[position]

        // Set item views based on your views and data model
        holder.nameView.text = sequence.name
        holder.timeView.text = "Duration:\n${round(sequence.getDuration())}s"
        holder.imgView.setImageDrawable(sequence.getDrawableRepresentation())
        if (scheduledChange != null) {
            holder.selectedCheckbox.isChecked = scheduledChange.newColorSequenceID == sequence.id
        } else if (strip != null)
            holder.selectedCheckbox.isChecked = strip.currentSeq == sequence
        else
            holder.selectedCheckbox.visibility = View.GONE
        holder.editButton.setOnClickListener {
            buttonClickListener.onButtonClicked(position, R.id.item_color_sequence_edit_button)
        }
    }
}