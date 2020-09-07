package net.shadowxcraft.smartlights

import java.util.*
import kotlin.collections.HashMap

class PWMDriver(val i2cAddress: Int) : PinDriver {
    var pins: HashMap<String, Int> = HashMap()

    init {
        for (x in 0..15) {
            pins[x.toString()] = x
        }
    }

    override fun getAllPins() : Map<String, Int> {
        return pins;
    }

    override fun getAddress(): Int {
        return i2cAddress
    }

    override fun toString(): String {
        return i2cAddress.toString()
    }
}