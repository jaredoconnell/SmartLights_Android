package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.Color
import net.shadowxcraft.smartlights.ESP32
import net.shadowxcraft.smartlights.LEDStrip
import net.shadowxcraft.smartlights.PWMDriver

class SetColorForLEDStripPacket(private val ledStrip: LEDStrip,
                                private val color: Color,
                                private val milliseconds: UInt)
    : SendablePacket(ledStrip.controller, 19)
{
    override fun send() {
        val output = getHeader()

        output.addAll(strToByteList(ledStrip.id))
        output.addAll(colorToByteList(color))
        output.addAll(intToByteList(milliseconds))
        sendData(output.toByteArray())

    }
}