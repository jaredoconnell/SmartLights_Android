package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.ESP32
import net.shadowxcraft.smartlights.PWMDriver

class AddPWMDriverPacket(controller: ESP32, private val driver: PWMDriver) : SendablePacket(controller) {
    override fun send() {
        sendData(byteArrayOf(3, driver.i2cAddress.toByte()))
    }
}