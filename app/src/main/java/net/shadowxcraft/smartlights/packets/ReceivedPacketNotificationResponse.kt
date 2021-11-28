package net.shadowxcraft.smartlights.packets

import android.util.Log
import net.shadowxcraft.smartlights.ESP32

class ReceivedPacketNotificationResponse(controller: ESP32, bytes: UByteArray)
    : ReceivedPacket(controller, bytes)
{
    override fun process() {
        val packetIndex = getInt()
        val success = getByte()
        if (success == 0 || success == 2) {
            controller.unreceivedPackets.remove(packetIndex)
            Log.println(
                Log.INFO, "ReceivedPacketNotifResp",
                "Removing packet ID $packetIndex from queue"
            )
        }
    }
}