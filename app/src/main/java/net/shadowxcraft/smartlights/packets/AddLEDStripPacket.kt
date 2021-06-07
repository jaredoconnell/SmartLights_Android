package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.Color
import net.shadowxcraft.smartlights.ESP32
import net.shadowxcraft.smartlights.LEDStrip
import net.shadowxcraft.smartlights.PWMDriver

class AddLEDStripPacket(controller: ESP32, private val strip: LEDStrip)
    : SendablePacket(controller, 5)
{
    override fun send() {
        val output = getHeader()
        output.addAll(strToByteList(strip.id))
        output.add(strip.components.size.toByte())

        val curColorSequence = strip.currentSeq
        val curColorSequenceID = curColorSequence?.id ?: ""
        output.addAll(strToByteList(curColorSequenceID))

        output.add(if (strip.onState) 1 else 0)
        output.addAll(shortToByteList(strip.brightness))
        output.add(0)
        output.addAll(shortToByteList(0))
        output.addAll(colorToByteList(Color(0)))

        for (component in strip.components) {
            val driverID = component.driver.getAddress().toByte()
            output.add(driverID)
            output.add(component.driverPin.toByte())
            output.add(component.color.red.toByte())
            output.add(component.color.green.toByte())
            output.add(component.color.blue.toByte())
        }

        output.addAll(strToByteList(strip.name));

        sendData(output.toByteArray())
    }
}