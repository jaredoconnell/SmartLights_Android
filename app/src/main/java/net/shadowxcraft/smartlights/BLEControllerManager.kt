package net.shadowxcraft.smartlights

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.welie.blessed.*
import java.util.*


const val REQUEST_ENABLE_BT = 50

object BLEControllerManager : BluetoothCentralManagerCallback() {

    var bluetoothCentral: BluetoothCentralManager? = null
    // BluetoothDevice represents the device that can be connected
    // BluetoothGatt represents the actual connection.
    private val connected: HashMap<String, ESP32> = HashMap()
    private var bluetoothAdapter: BluetoothAdapter? = null
    var activity: MainActivity? = null
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

    fun init(activity: MainActivity) {
        this.activity = activity
        bluetoothCentral = BluetoothCentralManager(activity, this, Handler(Looper.getMainLooper()))
        val bluetoothManager: BluetoothManager =
            activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        if (!hasBluetoothPerms()) {
            Toast.makeText(activity, "Bluetooth Permission Required", Toast.LENGTH_SHORT).show()
            return
        }
        activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }

    fun hasBluetoothPerms() : Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(
                activity!!,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
        if (activity == null)
            return false
        return ActivityCompat.checkSelfPermission(
            activity!!,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
            activity!!,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
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
            // Already has
            val controller = connected[device.address]
            bluetoothCentral!!.connectPeripheral(controller!!.device!!, controller)
            if (device.bondState == BondState.NONE)
                device.createBond()
            return false
        } else {
            val controller = if (ControllerManager.controllerAddrMap.containsKey(device.address)) {
                // Never connected, but in the list
                ControllerManager.controllerAddrMap[device.address]!!
            } else {
                // Never connected, not in the list
                ESP32(activity!!, device.address, device.name ?: DEFAULT_NAME)
            }

            controller.device = device
            connected[device.address] = controller
            bluetoothCentral!!.connectPeripheral(controller.device!!, controller)
            device.createBond()
            return true
        }
    }

    override fun onScanFailed(scanFailure: ScanFailure) {
        this.externScanListener?.onScanFailed()
    }

    override fun onDiscoveredPeripheral(
        peripheral: BluetoothPeripheral,
        scanResult: ScanResult
    ) {
        this.externScanListener?.onPeripheralDiscovered(peripheral, scanResult)
    }

    override fun onConnectedPeripheral(peripheral: BluetoothPeripheral) {
        val controller = connected[peripheral.address]
        controller?.connecting = false
        if (controller != null && !ControllerManager.controllerAddrMap.containsKey(controller.addr)) {
            ControllerManager.addController(controller)
            externConnectionListener?.onControllerChange(controller)
            controller.saveToDB(activity!!)
        }
        controller!!.onConnection()
    }

    override fun onConnectionFailed(peripheral: BluetoothPeripheral, status: HciStatus) {
        activity?.runOnUiThread {
            Toast.makeText(
                activity,
                "Bluetooth Connection Failed: $status",
                Toast.LENGTH_SHORT
            ).show()
            if (activity != null) {
                // To display connecting status
                val controller = connected[peripheral.address]
                controller?.connecting
                SharedData.notifyDataChanged(activity!!)
            }
        }
    }

    override fun onDisconnectedPeripheral(peripheral: BluetoothPeripheral, status: HciStatus) {
        val controller = connected[peripheral.address]
        controller?.onDisconnect()
        bluetoothCentral!!.connectPeripheral(peripheral, controller!!)
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