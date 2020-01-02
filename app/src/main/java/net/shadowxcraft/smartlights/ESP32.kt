package net.shadowxcraft.smartlights

import android.util.SparseArray
import androidx.core.util.containsKey
import java.lang.IllegalStateException

class ESP32 {
    // Bluetooth stuff
    private val pwmControllers: SparseArray<PWMController> = SparseArray<PWMController>()

    /**
     * Adds a controller.
     * @throws IllegalStateException if a controller with the same address is there.
     */
    fun addPWMController(controller: PWMController) {
        if(pwmControllers.containsKey(controller.i2cAddress))
                throw IllegalStateException("Controller with same address already exists.")
        pwmControllers.append(controller.i2cAddress, controller)
    }
}