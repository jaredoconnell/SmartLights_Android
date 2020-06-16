package net.shadowxcraft.smartlights

import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.util.Log
import android.util.SparseArray
import android.widget.EditText
import android.widget.Toast
import androidx.core.util.containsKey
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.BluetoothPeripheralCallback
import net.shadowxcraft.smartlights.packets.AddLEDStripDriverPacket
import net.shadowxcraft.smartlights.packets.AddPWMDriverPacket
import java.nio.charset.Charset

const val DEFAULT_NAME: String = "New Device"

class ESP32(private val act: Activity) : BluetoothPeripheralCallback() {
    // Bluetooth stuff
    var device: BluetoothPeripheral? = null
    // Other stuff
    var name: String = DEFAULT_NAME
    val pwmDrivers: SparseArray<PWMDriver> = SparseArray<PWMDriver>()
    val ledStrips: SparseArray<LEDStrip> = SparseArray<LEDStrip>()
    val colorsSequences: SparseArray<ColorSequence> = SparseArray<ColorSequence>()
    private var nextLEDStripID = 0
    private var nextColorSequenceID = 0

    /**
     * Adds a controller.
     * @throws IllegalStateException if a controller with the same address is there.
     */
    fun addPWMDriver(pwmDriver: PWMDriver) {
        //if(pwmDrivers.containsKey(pwmDriver.i2cAddress))
        //        throw IllegalStateException("Controller with same address already exists.")
        pwmDrivers.append(pwmDriver.i2cAddress, pwmDriver)
        Log.println(Log.INFO, "ESP32", "Adding PWM driver "
                + pwmDriver.i2cAddress.toString() + " Is still connected: " + BLEControllerManager.bluetoothCentral?.connectedPeripherals.toString())
        AddPWMDriverPacket(this, pwmDriver).send()
    }

    fun getNextLEDStripID() : Int {
        // TODO: Do a more intelligent search for the next ID
        return nextLEDStripID++
    }

    fun getNextColorStripID() : Int {
        // TODO: Do a more intelligent search for the next ID
        return nextColorSequenceID++
    }

    /**
     * Stores the LED strip, then sends the LED strip to the controller.
     */
    fun addLEDStrip(strip: LEDStrip) {
        if (nextLEDStripID < strip.id) {
            nextLEDStripID = strip.id + 1
        }
        if (ledStrips.containsKey(strip.id)) {
            // Maybe throw error in the future
        }
        ledStrips.put(strip.id, strip)

        AddLEDStripDriverPacket(this, strip).send()
    }

    fun checkName(listener: BLEControllerManager.BluetoothConnectionListener?) {
        if (name == DEFAULT_NAME) {
            val builder = AlertDialog.Builder(act)
            val inflater = act.layoutInflater;
            val view = inflater.inflate(R.layout.bluetooth_connected_layout, null)
            builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.set) { _, _ ->
                    this.name = view.findViewById<EditText>(R.id.controllerName).text.toString().trim()
                    if (this.name.isEmpty())
                        this.name = DEFAULT_NAME

                    listener?.onControllerChange(this)
                    Toast.makeText(
                        BLEControllerManager.activity,
                        "Saved to RAM",
                        Toast.LENGTH_SHORT
                    ).show()
                }.show()
        }
    }

    override fun onCharacteristicWrite(
        peripheral: BluetoothPeripheral?,
        value: ByteArray?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        // TODO: Validate that it sent properly.


    }

    override fun onServicesDiscovered(peripheral: BluetoothPeripheral?) {
        this.device = peripheral
        Log.println(Log.INFO, "ESP32", "onServicesDiscovered")
        val readCharacteristic = peripheral!!.getCharacteristic(
            BLEControllerManager.SERVICE_UUID,
            BLEControllerManager.TO_PHONE_UUID
        )
        peripheral.setNotify(readCharacteristic, true)
        /*peripheral.readCharacteristic(readCharacteristic)
        val writeCharacteristic = peripheral.getCharacteristic(
            BLEControllerManager.SERVICE_UUID,
            BLEControllerManager.TO_ESP32_UUID
        )*/
    }

    /**
     * Callback invoked when the notification state of a characteristic has changed.
     *
     *
     * Use [BluetoothPeripheral.isNotifying] to get the current notification state of the characteristic
     *
     * @param peripheral the peripheral
     * @param characteristic the characteristic for which the notification state changed
     * @param status GATT status code
     */
    override fun onNotificationStateUpdate(
        peripheral: BluetoothPeripheral?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        Log.println(Log.INFO, "ESP32", "onNotificationStateUpdate")
        device!!.readCharacteristic(characteristic)
    }

    /**
     * Callback invoked as the result of a characteristic read operation or notification
     *
     *
     * The value byte array is a threadsafe copy of the byte array contained in the characteristic.
     *
     * @param peripheral the peripheral
     * @param value the new value received
     * @param characteristic the characteristic for which the new value was received
     * @param status GATT status code
     */
    override fun onCharacteristicUpdate(
        peripheral: BluetoothPeripheral?,
        value: ByteArray?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        val dataAsStr = String(value!!, Charset.defaultCharset())
        Log.println(Log.INFO, "ESP32", "onCharacteristicUpdate $dataAsStr " + value.size)
        /*Toast.makeText(
            BLEControllerManager.activity,
            "Got data " + dataAsStr,
            Toast.LENGTH_SHORT
        ).show()*/
    }

    /**
     * Callback invoked as the result of a descriptor read operation
     *
     * @param peripheral the peripheral
     * @param value the read value
     * @param descriptor the descriptor that was read
     * @param status GATT status code
     */
    override fun onDescriptorRead(
        peripheral: BluetoothPeripheral?,
        value: ByteArray?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        Log.println(Log.INFO, "ESP32", "onDescriptorRead")
    }

    /**
     * Callback invoked as the result of a descriptor write operation.
     * This callback is not called for the Client Characteristic Configuration descriptor. Instead the [BluetoothPeripheralCallback.onNotificationStateUpdate] will be called
     *
     * @param peripheral the peripheral
     * @param value the value that to be written
     * @param descriptor the descriptor written to
     * @param status the GATT status code
     */
    override fun onDescriptorWrite(
        peripheral: BluetoothPeripheral?,
        value: ByteArray?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        Log.println(Log.INFO, "ESP32", "onDescriptorWrite")
    }

    /**
     * Callback invoked when a bonding process is started
     *
     * @param peripheral the peripheral
     */
    override fun onBondingStarted(peripheral: BluetoothPeripheral?) {
        Log.println(Log.INFO, "ESP32", "onBondingStarted")
    }

    /**
     * Callback invoked when a bonding process has succeeded
     *
     * @param peripheral the peripheral
     */
    override fun onBondingSucceeded(peripheral: BluetoothPeripheral?) {
        Log.println(Log.INFO, "ESP32", "onBondingSucceeded")
    }

    /**
     * Callback invoked when a bonding process has failed
     *
     * @param peripheral the peripheral
     */
    override fun onBondingFailed(peripheral: BluetoothPeripheral?) {
        Log.println(Log.INFO, "ESP32", "onBondingFailed")
    }

    /**
     * Callback invoked when a bond has been lost and the peripheral is not bonded anymore.
     *
     * @param peripheral the peripheral
     */
    override fun onBondLost(peripheral: BluetoothPeripheral?) {
        Log.println(Log.INFO, "ESP32", "onBondLost")
    }

    /**
     * Callback invoked as the result of a read RSSI operation
     *
     * @param peripheral the peripheral
     * @param rssi the RSSI value
     * @param status GATT status code
     */
    override fun onReadRemoteRssi(
        peripheral: BluetoothPeripheral?,
        rssi: Int,
        status: Int
    ) {
        Log.println(Log.INFO, "ESP32", "onReadRemoteRssi")
    }

    /**
     * Callback invoked as the result of a MTU request operation
     * @param peripheral the peripheral
     * @param mtu the new MTU
     * @param status GATT status code
     */
    override fun onMtuChanged(peripheral: BluetoothPeripheral?, mtu: Int, status: Int) {
        Log.println(Log.INFO, "ESP32", "onMtuChanged")
    }

}