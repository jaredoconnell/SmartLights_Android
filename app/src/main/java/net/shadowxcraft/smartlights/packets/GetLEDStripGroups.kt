package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.ESP32
import net.shadowxcraft.smartlights.PWMDriver

class GetLEDStripGroups(controller: ESP32) : SendablePacket(controller, 22) {
    override fun send() {
        sendData(getHeader().toByteArray())
    }
}