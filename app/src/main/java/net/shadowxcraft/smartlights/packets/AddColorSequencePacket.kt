package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.ColorSequence
import net.shadowxcraft.smartlights.ESP32
import net.shadowxcraft.smartlights.LEDStrip
import net.shadowxcraft.smartlights.PWMDriver

class AddColorSequencePacket(controller: ESP32, private val sequence: ColorSequence) : SendablePacket(controller) {
    override fun send() {
        val output = ArrayList<Byte>();
        output.add(8) // packet ID 8
        output.add(1) // It's okay to overwrite it

        output.addAll(shortToByteList(sequence.id)) // First the ID
        output.add(sequence.colors.size.toByte()) // Number of colors in the sequence
        output.add(sequence.sequenceType)
        output.addAll(shortToByteList(sequence.sustainTime))
        output.addAll(shortToByteList(sequence.transitionTime))
        output.add(sequence.transitionType)
        for (color in sequence.colors) {
            output.addAll(colorToByteList(color))
        }
        output.addAll(strToByteList(sequence.name))
        sendData(output.toByteArray())
    }
}