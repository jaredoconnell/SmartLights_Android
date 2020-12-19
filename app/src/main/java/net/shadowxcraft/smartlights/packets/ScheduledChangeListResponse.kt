package net.shadowxcraft.smartlights.packets

import android.util.Log
import net.shadowxcraft.smartlights.ESP32

class ScheduledChangeListResponse(controller: ESP32, bytes: ByteArray)
    : ReceivedPacket(controller, bytes)
{
    override fun process() {
        val numScheduledChanges = getShort()
        val offset = getShort()
        val numScheduledChangesSentInThisPacket = getByte()
        for (i in 0 until numScheduledChangesSentInThisPacket) {
            val scheduledChange = bytesToScheduledChange();
            scheduledChange.ledStrip!!.scheduledChanges[scheduledChange.id] = scheduledChange
        }

        Log.i("SchCngListResponse", "Num scheduled changes: $numScheduledChanges, offsetOfPacket:"
                +" $offset, num scheduled changes in packet: $numScheduledChangesSentInThisPacket")
    }
}