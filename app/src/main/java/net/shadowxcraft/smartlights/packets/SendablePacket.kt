package net.shadowxcraft.smartlights.packets

import android.util.Log
import androidx.core.util.set
import com.welie.blessed.WriteType
import net.shadowxcraft.smartlights.BLEControllerManager
import net.shadowxcraft.smartlights.Color
import net.shadowxcraft.smartlights.ESP32

abstract class SendablePacket(private val controller: ESP32, val packetID: Byte) {
    val packetIndexID: Int = controller.nextPacketIndex++
    var sendTime = 0L

    fun queue() {
        controller.queuePacket(this)
    }

    protected fun getHeader(): ArrayList<Byte> {
        val output = ArrayList<Byte>();
        output.add(packetID) // packet ID
        output.addAll(intToByteList(packetIndexID.toUInt())) // index
        return output
    }

    abstract fun send();

    protected fun sendData(data: ByteArray) {
        if (controller.device != null) {
            Log.println(Log.INFO, "SendablePacket",
                "Attempting to send packet with index $packetIndexID"
            )
            val writeCharacteristic = controller.device!!.getCharacteristic(
                BLEControllerManager.SERVICE_UUID,
                BLEControllerManager.TO_ESP32_UUID
            )
            writeCharacteristic?.let {
                controller.device!!.writeCharacteristic(
                    it, data,
                    WriteType.WITH_RESPONSE
                )
                return // success
            }
        }
        this.sendTime = System.currentTimeMillis()
        controller.unreceivedPackets[packetIndexID] = this
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