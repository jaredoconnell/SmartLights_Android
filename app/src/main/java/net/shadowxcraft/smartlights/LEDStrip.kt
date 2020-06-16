package net.shadowxcraft.smartlights

class LEDStrip(val id: Int, val name: String, val components: ArrayList<LEDStripComponent>,
               var currentSeq: ColorSequence?, val controller: ESP32) {
    var brightness = 4095
}