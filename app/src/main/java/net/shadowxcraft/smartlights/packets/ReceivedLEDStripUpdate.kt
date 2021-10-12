package net.shadowxcraft.smartlights.packets

import android.util.Log
import net.shadowxcraft.smartlights.ESP32
import net.shadowxcraft.smartlights.LEDStrip
import net.shadowxcraft.smartlights.LEDStripComponent
import net.shadowxcraft.smartlights.SharedData

class ReceivedLEDStripUpdate(controller: ESP32, bytes: UByteArray)
    : ReceivedPacket(controller, bytes)
{
    override fun process() {
        val ledStripID = bytesToStr()
        val ledStrip = controller.getLEDStrip(ledStripID)
        if (ledStrip == null) {
            Log.e("ReceivedLEDStripUpdate", "Unknown LED Strip ID")
            return;
        }
        val packetData = getByte()
        if ((packetData and 0b00000001) > 0) {
            ledStrip.setOnState(getByte() == 1, true)
        }
        if ((packetData and 0b00000010) > 0) {
            ledStrip.setBrightness(getShort(), true)
        }
        if ((packetData and 0b00000100) > 0) {
            val color = bytesToColor()
            getInt() // ms
            ledStrip.setSimpleColor(color, true)
        }
        if ((packetData and 0b00001000) > 0) {
            val colorSequenceID = bytesToStr()
            val colorSequence = SharedData.colorsSequences[colorSequenceID]
            ledStrip.setCurrentSeq(colorSequence, true)
        }
        SharedData.ledStripsFragment?.adapter?.notifyDataSetChanged()
        SharedData.ledStripGroupsFragment?.adapter?.notifyDataSetChanged()
        Log.i("ReceivedLEDStripUpdate", "Received LED Strip Update")
    }
}