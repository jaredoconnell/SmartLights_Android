package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.LEDStrip

class SetLEDStripCalibrationMode(private val ledStrip: LEDStrip,
                                 private val enable: Boolean,
                                 private val selectedIndex: Int)
    : SendablePacket(ledStrip.controller, 23)
{
    override fun send() {
        val output = getHeader()

        output.addAll(strToByteList(ledStrip.id))
        output.add(if (enable) 1 else 0)
        output.add((if (selectedIndex == -1) 255 else selectedIndex).toByte())
        sendData(output.toByteArray())

    }
}