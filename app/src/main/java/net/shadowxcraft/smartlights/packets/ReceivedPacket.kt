package net.shadowxcraft.smartlights.packets

import android.util.Log
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

    protected fun getInt() : Int {
        return getByte() * 256 * 256 * 256 +
            getByte() * 256 * 256 +
            getByte() * 256 +
            getByte()
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
        val id = bytesToStr()
        val components = ArrayList<LEDStripComponent>()
        val numColors = getByte()
        val currentSequenceID = bytesToStr()
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
        val currentSequence : ColorSequence? = if (currentSequenceID != "") {
            controller.colorsSequences[currentSequenceID]
        } else {
            null
        }

        val newLEDStrip = LEDStrip(id, name, controller)
        newLEDStrip.components.addAll(components)

        newLEDStrip.setCurrentSeq(currentSequence, false)
        newLEDStrip.setOnState(isOn, false)
        newLEDStrip.setBrightness(brightness, false)
        if (hasTemporaryColor && secondsLeftTempColor == 0)
            newLEDStrip.setSimpleColor(tempColor, false)

        return newLEDStrip
    }

    protected fun bytesToLEDStripGroup() : LEDStripGroup {
        val id = bytesToStr()
        val name = bytesToStr()
        val numLEDStrips = getByte()
        val ledStrips = ArrayList<LEDStrip>()
        for (i in 1..numLEDStrips) {
            val ledStripID = bytesToStr()
            val ledStrip = controller.ledStrips[ledStripID]
            if (ledStrip != null) {
                ledStrips.add(ledStrip)
            } else {
                Log.println(Log.WARN, "ReceivedPacket", "Could not find LED Strip" +
                        " matching that ID in Led Strip Group packet.")
            }
        }
        return LEDStripGroup(id, name, ledStrips, controller)
    }

    protected fun bytesToColorSequence() : ColorSequence {
        val id = bytesToStr()
        val numItems = getByte()
        getByte() // unused, sequenceType
        val sustainTime = getShort()
        val transitionTime = getShort()
        getByte() // unused, transitionType

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

    protected fun bytesToScheduledChange(): ScheduledChange {
        val id = bytesToStr()
        val name = bytesToStr()
        val sc = ScheduledChange(id, name, null)

        sc.hour = getByte().toByte()
        sc.minute = getByte().toByte()
        sc.second = getByte().toByte()
        sc.secondsUntilOff = getInt()

        val type = getByte()
        if (type == 0) {
            sc.isSpecificDate = true
            // Specific dates
            val yearsSince1900 = getByte()
            sc.year = yearsSince1900 + 1900
            sc.month = getByte().toByte()
            sc.day = getByte().toByte()
            sc.repeatInverval = getShort()
        } else if (type == 1) {
            sc.isSpecificDate = false
            // Days of week
            val daysBitwise = getByte()
            sc.days = daysBitwise
        } else {
            Log.println(Log.ERROR, "ReceivedPacket", "Unknown schedule type. Aborting.\n");
            return sc;
        }

        val ledStripID = bytesToStr()
        sc.ledStrip = controller.ledStrips[ledStripID]
        if (sc.ledStrip == null) {
            sc.ledStrip = controller.ledStripGroups[ledStripID]
        }

        val changes = getByte()
        val turnOn = changes and 0b00000001 > 0
        val brightnessChanges = changes and 0b00000010 > 0
        val colorChanges = changes and 0b00000100 > 0
        val colorSequenceChanges = changes and 0b00001000 > 0
        sc.turnOn = turnOn

        if (brightnessChanges) {
            val newBrightness = getShort()
            sc.newBrightness = newBrightness
        } else {
            sc.newBrightness = -1
        }
        if (colorChanges) {
            sc.newColor = bytesToColor()
        }
        if (colorSequenceChanges) {
            sc.newColorSequenceID = bytesToStr()
        }
        return sc
    }
}