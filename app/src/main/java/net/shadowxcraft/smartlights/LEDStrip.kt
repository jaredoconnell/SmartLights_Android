package net.shadowxcraft.smartlights

import kotlin.math.*

class LEDStrip {
    class Channel private constructor(pwmPin: Int, pwmController: PWMController) {
        constructor (pwmPin: Int, pwmController: PWMController, red: Double, green: Double, blue: Double)
                : this(pwmPin, pwmController)
        {
            isWhite = false
            this.red = red
            this.green = green
            this.blue = blue
        }
        constructor (pwmPin: Int, pwmController: PWMController, colorTemp: Int)
                : this(pwmPin, pwmController)
        {
            isWhite = true
            this.colorTemp = colorTemp
            setRGBFromTemp(colorTemp)
        }
        var pwmPin: Int = pwmPin
        var pwmController: PWMController = pwmController
        var red: Double = 0.0
        var green: Double = 0.0
        var blue: Double = 0.0
        var colorTemp: Int = -1
        var isWhite: Boolean = false

        private fun setRGBFromTemp(temp: Int) {
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

            if(temperature >= 66) {
                blue = 255.0
            } else {

                if (temperature <= 19) {
                    blue = 0.0
                } else {
                    blue = temperature - 10
                    blue = 138.5177312231 * ln(blue) - 305.0447927307
                    if (blue < 0) {
                        blue = 0.0
                    } else if (blue > 255) {
                        blue = 255.0
                    }
                }
            }
            this.red = red
            this.green = green
            this.blue = blue
        }
    }


}