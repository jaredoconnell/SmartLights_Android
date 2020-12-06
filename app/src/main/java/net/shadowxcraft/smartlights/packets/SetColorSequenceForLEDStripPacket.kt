package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.ESP32
import net.shadowxcraft.smartlights.LEDStrip
import net.shadowxcraft.smartlights.PWMDriver

class SetColorSequenceForLEDStripPacket(private val ledStrip: LEDStrip)
    : SendablePacket(ledStrip.controller, 9)
{
    override fun send() {
        val output = ArrayList<Byte>();
        output.add(9) // packet ID

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