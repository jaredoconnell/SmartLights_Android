package net.shadowxcraft.smartlights

import java.util.*
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone

class ScheduledChange(val id: String, var name: String, var ledStrip: LEDStrip?) {
    // name max length 29

    var hour: Byte = 0 // zero-indexed
    var minute: Byte = 0 // zero-indexed
    var second: Byte = 0 // zero-indexed
    var isSpecificDate = true
    var repeatInverval = 0
    var secondsUntilOff = 0 // 0 for no turn off

    // For date
    var year = 1970
    var month: Byte = 1 // one-indexed
    var day: Byte = 1 // one-indexed

    // for days of the week
    // Stored in the bits
    var days = 0

    private fun getDayBitwise(day: Int) : Int {
        return 0b00000001 shl day
    }

    // Sunday is 0, Saturday is 6
    fun setDayStatus(day: Int, isOn: Boolean) {
        val dayBitwise = getDayBitwise(day)
        days = if (isOn) {
            days or dayBitwise
        } else {
            days and dayBitwise.inv()
        }
    }

    fun makeDaily() {
        days = 0b01111111
    }

    fun getDayStatus(day: Int): Boolean {
        val dayBitwise = getDayBitwise(day)
        return days and dayBitwise > 0
    }

    // Changes
    // If true, it will turn it on
    // If false, and nothing else, it will turn it off
    // If false, and another change is set, it will only change it without
    // changing the on status, useful for changes like sunset change to lower
    // brightness, but only if it is on.
    var turnOn = true // false for turn off or do nothing, true for turn on
    var newBrightness = -1
    var newColor: Color? = null
    var newColorSequenceID: String? = null

    fun turnsOff(): Boolean {
        return !turnOn && newColor == null && newColorSequenceID == null && newBrightness == -1
    }

    fun isWithin24Hours(): Boolean {
        val millisFromNow = getTimeLocal().millis - DateTime.now().millis
        return millisFromNow < 24 /*hours*/ * 60 /*mins*/ * 60 /*seconds*/ * 1000 /*milliseconds*/
    }

    fun isEveryDay(): Boolean {
        return days == 0b01111111
    }

    fun setFromLocalDateTime(localTime: DateTime) {
        val utcTime = convertFromLocalTime(localTime)
        this.year = utcTime.year
        this.month = utcTime.monthOfYear.toByte()
        this.day = utcTime.dayOfMonth.toByte()
        this.hour = utcTime.hourOfDay.toByte()
        this.minute = utcTime.minuteOfHour.toByte()
        this.second = utcTime.secondOfMinute.toByte()
    }

    fun getTimeLocal(): DateTime {
        val utcTime = DateTime(year, month.toInt(), day.toInt(), hour.toInt(),
            minute.toInt(), second.toInt())
        return convertToLocalTime(utcTime)
    }

    fun getNowLocal(): DateTime {
        return convertToLocalTime(DateTime.now())
    }

    fun convertToLocalTime(dateTime: DateTime): DateTime {
        return dateTime.toDateTime(DateTimeZone.forOffsetMillis(TimeZone.getDefault().rawOffset))
    }
    fun convertFromLocalTime(dateTime: DateTime): DateTime {
        return dateTime.toDateTime(DateTimeZone.forOffsetMillis(0))
    }

    private val dayInitials = arrayOf("Su.", "Mo.", "Tu.", "We.", "Th.", "Fr.", "Sa.")
    fun getDaysOfWeekInitials(): String {
        var output = ""
        for (i in 0..6) {
            if (getDayStatus(i)) {
                if (output.isNotEmpty())
                    output += " "
                output += dayInitials[i]
            }
        }
        return output
    }
}