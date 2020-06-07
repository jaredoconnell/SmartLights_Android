package net.shadowxcraft.smartlights

import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.DialogInterface
import android.util.SparseArray
import android.widget.EditText
import android.widget.Toast
import androidx.core.util.containsKey
import java.lang.IllegalStateException

const val DEFAULT_NAME: String = "New Device"

class ESP32 {
    // Bluetooth stuff
    var device: BluetoothDevice? = null
    var bluetoothConnection: BluetoothGatt? = null;
    // Other stuff
    var name: String = DEFAULT_NAME
    private val pwmControllers: SparseArray<PWMController> = SparseArray<PWMController>()

    /**
     * Adds a controller.
     * @throws IllegalStateException if a controller with the same address is there.
     */
    fun addPWMController(controller: PWMController) {
        if(pwmControllers.containsKey(controller.i2cAddress))
                throw IllegalStateException("Controller with same address already exists.")
        pwmControllers.append(controller.i2cAddress, controller)
    }

    fun checkName(act: Activity, listener: BLEControllerManager.BluetoothConnectionListener?) {
        if (name == DEFAULT_NAME) {
            val builder = AlertDialog.Builder(act)
            val inflater = act.layoutInflater;
            val view = inflater.inflate(R.layout.bluetooth_connected_layout, null)
            builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.set) { _, _ ->
                    this.name = view.findViewById<EditText>(R.id.controllerName).text.toString()
                    listener?.onControllerChange(this)
                    Toast.makeText(
                        BLEControllerManager.activity,
                        "Saved to RAM",
                        Toast.LENGTH_SHORT
                    ).show()
                }.show()

        }
    }
}