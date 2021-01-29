package net.shadowxcraft.smartlights.packets

import android.util.Log
import net.shadowxcraft.smartlights.ESP32
import net.shadowxcraft.smartlights.LEDStrip
import net.shadowxcraft.smartlights.LEDStripComponent

class LEDStripGroupsListResponse(controller: ESP32, bytes: ByteArray)
    : ReceivedPacket(controller, bytes)
{
    override fun process() {
        val numGroupsTotal = getShort()
        val groupIndex = getShort()
        val ledStripGroup = bytesToLEDStripGroup()
        controller.addLEDStripGroup(ledStripGroup, false)
        Log.i("LEDStripGroupsListResp", "Num Groups: $numGroupsTotal, index:"
            + " $groupIndex")
    }
}