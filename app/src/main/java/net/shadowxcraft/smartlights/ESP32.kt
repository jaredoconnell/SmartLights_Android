package net.shadowxcraft.smartlights

import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.util.Log
import android.util.SparseArray
import android.widget.EditText
import android.widget.Toast
import androidx.core.util.contains
import androidx.core.util.containsKey
import androidx.core.util.set
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.BluetoothPeripheralCallback
import net.shadowxcraft.smartlights.packets.*
import java.nio.charset.Charset
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

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
    fun addPWMDriver(pwmDriver: PWMDriver, sendPacket: Boolean) {
        //if(pwmDrivers.containsKey(pwmDriver.i2cAddress))
        //        throw IllegalStateException("Controller with same address already exists.")
        pwmDrivers.append(pwmDriver.i2cAddress, pwmDriver)
        Log.println(Log.INFO, "ESP32", "Adding PWM driver "
                + pwmDriver.i2cAddress.toString() + " Is still connected: " + BLEControllerManager.bluetoothCentral?.connectedPeripherals.toString())

        if (sendPacket)
            AddPWMDriverPacket(this, pwmDriver).send()
    }

    fun getPWMDriver(address: Int) : PWMDriver? {
        return if (pwmDrivers.containsKey(address)) {
            pwmDrivers[address]
        } else {
            null;
        }
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
    fun addLEDStrip(strip: LEDStrip, sendPacket: Boolean) {
        if (nextLEDStripID <= strip.id) {
            nextLEDStripID = strip.id + 1
        }
        if (ledStrips.containsKey(strip.id)) {
            // Maybe throw error in the future
        }
        ledStrips.put(strip.id, strip)

        if (sendPacket)
            AddLEDStripPacket(this, strip).send()
    }

    fun addColorSequence(colorSequence: ColorSequence, sendPacket: Boolean) {
        if (nextColorSequenceID <= colorSequence.id) {
            nextColorSequenceID = colorSequence.id + 1
        }
        //if (!colorsSequences.contains(colorSequence.id)) {
        colorsSequences[colorSequence.id] = colorSequence
        //}

        if (sendPacket)
            AddColorSequencePacket(this, colorSequence).send()
    }

    private fun checkName(listener: BLEControllerManager.BluetoothConnectionListener?) {
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

    fun onConnection() {
        // Makes sure proper communication is possible, then makes sure it's all up to date.
        device?.requestMtu(512)
        checkName(BLEControllerManager.externConnectionListener)

        val exec: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

        exec.schedule({
            // Delay in case the requested mtu has not applied.
            requestDataFromDriver()
        }, 2, TimeUnit.SECONDS)
    }

    private fun requestDataFromDriver() {
        // This is the order they should be sent to prevent issues with missing components.
        GetColorSequences(this).send();
        GetPWMDrivers(this).send();
        GetLEDStrips(this).send();
    }

    /*
     * Bluetooth events
     */

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

    @ExperimentalUnsignedTypes
    private fun getPacket(value: ByteArray) : ReceivedPacket {
        return when(value[0].toUByte().toInt()) {
            255 -> PWMDriversListResponse(this, value)
            254 -> LEDStripsListResponse(this, value)
            252 -> LEDStripColorSequenceListResponse(this, value)
            251 -> ColorSequenceListResponse(this, value)
            else -> throw IllegalStateException("Unknown packet ID ${value[0].toInt()}")
        }
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
        Log.println(Log.INFO, "ESP32", "onCharacteristicUpdate got packet!")
        val packet: ReceivedPacket? = value?.let { getPacket(it) }
        Log.println(Log.INFO, "ESP32", "Packet: " + packet?.javaClass?.name)
        packet?.process()
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
        Toast.makeText(
            BLEControllerManager.activity,
            "Bonding started",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Callback invoked when a bonding process has succeeded
     *
     * @param peripheral the peripheral
     */
    override fun onBondingSucceeded(peripheral: BluetoothPeripheral?) {
        Log.println(Log.INFO, "ESP32", "onBondingSucceeded")
        Toast.makeText(
            BLEControllerManager.activity,
            "Bonding succeeded",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Callback invoked when a bonding process has failed
     *
     * @param peripheral the peripheral
     */
    override fun onBondingFailed(peripheral: BluetoothPeripheral?) {
        Log.println(Log.INFO, "ESP32", "onBondingFailed")
        Toast.makeText(
            BLEControllerManager.activity,
            "Bonding failed",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Callback invoked when a bond has been lost and the peripheral is not bonded anymore.
     *
     * @param peripheral the peripheral
     */
    override fun onBondLost(peripheral: BluetoothPeripheral?) {
        Log.println(Log.INFO, "ESP32", "onBondLost")
        Toast.makeText(
            BLEControllerManager.activity,
            "Bond lost",
            Toast.LENGTH_SHORT
        ).show()
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