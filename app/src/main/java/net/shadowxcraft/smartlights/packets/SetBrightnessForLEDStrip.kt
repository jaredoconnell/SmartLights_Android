package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.LEDStrip

class SetBrightnessForLEDStrip(private val ledStrip: LEDStrip, private val sendOnState: Boolean)
    : SendablePacket(ledStrip.controller, 16)
{
    override fun send() {
        val output = getHeader()

        output.addAll(strToByteList(ledStrip.id))
        output.add(if (sendOnState) { if (ledStrip.onState) 1 else 0} else { 2 })
        output.addAll(shortToByteList(ledStrip.brightness))
        sendData(output.toByteArray())

    }
}