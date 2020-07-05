package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.ESP32
import net.shadowxcraft.smartlights.LEDStrip
import net.shadowxcraft.smartlights.LEDStripComponent

class LEDStripsListResponse(controller: ESP32, bytes: ByteArray)
    : ReceivedPacket(controller, bytes)
{
    override fun process() {
        val numLedStrips = getShort()
        val offsetOfPacket = getShort()
        val numLEDStripsSentInPacket = getByte()
        for (colorStripIndex in 0 until numLEDStripsSentInPacket) {
            controller.addLEDStrip(bytesToLEDStrip(), false)
        }
    }
}