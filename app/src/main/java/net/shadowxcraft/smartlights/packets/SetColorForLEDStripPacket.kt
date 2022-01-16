package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.Color
import net.shadowxcraft.smartlights.LEDStrip

class SetColorForLEDStripPacket(private val ledStrip: LEDStrip,
                                private val color: Color,
                                private val milliseconds: UInt,
                                private val override: Boolean = false)
    : SendablePacket(ledStrip.controller, 19)
{
    override fun send() {
        val output = getHeader()

        output.addAll(strToByteList(ledStrip.id))
        output.addAll(colorToByteList(color))
        output.addAll(intToByteList(milliseconds))
        output.add(if (override) 1 else 0)
        sendData(output.toByteArray())

    }
}