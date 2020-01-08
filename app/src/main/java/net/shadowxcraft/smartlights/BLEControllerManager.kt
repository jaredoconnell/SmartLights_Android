package net.shadowxcraft.smartlights

import android.app.Activity
import android.bluetooth.*
import android.bluetooth.BluetoothDevice.*
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.le.BluetoothLeScanner
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.util.Log
import android.widget.Toast
import sun.audio.AudioDevice.device
import java.util.*


const val REQUEST_ENABLE_BT = 50

object BLEControllerManager {
    val connected: HashMap<BluetoothDevice, BluetoothGatt> = HashMap()
    var bluetoothAdapter: BluetoothAdapter? = null
    var activity: Activity? = null
    var discoverServicesRunnable: Runnable? = null

    private fun convertFromInteger(i: Int): UUID? {
        val msb = 0x0000000000001000L
        val lsb = -0x7fffff7fa064cb05L
        val value = (i and ((-0x1).toLong()).toInt()).toLong()
        return UUID(msb or (value shl 32), lsb)
    }

    val SERVICE_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    val MEASUREMENT_CHAR_UUID = SERVICE_UUID//convertFromInteger(0x2A37)
    val CONTROL_POINT_CHAR_UUID = SERVICE_UUID//convertFromInteger(0x2A39)
    val CLIENT_CHARACTERISTIC_CONFIG_UUID = SERVICE_UUID//convertFromInteger(0x2902)

    fun init(activity: Activity) {
        this.activity = activity
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

    fun connectTo(device: BluetoothDevice): Boolean {
        if(connected.contains(device)) {
            connected[device]?.connect()
            return false
        } else {
            val gattCallback = object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    Log.println(Log.INFO, "BluetoothGattCallback", "State changed to $newState")
                    if(status == GATT_SUCCESS) {
                        when (newState) {
                            BluetoothProfile.STATE_CONNECTED -> {
                                // We successfully connected, proceed with service discovery
                                activity?.runOnUiThread {
                                    Toast.makeText(
                                        activity,
                                        "Connected!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                val bondstate: Int = device.bondState
                                var discover = true
                                var delay = 0L
                                when (bondstate) {
                                    BOND_BONDING -> {
                                        discover = false // not ready yet
                                    }
                                    BOND_BONDED -> {
                                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                                            delay = 1000 // Delay for Android 7 and older.
                                        }
                                        activity?.runOnUiThread {
                                            Toast.makeText(activity,"Bonded!",
                                                Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                if(discover) {
                                    if(delay <= 0)
                                        gatt.discoverServices()
                                    else {
                                        discoverServicesRunnable = Runnable {
                                            val result = gatt.discoverServices()
                                            if (!result) {
                                                Log.e("BLEControllerManager","discoverServices failed to start")
                                            }
                                            discoverServicesRunnable = null
                                        }
                                        val handler = Handler()
                                        handler.postDelayed(discoverServicesRunnable, delay)
                                    }
                                }
                            }
                            BluetoothProfile.STATE_DISCONNECTED -> {
                                // We successfully disconnected on our own request
                                gatt.close()
                            }
                            BluetoothProfile.STATE_CONNECTING -> {
                                activity?.runOnUiThread {
                                    Toast.makeText(
                                        activity,
                                        "Connecting..",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            else -> {
                                // We're DISCONNECTING, ignore for now
                            }
                        }
                    } else {
                        // An error happened...figure out what happened!
                        gatt.close()
                    }
                }
                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int){
                    Log.println(Log.INFO, "BluetoothGattCallback", "Services discovered")
                    val characteristic: BluetoothGattCharacteristic =
                        gatt.getService(SERVICE_UUID)
                            .getCharacteristic(MEASUREMENT_CHAR_UUID)

                    val descriptor =
                    characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)

                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
                    gatt.writeDescriptor(descriptor)
                }

                override fun onDescriptorWrite(
                    gatt: BluetoothGatt,
                    descriptor: BluetoothGattDescriptor?,
                    status: Int
                ) {
                    Log.println(Log.INFO, "BluetoothGattCallback", "Descriptor Write")
                    val characteristic =
                        gatt.getService(SERVICE_UUID)
                            .getCharacteristic(CONTROL_POINT_CHAR_UUID)
                    characteristic.value = "010".toByteArray()
                    gatt.writeCharacteristic(characteristic)
                }

                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt?,
                    characteristic: BluetoothGattCharacteristic
                ) {
                    Log.println(Log.INFO, "BluetoothGattCallback", "Characteristic Changed")
                    processData(characteristic.value)
                }
            }
            val gatt = device.connectGatt(activity, false, gattCallback, TRANSPORT_LE)
            connected[device] = gatt
            return true
        }
    }

    fun processData(byteArray: ByteArray) {
        println("Got data: $byteArray")
    }
}