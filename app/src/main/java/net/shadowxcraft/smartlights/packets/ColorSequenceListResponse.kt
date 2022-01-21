package net.shadowxcraft.smartlights.packets

import android.util.Log
import android.widget.Toast
import androidx.navigation.fragment.NavHostFragment
import net.shadowxcraft.smartlights.ESP32
import net.shadowxcraft.smartlights.SharedData
import net.shadowxcraft.smartlights.ui.edit_color_sequence.ColorSequenceEditorFragment

class ColorSequenceListResponse(controller: ESP32, bytes: UByteArray)
    : ReceivedPacket(controller, bytes)
{
    override fun process() {
        val numColorSequences = getShort()
        val offset = getShort()
        val numColorSequencesSentInThisPacket = getByte()
        // First, get the fragment if visible to know if it needs to be removed from view
        val currentlyEditedColorSequenceID = SharedData.editColorSequenceFragment?.colorSequence?.id

        for (i in 0 until numColorSequencesSentInThisPacket) {
            val colorSequence = bytesToColorSequence()

            // Exits editor if this colorsequence is there
            if (currentlyEditedColorSequenceID != null
                && currentlyEditedColorSequenceID == colorSequence.id) {
                    // Exit fragment due to the updated ColorSequence
                SharedData.editColorSequenceFragment?.activity?.supportFragmentManager?.popBackStack()
                Toast.makeText(SharedData.editColorSequenceFragment?.activity, "ColorSequence edited with remote.", Toast.LENGTH_SHORT).show()
            }

            controller.addColorSequence(colorSequence, false)
        }

        Log.i("ColorSequenceResponse", "Num Color sequences Strips: $numColorSequences, offsetOfPacket:"
                +" $offset, num color sequences in packet: $numColorSequencesSentInThisPacket")
    }
}