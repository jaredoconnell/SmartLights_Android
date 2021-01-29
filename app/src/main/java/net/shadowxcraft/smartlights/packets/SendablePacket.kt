package net.shadowxcraft.smartlights.packets

import android.bluetooth.BluetoothGattCharacteristic
import com.welie.blessed.WriteType
import net.shadowxcraft.smartlights.BLEControllerManager
import net.shadowxcraft.smartlights.Color
import net.shadowxcraft.smartlights.ESP32

abstract class SendablePacket(private val controller: ESP32, val packetID: Byte) {
    protected var writeCharacteristic: BluetoothGattCharacteristic? = null

    init {
        writeCharacteristic = controller.device!!.getCharacteristic(
            BLEControllerManager.SERVICE_UUID,
            BLEControllerManager.TO_ESP32_UUID
        )
    }

    fun queue() {
        controller.queuePacket(this)
    }

    abstract fun send();

    protected fun sendData(data: ByteArray) {
        writeCharacteristic?.let {
            controller.device!!.writeCharacteristic(
                it, data,
                WriteType.WITH_RESPONSE
            )
        }
        if (writeCharacteristic == null)
            controller.reconnect()
        else
            controller.checkConnection()
    }

    @ExperimentalUnsignedTypes
    protected fun longToByteList(long: ULong) : List<Byte> {
        var tmp = long
        val list = ArrayList<Byte>()
        for (i in 0..7) {
            list.add(0, (tmp % 256u).toByte())
            tmp /= 256u
        }
        return list
    }
    @ExperimentalUnsignedTypes
    protected fun intToByteList(int: UInt) : List<Byte> {
        var tmp = int
        val list = ArrayList<Byte>()
        for (i in 0..3) {
            list.add(0, (tmp % 256u).toByte())
            tmp /= 256u
        }
        return list
    }
    protected fun shortToByteList(short: Int) : List<Byte> {
        val list = ArrayList<Byte>()
        list.add(((short / 256).toByte()))
        list.add(((short % 256).toByte()))
        return list
    }

    protected fun strToByteList(string: String) : List<Byte> {
        val list = ArrayList<Byte>()
        list.add(string.length.toByte())
        list.addAll(string.toByteArray().toTypedArray())
        return list
    }

    protected fun colorToByteList(color: Color) : List<Byte> {
        val list = ArrayList<Byte>(3)
        list.add(color.red.toByte())
        list.add(color.green.toByte())
        list.add(color.blue.toByte())
        return list
    }
}