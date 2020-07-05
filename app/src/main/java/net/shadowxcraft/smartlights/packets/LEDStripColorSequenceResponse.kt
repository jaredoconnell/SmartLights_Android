package net.shadowxcraft.smartlights.packets;

import net.shadowxcraft.smartlights.ESP32

class LEDStripColorSequenceListResponse(controller: ESP32, bytes: ByteArray)
        : ReceivedPacket(controller, bytes)
{
    override fun process() {

    }
}