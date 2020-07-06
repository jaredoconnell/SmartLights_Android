package net.shadowxcraft.smartlights

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanResult
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.welie.blessed.BluetoothCentral
import com.welie.blessed.BluetoothCentralCallback
import com.welie.blessed.BluetoothPeripheral
import java.util.*


const val REQUEST_ENABLE_BT = 50

object BLEControllerManager : BluetoothCentralCallback() {

    var bluetoothCentral: BluetoothCentral? = null
    // BluetoothDevice represents the device that can be connected
    // BluetoothGatt represents the actual connection.
    private val connected: HashMap<String, ESP32> = HashMap()
    private var bluetoothAdapter: BluetoothAdapter? = null
    var activity: Activity? = null
    var discoverServicesRunnable: Runnable? = null
    var externConnectionListener: BluetoothConnectionListener? = null
    var externScanListener: BluetoothScanListener? = null

    private fun convertFromInteger(i: Int): UUID? {
        val msb = 0x0000000000001000L
        val lsb = -0x7fffff7fa064cb05L
        val value = (i and ((-0x1).toLong()).toInt()).toLong()
        return UUID(msb or (value shl 32), lsb)
    }

    val SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    val TO_ESP32_UUID: UUID = UUID.fromString("6eed7e34-9f2a-4f0f-b1d6-70cd04e8e581")
    val TO_PHONE_UUID: UUID = UUID.fromString("1cf8d309-11a3-46fb-9378-9afff7dce3b4")

    fun init(activity: Activity) {
        this.activity = activity
        bluetoothCentral = BluetoothCentral(activity, this, Handler(Looper.getMainLooper()))
        val bluetoothManager: BluetoothManager =
            activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }

    fun supportsBluetooth(): Boolean {
        return bluetoothAdapter != null
    }

    fun startScan() {
        bluetoothCentral!!.scanForPeripheralsWithServices(arrayOf(SERVICE_UUID))
        //bluetoothCentral?.scanForPeripherals()
    }

    fun endScan() {
        bluetoothCentral?.stopScan()
    }

    fun attemptToConnectToAddr(addr: String) {
        val peripheral = bluetoothCentral!!.getPeripheral(addr)
        connectTo(peripheral)
    }

    fun connectTo(device: BluetoothPeripheral): Boolean {
        if(device.address in connected) {
            val controller = connected[device.address]
            bluetoothCentral!!.connectPeripheral(controller!!.device, controller)
            if (device.bondState == BluetoothPeripheral.BOND_NONE)
                device.createBond()
            return false
        } else {
            val newController = ESP32(this.activity!!)
            newController.device = device
            connected[device.address] = newController
            bluetoothCentral!!.connectPeripheral(newController.device, newController)
            device.createBond()
            activity?.runOnUiThread {
                Toast.makeText(
                    activity,
                    "Connecting...",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return true
        }
    }

    fun processData(byteArray: ByteArray) {
        println("Got data: $byteArray")
    }

    override fun onScanFailed(errorCode: Int) {
        this.externScanListener?.onScanFailed()
    }

    override fun onDiscoveredPeripheral(
        peripheral: BluetoothPeripheral?,
        scanResult: ScanResult?
    ) {
        this.externScanListener?.onPeripheralDiscovered(peripheral!!, scanResult!!)
    }

    override fun onConnectedPeripheral(peripheral: BluetoothPeripheral) {
        activity?.runOnUiThread {
            Toast.makeText(
                activity,
                "Connected!",
                Toast.LENGTH_SHORT
            ).show()
        }
        var controller = connected[peripheral.address]
        if (controller != null && !ControllerManager.controllers.contains(controller)) {
            ControllerManager.controllers.add(controller)
            externConnectionListener?.onControllerChange(controller)
            controller.saveToDB(activity!!)
        }
        controller!!.onConnection()
    }

    override fun onConnectionFailed(peripheral: BluetoothPeripheral?, status: Int) {
        activity?.runOnUiThread {
            Toast.makeText(
                activity,
                "Bluetooth Connection Failed.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDisconnectedPeripheral(peripheral: BluetoothPeripheral?, status: Int) {
        activity?.runOnUiThread {
            Toast.makeText(
                activity,
                "Disconnected. Trying to reconnect...",
                Toast.LENGTH_SHORT
            ).show()
        }
        if (peripheral != null) {
            val controller = connected[peripheral.address]
            bluetoothCentral!!.connectPeripheral(peripheral, controller)
        }

    }

    fun setConnectionListener(listener: BluetoothConnectionListener) {
        this.externConnectionListener = listener
    }

    fun setScanListener(listener: BluetoothScanListener) {
        this.externScanListener = listener
    }

    interface BluetoothConnectionListener {
        fun onControllerChange(device: ESP32)
    }

    interface BluetoothScanListener {
        fun onScanFailed()
        fun onPeripheralDiscovered(peripheral: BluetoothPeripheral, status: ScanResult)
    }
}