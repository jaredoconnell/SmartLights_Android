package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.*

class ScheduleChangePacket(private val scheduled: ScheduledChange)
    : SendablePacket(scheduled.ledStrip!!.controller, 10)
{
    override fun send() {
        val output = ArrayList<Byte>();
        output.add(packetID) // packet ID 8

        output.addAll(strToByteList(scheduled.id)) // First the ID
        if (scheduled.name.length > 29)
            throw IllegalStateException("Name too long according to the specification")
        output.addAll(strToByteList(scheduled.name))
        output.add(scheduled.hour)
        output.add(scheduled.minute)
        output.add(scheduled.second)
        output.addAll(intToByteList(scheduled.secondsUntilOff.toUInt()))
        if (scheduled.isSpecificDate) {
            output.add(0)
            output.add((scheduled.year - 1900).toByte())
            output.add(scheduled.month)
            output.add(scheduled.day)
            output.addAll(shortToByteList(scheduled.repeatInverval))
        } else {
            output.add(1)
            var weekData = scheduled.days
            if (scheduled.repeatInverval > 0) {
                weekData = weekData or 0b10000000
            }
            output.add(weekData.toByte())
        }
        output.addAll(strToByteList(scheduled.ledStrip!!.id))
        var scheduledChanges = 0
        if (scheduled.turnOn) {
            scheduledChanges = scheduledChanges or 0b00000001
        }
        if (scheduled.newBrightness >= 0) {
            scheduledChanges = scheduledChanges or 0b00000010
        }
        if (scheduled.newColor != null) {
            scheduledChanges = scheduledChanges or 0b00000100
        }
        if (scheduled.newColorSequenceID != null) {
            scheduledChanges = scheduledChanges or 0b00001000
        }
        output.add(scheduledChanges.toByte())

        if (scheduled.newBrightness >= 0) {
            output.addAll(shortToByteList(scheduled.newBrightness))
        }
        if (scheduled.newColor != null) {
            output.addAll(colorToByteList(scheduled.newColor!!))
        }
        if (scheduled.newColorSequenceID != null) {
            output.addAll(strToByteList(scheduled.newColorSequenceID!!))
        }
        sendData(output.toByteArray())
    }
}