package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.Color
import net.shadowxcraft.smartlights.ESP32
import net.shadowxcraft.smartlights.LEDStrip
import net.shadowxcraft.smartlights.PWMDriver

class SetColorForLEDStripPacket(private val ledStrip: LEDStrip,
                                private val color: Color,
                                private val seconds: Int)
    : SendablePacket(ledStrip.controller, 19)
{
    override fun send() {
        val output = ArrayList<Byte>();
        output.add(19) // packet ID

        output.addAll(shortToByteList(ledStrip.id))
        output.addAll(colorToByteList(color))
        output.addAll(shortToByteList(seconds))
        sendData(output.toByteArray())

    }
}