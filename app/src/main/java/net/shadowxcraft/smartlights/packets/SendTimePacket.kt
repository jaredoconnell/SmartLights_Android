package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.*

class SendTimePacket(controller: ESP32)
    : SendablePacket(controller, 20)
{
    override fun send() {
        val output = getHeader()

        // Current time plus 200 milliseconds
        output.addAll(longToByteList(((System.currentTimeMillis() + 200) / 1000).toULong()))
        sendData(output.toByteArray())
    }
}