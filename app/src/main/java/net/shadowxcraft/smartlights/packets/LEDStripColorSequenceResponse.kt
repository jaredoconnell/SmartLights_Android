package net.shadowxcraft.smartlights.packets;

import net.shadowxcraft.smartlights.ESP32

class LEDStripColorSequenceListResponse(controller: ESP32, bytes: UByteArray)
        : ReceivedPacket(controller, bytes)
{
    override fun process() {

    }
}