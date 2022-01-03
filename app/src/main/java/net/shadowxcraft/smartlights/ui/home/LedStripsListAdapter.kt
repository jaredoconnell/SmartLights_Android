package net.shadowxcraft.smartlights.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.view.*
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.welie.blessed.BluetoothPeripheral
import net.shadowxcraft.smartlights.*
import net.shadowxcraft.smartlights.ui.calibrate_led_strip.CalibrateLedStripFragment


// Create the basic adapter extending from RecyclerView.Adapter
// Note that we specify the custom ViewHolder which gives us access to our views
class LEDStripListAdapter(
    private val setColorClickListener: ButtonClickListener,
    private val parentFragmentManager: FragmentManager,
    private val isGroups: Boolean
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
        private val statusIndicator: ImageView = itemView.findViewById(R.id.connection_status)
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
                        && (BLEControllerManager.activity as MainActivity).getLuxVal() > 100
                    ) {
                        Toast.makeText(
                            BLEControllerManager.activity,
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

                @SuppressLint("NotifyDataSetChanged")
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    if (ledStrip is LEDStripGroup)
                        notifyDataSetChanged()
                }
            })
            colorsButtonView.setOnClickListener {
                setColorClickListener.onButtonClicked(absoluteAdapterPosition, R.id.set_colors_button)
            }
            colorButtonView.setOnClickListener {
                setColorClickListener.onButtonClicked(absoluteAdapterPosition, R.id.set_color_button)
            }
            editSchedulesView.setOnClickListener {
                setColorClickListener.onButtonClicked(absoluteAdapterPosition, R.id.edit_schedules_button)
            }
            colorButtonView.drawable.mutate().setTint(ledStrip.simpleColor.toArgb())

            statusIndicator.setOnClickListener {
                onStatusClick(it)
            }

            val device = ledStrip.controller.device
            if (ledStrip.controller.connecting) {
                statusIndicator.setColorFilter(
                    BLEControllerManager.activity!!.resources.getColor(
                        R.color.status_connecting,
                        null
                    )
                )
            } else if (device == null || device.state == BluetoothPeripheral.STATE_DISCONNECTED) {
                statusIndicator.setColorFilter(
                    BLEControllerManager.activity!!.resources.getColor(
                        R.color.status_offline,
                        null
                    )
                )
            } else if (device.state == BluetoothPeripheral.STATE_CONNECTED) {
                statusIndicator.setColorFilter(BLEControllerManager.activity!!.resources.getColor(R.color.status_online, null))
            } else {
                statusIndicator.setColorFilter(BLEControllerManager.activity!!.resources.getColor(R.color.status_connecting, null))
            }
        }

        private fun onStatusClick(view: View) {
            val inflater = LayoutInflater.from(itemView.context)
            val layoutView: View = inflater.inflate(R.layout.status_description_popup, null, false)

            // create the popup window
            val width = LinearLayout.LayoutParams.WRAP_CONTENT
            val height = LinearLayout.LayoutParams.WRAP_CONTENT
            val focusable = true // lets taps outside the popup also dismiss it

            val popupWindow = PopupWindow(layoutView, width, height, focusable)

            // show the popup window
            // which view you pass in doesn't matter, it is only used for the window tolken
            popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)

            // dismiss the popup window when touched

            // dismiss the popup window when touched
            layoutView.setOnClickListener { _ ->
                popupWindow.dismiss()
            }
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
        return OrderingManager.getLedStripPositions(isGroups).size
    }

    fun getNthLEDStrip(index: Int) : LEDStrip? {
        val ledList = OrderingManager.getLedStripPositions(isGroups)

        if (index >= ledList.size)
            OrderingManager.checkLedStripOrders(isGroups)
        if (index >= ledList.size)
            return null
        val uuid = ledList[index]
        return ControllerManager.getLEDStripByID(uuid)
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