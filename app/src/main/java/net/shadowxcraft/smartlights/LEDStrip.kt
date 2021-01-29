package net.shadowxcraft.smartlights

import android.util.Log
import net.shadowxcraft.smartlights.packets.SetBrightnessForLEDStrip
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.pow
import kotlin.math.roundToInt

const val MAX_BRIGHTNESS = 4095

open class LEDStrip(val id: String, val name: String,
                    open var currentSeq: ColorSequence?, val controller: ESP32)
{
    val components = ArrayList<LEDStripComponent>()
    var brightness = MAX_BRIGHTNESS
    open var onState = true
    open var simpleColor = Color(255, 0, 0)
    val scheduledChanges = TreeMap<String, ScheduledChange>()

    fun convertToLinear(exponentialInput: Int): Int {
        val originalPercentageDecimal = exponentialInput.toDouble() / MAX_BRIGHTNESS
        val adjustedPercentageDecimal = originalPercentageDecimal.pow(3)
        return (adjustedPercentageDecimal * MAX_BRIGHTNESS).roundToInt()
    }

    fun convertToExponential(linearInput: Int): Int {
        val linearPercentageDecimal = linearInput.toDouble() / MAX_BRIGHTNESS
        val adjustedPercentageDecimal = linearPercentageDecimal.pow(1.0/3.0)
        return (adjustedPercentageDecimal * MAX_BRIGHTNESS).roundToInt()
    }

    /**
     * Give it a value between 0 and MAX_BRIGHTNESS,
     * and it will interpret that as on an exponential scale,
     * then convert that to linear.
     */
    open fun setBrightnessExponential(exponentialInput: Int) {
        brightness = convertToLinear(exponentialInput)
        Log.println(Log.INFO,"LEDStrip", "Set brightness to $brightness")
    }

    /**
     * Gets the brightness in a way that matches the
     * exponential brightness.
     */
    fun getBrightnessExponential() : Int {
        return convertToExponential(brightness)
    }

    fun sendBrightnessPacket() {
        SetBrightnessForLEDStrip(this).queue()
    }
}