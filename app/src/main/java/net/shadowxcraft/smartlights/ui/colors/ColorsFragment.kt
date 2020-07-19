package net.shadowxcraft.smartlights.ui.colors

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.shadowxcraft.smartlights.*
import net.shadowxcraft.smartlights.packets.SetColorSequenceForLEDStripPacket
import net.shadowxcraft.smartlights.ui.edit_color_sequence.ColorSequenceEditorFragment

class ColorsFragment(private val strip: LEDStrip?): Fragment(), ButtonClickListener, ClickListener {
    private var adapter: ColorsListListAdapter? = null

    constructor() : this(null)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val currentView: View = inflater.inflate(R.layout.fragment_color_sequence_list, container, false)
        if (strip != null) {
            val addButton: View = currentView.findViewById(R.id.add_color_sequence)
            addButton.setOnClickListener {
                // Open the color editor for a new color
                Utils.replaceFragment(
                    ColorSequenceEditorFragment(
                        requireActivity(),
                        ColorSequence(strip.controller.getNextColorStripID(), "New Color Sequence"),
                        strip.controller,
                        strip
                    ), parentFragmentManager
                )
            }
            // Create adapter passing in the led strip components
            adapter = ColorsListListAdapter(strip.controller, this, this)

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
            activity?.supportFragmentManager?.popBackStack();
        }

        return currentView
    }

    override fun onButtonClicked(position: Int) {
        // Open the editor for an existing color

        val keyAtPosition = strip!!.controller.colorsSequences.keyAt(position)
        val selectedSequence = strip.controller.colorsSequences.get(keyAtPosition)

        Utils.replaceFragment(ColorSequenceEditorFragment(requireActivity(),
            selectedSequence, strip.controller, strip), parentFragmentManager)
    }

    override fun onPositionClicked(position: Int) {
        // Set the color sequence for the LED strip
        val keyAtPosition = strip!!.controller.colorsSequences.keyAt(position)
        strip.currentSeq = strip.controller.colorsSequences.get(keyAtPosition)
        SetColorSequenceForLEDStripPacket(strip).send()
    }
}



// Create the basic adapter extending from RecyclerView.Adapter
// Note that we specify the custom ViewHolder which gives us access to our views
class ColorsListListAdapter(val controller: ESP32, val clickListener: ClickListener,
                            private val buttonClickListener: ButtonClickListener)
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
        return controller.colorsSequences.size()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Get the data model based on position
        val keyAtPosition = controller.colorsSequences.keyAt(position)
        val sequence: ColorSequence = controller.colorsSequences.get(keyAtPosition)

        // Set item views based on your views and data model
        holder.nameView.text = sequence.name
        holder.timeView.text = "Duration: ${sequence.getDuration()}s"
        holder.imgView.setImageDrawable(sequence.getDrawableRepresentation())
        holder.editButton.setOnClickListener {
            buttonClickListener.onButtonClicked(position)
        }
    }
}