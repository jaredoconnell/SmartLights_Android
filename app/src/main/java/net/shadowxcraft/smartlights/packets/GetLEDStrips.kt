package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.ESP32

class GetLEDStrips(controller: ESP32) : SendablePacket(controller, 2) {
    override fun send() {
        sendData(getHeader().toByteArray())
    }
}