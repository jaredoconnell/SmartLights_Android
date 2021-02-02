package net.shadowxcraft.smartlights

import android.app.AlertDialog
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.ContentValues
import android.content.Context
import android.util.Log
import android.util.SparseArray
import android.widget.EditText
import android.widget.Toast
import androidx.core.util.isNotEmpty
import androidx.core.util.set
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.BluetoothPeripheralCallback
import com.welie.blessed.GattStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.shadowxcraft.smartlights.packets.*
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

const val DEFAULT_NAME: String = "LEDs"

class ESP32(private val act: MainActivity, val addr: String, var name: String)
    : BluetoothPeripheralCallback(), PinDriver
{
    var dbId = -1 // -1 means none yet
    // Bluetooth stuff
    var device: BluetoothPeripheral? = null
    // Other stuff
    private val pwmDriversByAddress: TreeMap<Int, PinDriver> = TreeMap()
    val pwmDriversByName: TreeMap<String, PinDriver> = TreeMap()
    val ledStrips: TreeMap<String, LEDStrip> = TreeMap()
    val ledStripGroups: TreeMap<String, LEDStripGroup> = TreeMap()
    val colorsSequences: TreeMap<String, ColorSequence> = TreeMap()
    val queuedPackets: SparseArray<SendablePacket> = SparseArray()
    private var pins: TreeMap<String, Int> = TreeMap()

    private val queuedPacketsTimer = Timer()

    init {
        pins["16-RX2"] = 16
        pins["17-TX2"] = 17
        pins["18-D18"] = 18
        pins["19-D19"] = 19
        pins["21-D21"] = 21
        pins["22-D22"] = 22
        pins["23-D23"] = 23
        pins["25-D25"] = 25
        pins["26-D26"] = 26
        pins["27-D27"] = 27
        pins["32-D32"] = 32
        pins["33-D33"] = 33

        pwmDriversByName[this.toString()] = this
        pwmDriversByAddress[this.getAddress()] = this

        queuedPacketsTimer.schedule(object : TimerTask() {
            override fun run() {
                if (queuedPackets.isNotEmpty()) {
                    val packet = queuedPackets.valueAt(0)
                    queuedPackets.removeAt(0)
                    packet.send()
                }
            }
        }, 200, 100)
    }

    fun checkConnection() {
        if (device == null || device?.state == BluetoothPeripheral.STATE_DISCONNECTED) {
            reconnect()
        }
    }

    fun reconnect() {
        if (device == null)
            BLEControllerManager.attemptToConnectToAddr(addr)
        else
            device?.let { BLEControllerManager.connectTo(it) }
    }

    /**
     * Adds a controller.
     * @throws IllegalStateException if a controller with the same address is there.
     */
    fun addPWMDriver(pwmDriver: PWMDriver, sendPacket: Boolean) {
        //if(pwmDrivers.containsKey(pwmDriver.i2cAddress))
        //        throw IllegalStateException("Controller with same address already exists.")
        pwmDriversByName[pwmDriver.toString()] = pwmDriver
        pwmDriversByAddress[pwmDriver.i2cAddress] = pwmDriver
        Log.println(Log.INFO, "ESP32", "Adding PWM driver "
                + pwmDriver.i2cAddress.toString() + " Is still connected: " + BLEControllerManager.bluetoothCentral?.connectedPeripherals.toString())

        if (sendPacket)
            AddPWMDriverPacket(this, pwmDriver).send()
    }

    fun getPWMDriverByName(name: String) : PinDriver? {
        return when {
            pwmDriversByName.containsKey(name) -> {
                pwmDriversByName[name]
            }
            else -> {
                null;
            }
        }
    }
    fun getPWMDriverByAddress(address: Int) : PinDriver? {
        return when {
            pwmDriversByAddress.containsKey(address) -> {
                pwmDriversByAddress[address]
            }
            else -> {
                null;
            }
        }
    }

    override fun getAllPins() : Map<String, Int> {
        return pins;
    }

    override fun getAddress(): Int {
        return 0
    }

    override fun toString(): String {
        return "ESP32"
    }

    /**
     * Stores the LED strip, then sends the LED strip to the controller.
     */
    fun addLEDStrip(strip: LEDStrip, sendPacket: Boolean) {
        if (ledStrips.containsKey(strip.id)) {
            // Maybe throw error in the future
            Log.println(Log.WARN, "ESP32", "Already contains LED Strip")
        }
        ledStrips[strip.id] = strip

        Log.println(Log.INFO, "ESP32", "About to notify")

        GlobalScope.launch {
            withContext(Dispatchers.Main) {
                Log.println(Log.INFO, "ESP32", "Notified")
                if (SharedData.ledStripsFragment == null)
                    Log.println(Log.INFO, "ESP32", "Could not notify. Fragment null.")
                SharedData.ledStripsFragment?.adapter?.notifyDataSetChanged()
            }
        }

        if (sendPacket)
            AddLEDStripPacket(this, strip).send()
    }

     /**
     * Stores the LED strip, then sends the LED strip to the controller.
     */
    fun addLEDStripGroup(group: LEDStripGroup, sendPacket: Boolean) {
        if (ledStrips.containsKey(group.id)) {
            // Maybe throw error in the future
            Log.println(Log.WARN, "ESP32", "Already contains LED Strip")
        }
        ledStripGroups[group.id] = group
         GlobalScope.launch {
             withContext(Dispatchers.Main) {
                 SharedData.ledStripGroupsFragment?.adapter?.notifyDataSetChanged()
             }
         }
    }

    fun addColorSequence(colorSequence: ColorSequence, sendPacket: Boolean) {
        //if (!colorsSequences.contains(colorSequence.id)) {
        colorsSequences[colorSequence.id] = colorSequence
        //}

        if (sendPacket)
            AddColorSequencePacket(this, colorSequence).send()
    }

    private fun checkName(listener: BLEControllerManager.BluetoothConnectionListener?) {
        if (name.trim() == DEFAULT_NAME) {
            renameController(listener);
        }
    }

    fun renameController(listener: BLEControllerManager.BluetoothConnectionListener?) {
        val builder = AlertDialog.Builder(act)
        val inflater = act.layoutInflater;
        val view = inflater.inflate(R.layout.bluetooth_rename_layout, null)
        builder.setView(view)
            // Add action buttons
            .setPositiveButton(R.string.set) { _, _ ->
                this.name = view.findViewById<EditText>(R.id.controllerName).text.toString().trim()
                if (this.name.isEmpty())
                    this.name = DEFAULT_NAME
                else {
                    SetSettingPacket(this,
                        ControllerSetting("name", this.name)).send()
                    listener?.onControllerChange(this)

                    saveToDB(BLEControllerManager.activity!!)
                    Toast.makeText(
                        BLEControllerManager.activity,
                        "Saved to DB",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.show()
    }

    fun onConnection() {
        // Makes sure proper communication is possible, then makes sure it's all up to date.
        device?.requestMtu(512)
        if (device != null) {
            name = device!!.name!!
        }
        Toast.makeText(BLEControllerManager.activity,
            "Connected.",
            Toast.LENGTH_SHORT
        ).show()

        val exec: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

        exec.schedule({
            // Delay in case the requested mtu has not applied.
            SendTimePacket(this).send()
            requestDataFromDriver()
            checkName(BLEControllerManager.externConnectionListener)
        }, 2, TimeUnit.SECONDS)
    }

    private fun requestDataFromDriver() {
        // This is the order they should be sent to prevent issues with missing components.
        GetColorSequences(this).send();
        GetPWMDrivers(this).send();
        GetLEDStrips(this).send();
        GetLEDStripGroups(this).send();
        GetScheduledSequences(this).send()
    }

    fun saveToDB(context: Context) {
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                val database = DBHelper(context).writableDatabase
                val values = ContentValues()
                values.put(SQLTableData.ControllerEntry.COLUMN_NAME_BLE_ADDR, device!!.address)
                values.put(SQLTableData.ControllerEntry.COLUMN_NAME_NAME, name)
                dbId = database.replace(SQLTableData.ControllerEntry.TABLE_NAME, null, values).toInt()
            }
        }
    }

    /*private suspend fun saveToDBSuspend(context: Context) {

    }*/

    /**
     * Used for queueing a packet that should be ignored if a second of that type shows up.
     * For example, when the user has a seek bar, and they drag their finger across it,
     * only the latest packets should be sent.
     */
    fun queuePacket(packet: SendablePacket) {
        queuedPackets[packet.packetID.toInt()] = packet
    }

    fun clearQueueForPacketID(id: Int) {
        queuedPackets.remove(id)
    }

    /*
     * Bluetooth events
     */

    override fun onServicesDiscovered(peripheral: BluetoothPeripheral) {
        this.device = peripheral
        Log.println(Log.INFO, "ESP32", "onServicesDiscovered")
        val readCharacteristic = peripheral.getCharacteristic(
            BLEControllerManager.SERVICE_UUID,
            BLEControllerManager.TO_PHONE_UUID
        )
        if (readCharacteristic != null) {
            peripheral.setNotify(readCharacteristic, true)
        } else {
            Toast.makeText(
                BLEControllerManager.activity,
                "Characteristic is null",
                Toast.LENGTH_SHORT
            ).show()
        }
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
        peripheral: BluetoothPeripheral,
        characteristic: BluetoothGattCharacteristic,
        status: GattStatus
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
            250 -> ScheduledChangeListResponse(this, value)
            245 -> LEDStripGroupsListResponse(this, value)
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
        peripheral: BluetoothPeripheral,
        value: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        status: GattStatus
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
        peripheral: BluetoothPeripheral,
        value: ByteArray,
        descriptor: BluetoothGattDescriptor,
        status: GattStatus
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
        peripheral: BluetoothPeripheral,
        value: ByteArray,
        descriptor: BluetoothGattDescriptor,
        status: GattStatus
    ) {
        Log.println(Log.INFO, "ESP32", "onDescriptorWrite")
    }

    /**
     * Callback invoked when a bonding process is started
     *
     * @param peripheral the peripheral
     */
    override fun onBondingStarted(peripheral: BluetoothPeripheral) {
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
    override fun onBondingSucceeded(peripheral: BluetoothPeripheral) {
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
    override fun onBondingFailed(peripheral: BluetoothPeripheral) {
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
    override fun onBondLost(peripheral: BluetoothPeripheral) {
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
        peripheral: BluetoothPeripheral,
        rssi: Int,
        status: GattStatus
    ) {
        Log.println(Log.INFO, "ESP32", "onReadRemoteRssi")
    }

    /**
     * Callback invoked as the result of a MTU request operation
     * @param peripheral the peripheral
     * @param mtu the new MTU
     * @param status GATT status code
     */
    override fun onMtuChanged(peripheral: BluetoothPeripheral, mtu: Int, status: GattStatus) {
        Log.println(Log.INFO, "ESP32", "onMtuChanged")
    }

}