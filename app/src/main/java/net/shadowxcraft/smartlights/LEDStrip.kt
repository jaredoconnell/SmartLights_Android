package net.shadowxcraft.smartlights

import android.util.Log
import net.shadowxcraft.smartlights.packets.SetBrightnessForLEDStrip
import kotlin.math.pow
import kotlin.math.roundToInt

const val MAX_BRIGHTNESS = 4095

class LEDStrip(val id: Int, val name: String, val components: ArrayList<LEDStripComponent>,
               var currentSeq: ColorSequence?, val controller: ESP32)
{
    var brightness = MAX_BRIGHTNESS
    var onState = true
    var simpleColor = Color(255, 0, 0)

    /**
     * Give it a value between 0 and MAX_BRIGHTNESS,
     * and it will interpret that as on an exponential scale,
     * then convert that to linear.
     */
    fun setBrightnessExponential(exponentialBrightness: Int) {
        val originalPercentageDecimal = exponentialBrightness.toDouble() / MAX_BRIGHTNESS
        val adjustedPercentageDecimal = originalPercentageDecimal.pow(3)
        brightness = (adjustedPercentageDecimal * MAX_BRIGHTNESS).roundToInt()
        Log.println(Log.INFO,"LEDStrip", "Set brightness to $brightness")
    }

    /**
     * Gets the brightness in a way that matches the
     * exponential brightness.
     */
    fun getBrightnessExponential() : Int {
        val linearPercentageDecimal = brightness.toDouble() / MAX_BRIGHTNESS
        val adjustedPercentageDecimal = linearPercentageDecimal.pow(1.0/3.0)
        return (adjustedPercentageDecimal * MAX_BRIGHTNESS).roundToInt()
    }

    fun sendBrightnessPacket() {
        SetBrightnessForLEDStrip(this).queue()
    }
}