package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.ESP32
import net.shadowxcraft.smartlights.PWMDriver

class PWMDriversListResponse(controller: ESP32, bytes: ByteArray)
    : ReceivedPacket(controller, bytes)
{
    override fun process() {
        val numDrivers = getByte()
        for (i in 0 until numDrivers) {
            controller.addPWMDriver(PWMDriver(getByte()), false)
        }
    }
}