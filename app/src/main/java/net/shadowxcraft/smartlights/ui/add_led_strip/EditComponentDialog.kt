package net.shadowxcraft.smartlights.ui.add_led_strip

import android.app.Activity
import android.app.AlertDialog
import android.view.View
import android.widget.*
import androidx.core.util.keyIterator
import androidx.core.util.valueIterator
import net.shadowxcraft.smartlights.BLEControllerManager
import net.shadowxcraft.smartlights.LEDStripComponent
import net.shadowxcraft.smartlights.R

class EditComponentDialog(private val fragment: LEDStripComponentFragment, context: Activity)
{
    private var dialog: AlertDialog? = null

    private var editedComponent: LEDStripComponent? = null
    private var pinSelector: Spinner? = null
    private var colorSelector: Spinner? = null
    private var driverSelector: Spinner? = null
    private var pinAdapter : ArrayAdapter<String>? = null
    private var driverValues : Array<String>? = null
    private val pinValues = ArrayList<String>()

    init {
        val builder = AlertDialog.Builder(context)
        val view = context.layoutInflater.inflate(R.layout.edit_led_strip_component, null)
        pinSelector = view.findViewById(R.id.led_strip_pin_selector)
        colorSelector = view.findViewById(R.id.color_picker_spinner)
        driverSelector = view.findViewById(R.id.driver_picker_spinner)


        pinAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            pinValues
        )
        pinSelector!!.adapter = pinAdapter

        val colorAdapter: ArrayAdapter<String> = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            fragment.colors.keys.toTypedArray()
        )
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        colorSelector!!.adapter = colorAdapter

        driverValues = Array(fragment.controller.pwmDriversByName.size) { "" }
        var i = 0;
        for (driverName in fragment.controller.pwmDriversByName.keys) {
            driverValues!![i++] = driverName
        }

        val driverAdapter: ArrayAdapter<String> = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            driverValues!!
        )
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        driverSelector!!.adapter = driverAdapter
        driverSelector!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                pinAdapter!!.clear()
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                addPinValues(position)
            }

        }


        dialog = builder.setView(view)
            // Add action buttons
            .setPositiveButton(R.string.finish_adding_led_strip_component, null)
            .create()
        // Override click listener to prevent it from closing when invalid.
        dialog!!.setOnShowListener {
            val button: Button =
                dialog!!.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                // First, validate the input
                var driver = fragment.controller.getPWMDriverByName(driverSelector!!.selectedItem.toString())
                val pin = driver!!.getAllPins()[pinSelector!!.selectedItem as String]
                val color = fragment.colors[colorSelector!!.selectedItem]

                when {
                    pin == null -> {
                        Toast.makeText(
                            BLEControllerManager.activity,
                            "Error getting pin.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    fragment.pinAlreadyInUse(pin, driver, editedComponent) -> {
                        Toast.makeText(
                            BLEControllerManager.activity,
                            "Port already in use.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {
                        if (editedComponent == null) {
                            fragment.components.add(LEDStripComponent(color!!, driver, pin))
                        } else {
                            editedComponent!!.color = color!!
                            editedComponent!!.driver = driver!!
                            editedComponent!!.driverPin = pin!!

                        }
                        fragment.adapter?.notifyDataSetChanged()

                        dialog!!.dismiss()
                    }
                }
            }
        }
        dialog!!.show()
    }
    constructor(fragment: LEDStripComponentFragment, context: Activity,
                componentToEdit: LEDStripComponent) : this(fragment, context)
    {
        editedComponent = componentToEdit
        val driverIndex = driverValues!!.indexOf(componentToEdit.driver.toString())
        driverSelector!!.setSelection(driverIndex, false)

        var selectedPinName = ""
        // Find the pin name that corresponds to the integer pin index
        for ((pinName, pinIndex) in componentToEdit.driver.getAllPins()) {
            if (pinIndex == componentToEdit.driverPin) {
                selectedPinName = pinName
                break
            }
        }

        addPinValues(driverIndex)

        pinSelector!!.setSelection(pinValues.indexOf(selectedPinName))

        //var selectedColorName = "Red"
        var selectedColorIndex = 0
        // Find the color name that corresponds to the color value
        for ((colorName, colorValue) in fragment.colors) {
            if (colorValue == componentToEdit.color) {
                //selectedColorName = colorName
                break
            }
            selectedColorIndex++;
        }

        colorSelector!!.setSelection(selectedColorIndex)
    }

    fun addPinValues(position: Int) {
        pinAdapter?.clear()
        fragment.controller.getPWMDriverByName(driverValues!![position])?.getAllPins()?.keys?.let {
            pinAdapter!!.addAll(
                it
            )
        }
    }
}