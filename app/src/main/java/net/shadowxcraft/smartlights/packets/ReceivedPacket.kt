package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.*

abstract class ReceivedPacket(protected val controller: ESP32, private val bytes: ByteArray) {
    private var index = 1 // skip 0, the packet ID

    abstract fun process()

    protected fun getByte() : Int {
        return bytes[index++].toUByte().toInt()
    }

    protected fun getShort() : Int {
        return getByte() * 256 + getByte()
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
        val red: Int = getByte()
        val green: Int = getByte()
        val blue: Int = getByte()

        return Color(red, green, blue)
    }

    protected fun bytesToLEDStrip() : LEDStrip {
        val id = getShort()
        val components = ArrayList<LEDStripComponent>()
        val numColors = getByte()
        val currentSequenceID = getShort()
        val isOn = getByte() != 0
        val brightness = getShort()
        val hasTemporaryColor = getByte() != 0
        val secondsLeftTempColor = getShort()
        val tempColor = bytesToColor()
        for (i in 0 until numColors) {
            val driverAddr = getByte()
            val pin = getByte()
            val color = bytesToColor()
            val pwmDriver = controller.getPWMDriverByAddress(driverAddr)
            components.add(LEDStripComponent(color, pwmDriver!!, pin))
        }
        val name = bytesToStr()
        val currentSequence : ColorSequence? = if (currentSequenceID != 0) {
            controller.colorsSequences[currentSequenceID]
        } else {
            null
        }

        val newLEDStrip = LEDStrip(id, name, components, currentSequence, controller)

        newLEDStrip.onState = isOn
        newLEDStrip.brightness = brightness
        if (hasTemporaryColor && secondsLeftTempColor == 0)
            newLEDStrip.simpleColor = tempColor

        return newLEDStrip
    }

    protected fun bytesToColorSequence() : ColorSequence {
        val id = getShort()
        val numItems = getByte()
        val sequenceType = getByte()
        val sustainTime = getShort()
        val transitionTime = getShort()
        val transitionType = getByte()

        val colors = ArrayList<Color>()
        for (i in 0 until numItems) {
            colors.add(bytesToColor())
        }
        val name = bytesToStr()

        val colorSequence = ColorSequence(id, name)
        colorSequence.colors.addAll(colors)
        //colorSequence.sequenceType = sequenceType
        colorSequence.sustainTime = sustainTime
        colorSequence.transitionTime = transitionTime
        //colorSequence.transitionType = transitionType.toByte()
        return colorSequence
    }
}