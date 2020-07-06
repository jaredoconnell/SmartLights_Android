package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.ESP32

class ColorSequenceListResponse(controller: ESP32, bytes: ByteArray)
    : ReceivedPacket(controller, bytes)
{
    override fun process() {
        val numLEDStrips = getShort()
        val offset = getShort()
        val numLEDStripsSentInThisPacket = getByte()
        for (i in 0 until numLEDStripsSentInThisPacket) {
            controller.addColorSequence(bytesToColorSequence(), false)
        }
        /*output += static_cast<char>(251);

        const std::map<int, ColorSequence *>& colorSequences = controller.getColorSequences();
        output += shortToStr(colorSequences.size()); // first, the total number of LED strips
        output += shortToStr(offset); // second, the offset for the packet
        auto itr = colorSequences.cbegin();
        std::advance(itr, offset);
        int actualQuantity = 0;
        std::string componentsStr = "";
        for (int i = 0; i < quantity && itr != colorSequences.cend(); i++) {
            ColorSequence * colorSequence = (* itr++).second;
            componentsStr += colorSequenceToStr(colorSequence);
            actualQuantity++;
        }
        output += static_cast<char>(actualQuantity); // third, the quantity sent in this packet
        output += componentsStr;*/
    }
}