package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.Color
import net.shadowxcraft.smartlights.ESP32
import net.shadowxcraft.smartlights.LEDStrip
import net.shadowxcraft.smartlights.LEDStripComponent

abstract class ReceivedPacket(protected val controller: ESP32, private val bytes: ByteArray) {
    private var index = 1 // skip 0, the packet ID

    abstract fun process()

    protected fun getByte() : Int {
        return bytes[index++].toInt()
    }

    protected fun getShort() : Int {
        return bytes[index++].toInt() * 256 + bytes[index++]
    }

    protected fun bytesToStr() : String {
        val length = bytes[index++].toInt()
        var str = ""
        for (i in 0 until length) {
            str += bytes[index++].toChar()
        }
        return str
    }

    protected fun bytesToColor() : Color {
        val red: Int = bytes[0 + index++].toInt()
        val green: Int = bytes[1 + index++].toInt()
        val blue: Int = bytes[2 + index++].toInt()

        return Color(red, green, blue)
    }

    protected fun bytesToLEDStrip() : LEDStrip {
        val id = getShort()
        val components = ArrayList<LEDStripComponent>()
        val numColors = getByte()
        for (i in 0 until numColors) {
            val driverAddr = getByte()
            val pin = getByte()
            val color = bytesToColor()
            components.add(LEDStripComponent(color, controller.getPWMDriver(driverAddr)!!, pin))
        }
        val name = bytesToStr()
        // TODO: Instead of null current sequence, get it.
        return LEDStrip(id, name, components, null, controller)
    }
}