package net.shadowxcraft.smartlights

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.content.Context
import android.content.Intent
import java.util.*


const val REQUEST_ENABLE_BT = 50

object BLEControllerManager {
    private fun convertFromInteger(i: Int): UUID? {
        val msb = 0x0000000000001000L
        val lsb = -0x7fffff7fa064cb05L
        val value = (i and ((-0x1).toLong()).toInt()).toLong()
        return UUID(msb or (value shl 32), lsb)
    }

    val SERVICE_UUID = convertFromInteger(0x180D)
    val MEASUREMENT_CHAR_UUID = convertFromInteger(0x2A37)
    val CONTROL_POINT_CHAR_UUID = convertFromInteger(0x2A39)

    var bluetoothAdapter: BluetoothAdapter? = null
    var gatt: BluetoothGatt? = null

    fun init(activity: Activity) {
        val bluetoothManager: BluetoothManager =
            activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }

    fun supportsBluetooth(): Boolean {
        return bluetoothAdapter != null
    }

    fun getBLEScanner(): BluetoothLeScanner {
        return bluetoothAdapter!!.bluetoothLeScanner
    }

    fun connectTo(device: BluetoothDevice) {

    }
}