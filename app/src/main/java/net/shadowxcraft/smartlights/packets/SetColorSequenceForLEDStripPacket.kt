package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.ESP32
import net.shadowxcraft.smartlights.LEDStrip
import net.shadowxcraft.smartlights.PWMDriver

class SetColorSequenceForLEDStripPacket(private val ledStrip: LEDStrip) : SendablePacket(ledStrip.controller) {
    override fun send() {
        val output = ArrayList<Byte>();
        output.add(9) // packet ID

        val id = if (ledStrip.currentSeq == null) {
            -1
        } else {
            ledStrip.currentSeq!!.id
        }
        output.addAll(shortToByteList(id))
        output.addAll(shortToByteList(ledStrip.id))
        sendData(output.toByteArray())

    }
}