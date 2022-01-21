package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.ColorSequence
import net.shadowxcraft.smartlights.ESP32

class SetButtonColorSequencePacket(controller: ESP32, private val sequence: ColorSequence,
                                   private val buttonID: Int)
    : SendablePacket(controller, 26)
{
    override fun send() {
        val output = getHeader()
        output.addAll(strToByteList(sequence.id)) // Just the ID of the color sequence.
        output.add(buttonID.toByte())
        sendData(output.toByteArray())
    }
}