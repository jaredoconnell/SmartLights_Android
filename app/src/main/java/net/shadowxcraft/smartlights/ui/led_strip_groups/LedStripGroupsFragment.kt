package net.shadowxcraft.smartlights.ui.led_strip_groups

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.shadowxcraft.smartlights.*
import net.shadowxcraft.smartlights.packets.SetColorForLEDStripPacket
import net.shadowxcraft.smartlights.ui.color_editor.ColorEditorDialog
import net.shadowxcraft.smartlights.ui.colors.ColorsFragment
import net.shadowxcraft.smartlights.ui.controllers.ControllersFragment
import net.shadowxcraft.smartlights.ui.schedules.SchedulesFragment


class LedStripGroupsFragment : Fragment(), ButtonClickListener, ColorEditorDialog.ColorSelectedListener {

    var adapter: LEDStripListAdapter? = null

    var dialog: ColorEditorDialog? = null

    private lateinit var rvControllers: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        OrderingManager.checkLedStripGroupOrders()

        val root = inflater.inflate(R.layout.fragment_led_strips, container, false)

        setHasOptionsMenu(true)

        val floatingButton: View = root.findViewById(R.id.led_floating_action_button)
        floatingButton.setOnClickListener {
            // Show menu to add LED Strip
            Utils.replaceFragment(ControllersFragment(), parentFragmentManager)
        }

        // Lookup the recyclerview in activity layout
        rvControllers = root.findViewById(R.id.list_led_strip) as RecyclerView
        rvControllers.setHasFixedSize(true)
        val itemDecoration: RecyclerView.ItemDecoration =
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        rvControllers.addItemDecoration(itemDecoration)

        // Create adapter that uses the list of LED strips.
        adapter = LEDStripListAdapter(this, rvControllers)


        // Attach the adapter to the recyclerview to populate items
        rvControllers.adapter = adapter

        // Set layout manager to position the items
        rvControllers.layoutManager = LinearLayoutManager(context)

        SharedData.ledStripGroupsFragment = this

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        SharedData.ledStripGroupsFragment = null
    }

    override fun onButtonClicked(position: Int, itemId: Int) {
        val ledStrip = adapter!!.getNthLEDStripGroup(position)
        when (itemId) {
            R.id.set_colors_button -> {
                Utils.replaceFragment(ColorsFragment(ledStrip), parentFragmentManager)
            }
            R.id.set_color_button -> {
                dialog = ColorEditorDialog(
                    BLEControllerManager.activity!!,
                    Color(255, 0, 0),
                    ledStrip
                )
                dialog!!.listener = this
                dialog!!.display()
            }
            R.id.edit_schedules_button -> {
                // TODO
                Utils.replaceFragment(SchedulesFragment(ledStrip), parentFragmentManager)
            }
        }
    }

    override fun onColorSelected(color: Color) {
        dialog!!.ledStrip!!.setSimpleColor(color, true)
        dialog!!.ledStrip!!.setCurrentSeq(null, true)
        // Clear the preview that likely built up.
        dialog!!.ledStrip!!.controller.clearQueueForPacketID(19)
        SetColorForLEDStripPacket(dialog!!.ledStrip!!, color, 0u).send()// indefinitely
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.led_strip_list_menu, menu)
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


    private val itemTouchHelper by lazy {
        val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN
                    or ItemTouchHelper.START or ItemTouchHelper.END, 0)
        {

            override fun onMove(recyclerView: RecyclerView,
                                viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder): Boolean {
                if (!adapter!!.reorderMode)
                    return false
                val adapter = recyclerView.adapter as RecyclerView.Adapter<*>
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                OrderingManager.moveLEDStripGroupOrder(from, to)
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

                viewHolder?.itemView?.alpha = 1.0f
            }
        }

        ItemTouchHelper(simpleItemTouchCallback)
    }
}


// Create the basic adapter extending from RecyclerView.Adapter
// Note that we specify the custom ViewHolder which gives us access to our views
class LEDStripListAdapter(
    private val setColorClickListener: ButtonClickListener, private val view: RecyclerView
)
    : RecyclerView.Adapter<LEDStripListAdapter.ViewHolder?>()
{

    var reorderMode = false

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
        private val offOnStateToggle: SwitchCompat = itemView.findViewById(R.id.on_off_switch)
        private val brightnessBar: SeekBar = itemView.findViewById(R.id.brightness_bar)
        private val dragHandle: ImageView = itemView.findViewById(R.id.drag_handle)
        private var ledStripGroup: LEDStripGroup? = null


        fun setLEDStripGroup(ledStripGroup: LEDStripGroup) {
            this.ledStripGroup = ledStripGroup
            nameView.text = ledStripGroup.name
            offOnStateToggle.isChecked = ledStripGroup.onState
            offOnStateToggle.setOnCheckedChangeListener { _, isChecked ->
                if (offOnStateToggle.isPressed) {
                    ledStripGroup.setOnState(isChecked, true)
                    ledStripGroup.sendBrightnessPacket(true)
                    this@LEDStripListAdapter.notifyDataSetChanged()
                }
            }
            dragHandle.visibility = if (reorderMode) {
                View.VISIBLE
            } else {
                View.GONE
            }
            brightnessBar.max = MAX_BRIGHTNESS
            brightnessBar.progress = ledStripGroup.getBrightnessExponential()
            brightnessBar.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int, fromUser: Boolean
                ) {
                    if (fromUser)
                        setBrightness(seekBar, progress, false)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    seekBar?.let { setBrightness(seekBar, it.progress, true) }
                }

                fun setBrightness(seekBar: SeekBar?, newVal: Int, updateList: Boolean) {
                    if (seekBar != null) {
                        ledStripGroup.setBrightnessExponential(seekBar.progress, true)
                    }
                    ledStripGroup.sendBrightnessPacket(false)
                    if (updateList) {
                        view.post(Runnable { this@LEDStripListAdapter.notifyDataSetChanged() })
                    }
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
            size += controller.ledStripGroups.size
        return size
    }

    fun getNthLEDStripGroup(index: Int) : LEDStripGroup? {
        if (index >= OrderingManager.ledStripGroupPositions.size)
            OrderingManager.checkLedStripOrders()
        if (index >= OrderingManager.ledStripGroupPositions.size)
            return null
        val uuid = OrderingManager.ledStripGroupPositions[index]
        return ControllerManager.getLEDStripByID(uuid) as LEDStripGroup?
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Get the data model based on position
        // Find the applicable controller
        val component: LEDStripGroup? = getNthLEDStripGroup(position)
            // Set item views based on your views and data model
        if (component != null)
            holder.setLEDStripGroup(component)

        /*holder.colorView.setBackgroundColor(android.graphics.Color.argb(255, color.red, color.green, color.blue))
        holder.driverView.text = component.driver.i2cAddress.toString()
        holder.pinView.text = component.driverPin.toString()*/
    }
}