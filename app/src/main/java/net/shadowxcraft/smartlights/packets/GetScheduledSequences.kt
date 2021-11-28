package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.ESP32

class GetScheduledSequences(controller: ESP32) : SendablePacket(controller, 13) {
    override fun send() {
        sendData(getHeader().toByteArray())
    }
}