package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.ESP32
import net.shadowxcraft.smartlights.PWMDriver

class GetLEDStrips(controller: ESP32) : SendablePacket(controller) {
    override fun send() {
        sendData(byteArrayOf(2))
    }
}