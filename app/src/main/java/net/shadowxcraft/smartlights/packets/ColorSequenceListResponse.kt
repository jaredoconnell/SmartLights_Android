package net.shadowxcraft.smartlights.packets

import android.util.Log
import net.shadowxcraft.smartlights.ESP32

class ColorSequenceListResponse(controller: ESP32, bytes: UByteArray)
    : ReceivedPacket(controller, bytes)
{
    override fun process() {
        val numColorSequences = getShort()
        val offset = getShort()
        val numColorSequencesSentInThisPacket = getByte()
        for (i in 0 until numColorSequencesSentInThisPacket) {
            controller.addColorSequence(bytesToColorSequence(), false)
        }

        Log.i("ColorSequenceResponse", "Num Color sequences Strips: $numColorSequences, offsetOfPacket:"
                +" $offset, num color sequences in packet: $numColorSequencesSentInThisPacket")
    }
}