package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.ColorSequence
import net.shadowxcraft.smartlights.ESP32

class DeleteColorSequencePacket(controller: ESP32, private val sequence: ColorSequence)
    : SendablePacket(controller, 25)
{
    override fun send() {
        val output = getHeader()
        output.addAll(strToByteList(sequence.id)) // Just the ID of the color sequence.
        sendData(output.toByteArray())
    }
}