package net.shadowxcraft.smartlights.ui.home

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.shadowxcraft.smartlights.*
import net.shadowxcraft.smartlights.BLEControllerManager.activity
import net.shadowxcraft.smartlights.packets.SetColorForLEDStripPacket
import net.shadowxcraft.smartlights.ui.calibrate_led_strip.CalibrateLedStripFragment
import net.shadowxcraft.smartlights.ui.color_editor.ColorEditorDialog
import net.shadowxcraft.smartlights.ui.colors.ColorsFragment
import net.shadowxcraft.smartlights.ui.controllers.ControllersFragment
import net.shadowxcraft.smartlights.ui.schedules.SchedulesFragment



class LedStripsFragment : Fragment(), ButtonClickListener, ColorEditorDialog.ColorSelectedListener {

    var adapter: LEDStripListAdapter? = null

    var dialog: ColorEditorDialog? = null

    private lateinit var rvControllers: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        ControllerManager.checkLedStripOrders()

        val root = inflater.inflate(R.layout.fragment_led_strips, container, false)

        setHasOptionsMenu(true)

        val floatingButton: View = root.findViewById(R.id.led_floating_action_button)
        floatingButton.setOnClickListener {
            // Show menu to add LED Strip
            Utils.replaceFragment(ControllersFragment(), parentFragmentManager)
        }

        // Create adapter that uses the list of LED strips.
        adapter = LEDStripListAdapter(this, parentFragmentManager)

        // Lookup the recyclerview in activity layout
        rvControllers = root.findViewById(R.id.list_led_strip) as RecyclerView
        rvControllers.setHasFixedSize(true)
        val itemDecoration: RecyclerView.ItemDecoration =
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        rvControllers.addItemDecoration(itemDecoration)


        // Attach the adapter to the recyclerview to populate items
        rvControllers.adapter = adapter

        // Set layout manager to position the items
        rvControllers.layoutManager = LinearLayoutManager(context)

        SharedData.ledStripsFragment = this

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        SharedData.ledStripsFragment = null
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

    override fun onButtonClicked(position: Int, itemId: Int) {
        val ledStrip = adapter!!.getNthLEDStrip(position)
        when (itemId) {
            R.id.set_colors_button -> {
                Utils.replaceFragment(ColorsFragment(ledStrip), parentFragmentManager)
            }
            R.id.set_color_button -> {
                dialog = ColorEditorDialog(
                    BLEControllerManager.activity!!,
                    ledStrip!!.simpleColor,
                    ledStrip
                )
                dialog!!.listener = this
                dialog!!.display()
            }
            R.id.edit_schedules_button -> {
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
                ControllerManager.moveLEDStripOrder(from, to)
                adapter.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)

                if (actionState == ACTION_STATE_DRAG) {
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
    private val setColorClickListener: ButtonClickListener,
    private val parentFragmentManager: FragmentManager
)
    : RecyclerView.Adapter<LEDStripListAdapter.ViewHolder?>()
{

    var reorderMode = false

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    inner class ViewHolder(itemView: View)
        : RecyclerView.ViewHolder(itemView), View.OnLongClickListener,
        PopupMenu.OnMenuItemClickListener {
        init {
            itemView.setOnLongClickListener(this)
        }

        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        private val nameView: TextView = itemView.findViewById(R.id.led_strip_name)
        private val colorsButtonView: ImageView = itemView.findViewById(R.id.set_colors_button)
        private val colorButtonView: ImageView = itemView.findViewById(R.id.set_color_button)
        private val editSchedulesView: ImageView = itemView.findViewById(R.id.edit_schedules_button)
        private val offOnStateToggle: SwitchCompat = itemView.findViewById(R.id.on_off_switch)
        private val dragHandle: ImageView = itemView.findViewById(R.id.drag_handle)
        val brightnessBar: SeekBar = itemView.findViewById(R.id.brightness_bar)
        private var ledStrip: LEDStrip? = null


        fun setLEDStrip(ledStrip: LEDStrip) {
            this.ledStrip = ledStrip
            nameView.text = ledStrip.name
            offOnStateToggle.isChecked = ledStrip.onState
            offOnStateToggle.setOnCheckedChangeListener { _, isChecked ->
                if (offOnStateToggle.isPressed) {
                    ledStrip.setOnState(isChecked, true)
                    ledStrip.sendBrightnessPacket(true)
                    if (isChecked && ledStrip.brightness < 300
                        && (activity as MainActivity).getLuxVal() > 100
                    ) {
                        Toast.makeText(
                            activity,
                            "The LED is dim.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            dragHandle.visibility = if (reorderMode) {
                View.VISIBLE
            } else {
                View.GONE
            }
            brightnessBar.max = MAX_BRIGHTNESS
            brightnessBar.progress = ledStrip.getBrightnessExponential()
            brightnessBar.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seek: SeekBar,
                    newProgress: Int, fromUser: Boolean
                ) {
                    val currentProgress = ledStrip.getBrightnessExponential()
                    if (newProgress != currentProgress) {
                        ledStrip.setBrightnessExponential(newProgress, true)
                        if (ledStrip.brightness == 0) {
                            // It's low enough that it rounds down to 0
                            brightnessBar.progress = 0
                        }
                        ledStrip.sendBrightnessPacket(true)
                    }
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

        override fun onLongClick(view: View): Boolean {
            if (reorderMode)
                return true
            val popup = PopupMenu(itemView.context, itemView)
            val inflater = popup.menuInflater
            inflater.inflate(R.menu.led_strip_item_menu, popup.menu)
            popup.show()
            popup.setOnMenuItemClickListener(this);
            // Return true to indicate the click was handled
            return true
        }

        override fun onMenuItemClick(item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.calibrate -> {
                    Utils.replaceFragment(
                        ledStrip?.let { CalibrateLedStripFragment(it) },
                        parentFragmentManager
                    )
                    true
                }
                else -> false
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
            size += controller.ledStrips.size
        return size
    }

    fun getNthLEDStrip(index: Int) : LEDStrip? {
        if (index >= ControllerManager.ledStripOrders.size)
            ControllerManager.checkLedStripOrders()
        if (index >= ControllerManager.ledStripOrders.size)
            return null
        return ControllerManager.ledStripOrders[index]
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Get the data model based on position
        // Find the applicable controller
        val component: LEDStrip? = getNthLEDStrip(position)
            // Set item views based on your views and data model
        if (component != null)
            holder.setLEDStrip(component)

        /*holder.colorView.setBackgroundColor(android.graphics.Color.argb(255, color.red, color.green, color.blue))
        holder.driverView.text = component.driver.i2cAddress.toString()
        holder.pinView.text = component.driverPin.toString()*/
    }
}