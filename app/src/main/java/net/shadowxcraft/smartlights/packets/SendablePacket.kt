package net.shadowxcraft.smartlights.packets

import android.bluetooth.BluetoothGattCharacteristic
import net.shadowxcraft.smartlights.BLEControllerManager
import net.shadowxcraft.smartlights.Color
import net.shadowxcraft.smartlights.ESP32

abstract class SendablePacket(private val controller: ESP32, val packetID: Int) {
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
        controller.device!!.writeCharacteristic(
            writeCharacteristic, data,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
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