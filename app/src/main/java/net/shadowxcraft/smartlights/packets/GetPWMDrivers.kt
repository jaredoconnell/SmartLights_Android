package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.ESP32

class GetPWMDrivers(controller: ESP32) : SendablePacket(controller, 1) {
    override fun send() {
        sendData(getHeader().toByteArray())
    }
}