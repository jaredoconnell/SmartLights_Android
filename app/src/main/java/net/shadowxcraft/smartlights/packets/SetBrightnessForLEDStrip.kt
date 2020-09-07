package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.LEDStrip

class SetBrightnessForLEDStrip(private val ledStrip: LEDStrip) : SendablePacket(ledStrip.controller) {
    override fun send() {
        val output = ArrayList<Byte>();
        output.add(16) // packet ID

        output.addAll(shortToByteList(ledStrip.id))
        output.add(if (ledStrip.onState) 1 else 0)
        output.addAll(shortToByteList(ledStrip.brightness))
        sendData(output.toByteArray())

    }
}