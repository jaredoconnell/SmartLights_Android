package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.LEDStrip

class SetColorSequenceForLEDStripPacket(private val ledStrip: LEDStrip)
    : SendablePacket(ledStrip.controller, 9)
{
    override fun send() {
        val output = getHeader()

        val id = if (ledStrip.currentSeq == null) {
            ""
        } else {
            ledStrip.currentSeq!!.id
        }
        output.addAll(strToByteList(ledStrip.id))
        output.addAll(strToByteList(id))
        sendData(output.toByteArray())

    }
}