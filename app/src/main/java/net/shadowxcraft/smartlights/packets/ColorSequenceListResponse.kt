package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.ESP32

class ColorSequenceListResponse(controller: ESP32, bytes: ByteArray)
    : ReceivedPacket(controller, bytes)
{
    override fun process() {

    }
}