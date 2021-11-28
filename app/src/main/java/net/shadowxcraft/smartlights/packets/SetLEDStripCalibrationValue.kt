package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.LEDStrip

class SetLEDStripCalibrationValue(private val ledStrip: LEDStrip,
                                  private val selectedIndex: Int,
                                  private val calibratedValue: Int)
    : SendablePacket(ledStrip.controller, 24)
{
    override fun send() {
        val output = getHeader()

        output.addAll(strToByteList(ledStrip.id))
        output.add(selectedIndex.toByte())
        output.addAll(shortToByteList(calibratedValue))
        sendData(output.toByteArray())

    }
}