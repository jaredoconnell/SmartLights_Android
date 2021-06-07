package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.ESP32
import net.shadowxcraft.smartlights.PWMDriver

class AddPWMDriverPacket(controller: ESP32, private val driver: PWMDriver)
    : SendablePacket(controller, 3)
{
    override fun send() {
        val output = getHeader()
        output.add(driver.i2cAddress.toByte())
        sendData(output.toByteArray())
    }
}