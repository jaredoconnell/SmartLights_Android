package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.ESP32

class GetColorSequences(controller: ESP32) : SendablePacket(controller, 12) {
    override fun send() {
        sendData(getHeader().toByteArray())
    }
}