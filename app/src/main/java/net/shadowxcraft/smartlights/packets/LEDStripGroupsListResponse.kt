package net.shadowxcraft.smartlights.packets

import android.util.Log
import net.shadowxcraft.smartlights.ESP32

class LEDStripGroupsListResponse(controller: ESP32, bytes: UByteArray)
    : ReceivedPacket(controller, bytes)
{
    override fun process() {
        val numGroupsTotal = getShort()
        val groupIndex = getShort()
        val ledStripGroup = bytesToLEDStripGroup()
        controller.addLEDStripGroup(ledStripGroup, false, true)
        Log.i("LEDStripGroupsListResp", "Num Groups: $numGroupsTotal, index:"
            + " $groupIndex")
    }
}