package net.shadowxcraft.smartlights.ui.colors

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.shadowxcraft.smartlights.*
import net.shadowxcraft.smartlights.packets.AddColorSequencePacket
import net.shadowxcraft.smartlights.packets.SetColorSequenceForLEDStripPacket
import net.shadowxcraft.smartlights.ui.edit_color_sequence.ColorSequenceEditorFragment
import java.util.*
import kotlin.math.round

class ColorsFragment(private val strip: LEDStrip? = null,
                     private val scheduledChange: ScheduledChange? = null):
    Fragment(), ButtonClickListener, ClickListener
{
    private var adapter: ColorsListListAdapter? = null

    private lateinit var rvControllers: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        OrderingManager.checkColorSequenceOrder()
        val currentView: View = inflater.inflate(R.layout.fragment_color_sequence_list, container, false)
        setHasOptionsMenu(true)

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
            rvControllers = currentView.findViewById(R.id.list_color_sequences) as RecyclerView
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.color_sequence_list_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return if (id == R.id.action_reorder) {
            toggleReorderMode()
            true
        } else super.onOptionsItemSelected(item)
    }

    private fun toggleReorderMode() {
        if (adapter == null)
            return
        adapter?.reorderMode = !adapter!!.reorderMode
        adapter?.notifyDataSetChanged()

        if (adapter!!.reorderMode) {
            itemTouchHelper.attachToRecyclerView(rvControllers)
            Toast.makeText(context, "Reordering mode enabled", Toast.LENGTH_SHORT).show()
        } else {
            itemTouchHelper.attachToRecyclerView(null)
            Toast.makeText(context, "Reordering mode disabled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onButtonClicked(position: Int, itemId: Int) {
        // Open the editor for an existing color
        val selectedSequence = adapter!!.getNthColorSequence(position)

        if (selectedSequence == null) {
            Toast.makeText(context, "Error finding matching color sequence", Toast.LENGTH_SHORT).show()
            return
        }

        Utils.replaceFragment(ColorSequenceEditorFragment(requireActivity(),
            selectedSequence, strip!!.controller, strip, scheduledChange), parentFragmentManager)
    }

    override fun onPositionClicked(position: Int) {
        val correctStrip = strip ?: (scheduledChange?.ledStrip ?: return)
        val selected = adapter!!.getNthColorSequence(position)
        if (selected == null) {
            Toast.makeText(context, "Error finding matching color sequence", Toast.LENGTH_SHORT).show()
            return
        }
        if (scheduledChange == null) {
            // Set the color sequence for the LED strip
            if (strip!!.currentSeq == selected) {
                // Already selected, so exit.
                activity?.supportFragmentManager?.popBackStack()
            } else {
                strip.setCurrentSeq(selected, true)
                AddColorSequencePacket(strip.controller, selected).send()
                SetColorSequenceForLEDStripPacket(correctStrip).send()
            }
        } else {
            // Sets the color sequence for the scheduled change
            scheduledChange.newColorSequenceID = selected.id
            activity?.supportFragmentManager?.popBackStack()
        }
        adapter?.notifyDataSetChanged()
    }


    private val itemTouchHelper by lazy {
        val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN
                    or ItemTouchHelper.START or ItemTouchHelper.END, 0
        )
        {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                if (!adapter!!.reorderMode)
                    return false
                val adapter = recyclerView.adapter as RecyclerView.Adapter<*>
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                OrderingManager.moveColorSequenceOrder(from, to)
                adapter.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)

                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.alpha = 0.5f
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)

                viewHolder.itemView.alpha = 1.0f
            }
        }

        ItemTouchHelper(simpleItemTouchCallback)
    }
}



// Create the basic adapter extending from RecyclerView.Adapter
// Note that we specify the custom ViewHolder which gives us access to our views
class ColorsListListAdapter(val controller: ESP32, val clickListener: ClickListener,
                            private val buttonClickListener: ButtonClickListener,
                            private val strip: LEDStrip?, private val scheduledChange: ScheduledChange?)
    : RecyclerView.Adapter<ColorsListListAdapter.ViewHolder?>()
{

    var reorderMode = false

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
        private val dragHandle: ImageView = itemView.findViewById(R.id.drag_handle)

        override fun onClick(v: View?) {
            clickListener.onPositionClicked(adapterPosition)
        }

        init {
            itemView.setOnClickListener(this)
        }

        fun setColorSequence(sequence: ColorSequence) {
            // Set item views based on your views and data model
            nameView.text = sequence.name
            timeView.text = "Duration:\n${round(sequence.getDuration())}s"
            imgView.setImageDrawable(sequence.getDrawableRepresentation())

            when {
                scheduledChange != null -> {
                    selectedCheckbox.isChecked = scheduledChange.newColorSequenceID == sequence.id
                }
                strip != null -> selectedCheckbox.isChecked = strip.currentSeq == sequence
                else -> selectedCheckbox.visibility = View.GONE
            }

            dragHandle.visibility = if (reorderMode) {
                View.VISIBLE
            } else {
                View.GONE
            }
            editButton.visibility = if (reorderMode) {
                View.GONE
            } else {
                View.VISIBLE
            }
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
        return SharedData.colorsSequences.size
    }

    fun getNthColorSequence(index: Int) : ColorSequence? {
        val colorSequenceList = OrderingManager.colorSequencePositions

        if (index >= colorSequenceList.size)
            OrderingManager.checkColorSequenceOrder()
        if (index >= colorSequenceList.size)
            return null
        val uuid = colorSequenceList[index]
        return SharedData.colorsSequences[uuid]
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Get the data model based on position
        val sequence: ColorSequence = getNthColorSequence(position) ?: return

        holder.setColorSequence(sequence)

        holder.editButton.setOnClickListener {
            buttonClickListener.onButtonClicked(position, R.id.item_color_sequence_edit_button)
        }
    }
}