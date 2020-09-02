package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.ESP32
import net.shadowxcraft.smartlights.LEDStrip
import net.shadowxcraft.smartlights.PWMDriver

class AddLEDStripPacket(controller: ESP32, private val strip: LEDStrip) : SendablePacket(controller) {
    override fun send() {
        val output = ArrayList<Byte>();
        output.add(5) // packet ID 5
        output.addAll(shortToByteList(strip.id))
        output.add(strip.components.size.toByte())
        for (component in strip.components) {
            output.add(if (component.driver == null) 0 else component.driver!!.i2cAddress.toByte())
            output.add(component.driverPin.toByte())
            output.add(component.color.red.toByte())
            output.add(component.color.green.toByte())
            output.add(component.color.blue.toByte())
            val curColorSequence = strip.currentSeq
            val curColorSequenceID = curColorSequence?.id ?: 0
            output.addAll(shortToByteList(curColorSequenceID))
        }
        output.addAll(strToByteList(strip.name));

        sendData(output.toByteArray())
    }
}