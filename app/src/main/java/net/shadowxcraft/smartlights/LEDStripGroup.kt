package net.shadowxcraft.smartlights

class LEDStripGroup(id: String, name: String, val ledStrips: ArrayList<LEDStrip>, controller: ESP32)
    : LEDStrip(id, name, null, controller)
{

    override var onState: Boolean
        get() = super.onState
        set(on) {
            super.onState = on
            for (i in ledStrips) {
                i.onState = on
            }
        }
    override var simpleColor: Color
        get() = super.simpleColor
        set(simpleColor) {
            super.simpleColor = simpleColor
            for (i in ledStrips) {
                i.simpleColor = simpleColor
            }
        }
    override var currentSeq: ColorSequence?
        get() = super.currentSeq
        set(simpleColor) {
            super.currentSeq = currentSeq
            for (i in ledStrips) {
                i.currentSeq = currentSeq
            }
        }

    override fun setBrightnessExponential(exponentialInput: Int) {
        for (i in ledStrips) {
            i.setBrightnessExponential(brightness)
        }
        super.setBrightnessExponential(exponentialInput)
    }
}