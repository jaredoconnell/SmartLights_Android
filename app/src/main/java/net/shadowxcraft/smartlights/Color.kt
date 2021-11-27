package net.shadowxcraft.smartlights

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

class Color() { // default constructor creates a white color.
    var red: Int = 255
    var green: Int = 255
    var blue: Int = 255

    // Color
    constructor(red: Int, green: Int, blue: Int) : this() {
        this.red = red
        this.green = green
        this.blue = blue
    }
    // White from color temp
    constructor(colorTemp: Int, brightness: Int) : this() {
        setRGBFromTemp(colorTemp)
    }

    constructor(rgba: Int) : this() {
        red = (rgba shr 16 and 0xff)
        green = (rgba shr 8 and 0xff)
        blue = (rgba and 0xff)
        //val a: Float = (rgba shr 24 and 0xff) / 255.0f
    }
    fun setRGBFromTemp(temp: Int) {
        val temperature = temp / 100.0
        var red: Double
        var green: Double
        var blue: Double
        // calc red

        if(temperature <= 66) {
            red = 255.0
        } else {
            red = temperature - 60
            red = 329.698727446 * (red.pow(-0.1332047592))
            if (red < 0) {
                red = 0.0
            } else if (red > 255) {
                red = 255.0
            }
        }
        // green

        if (temperature <= 66) {
            green = temperature
            green = 99.4708025861 * ln(green) - 161.1195681661

        } else {
            green = temperature - 60
            green = 288.1221695283 * (green.pow(-0.0755148492))
        }
        if(green < 0) {
            green = 0.0
        } else if(green > 255) {
            green = 255.0
        }

        // blue

        if(temperature >= 65) {
            blue = 255.0
        } else {

            if (temperature <= 17) {
                blue = 0.0
            } else {
                blue = 138.5177312231 * ln(temperature - 8) - 305.0447927307
                if (blue < 0) {
                    blue = 0.0
                } else if (blue > 255) {
                    blue = 255.0
                }
            }
        }
        this.red = red.roundToInt()
        this.green = green.roundToInt()
        this.blue = blue.roundToInt()
    }

    fun toArgb() : Int {
        return android.graphics.Color.argb(255, red, green, blue)
    }

    fun hasWhite() : Boolean {
        return red > 0 && green > 0 && blue > 0
    }

    override fun toString() : String {
        return "Red: $red, Green: $green, Blue: $blue"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Color) {
            return false
        }
        return other.red == red && other.green == green && other.blue == blue
    }
}