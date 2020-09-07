package net.shadowxcraft.smartlights

import java.util.*

interface PinDriver {
    fun getAllPins() : Map<String, Int> // pin name, pin index

    fun getAddress() : Int

    override fun toString() : String
}