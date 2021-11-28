package net.shadowxcraft.smartlights

object ControllerManager {
    val controllerAddrMap = HashMap<String, ESP32>() // map addr to controller
    val controllerIDMap = HashMap<Int, ESP32>() // map addr to controller
    val controllers = ArrayList<ESP32>()

    fun addController(controller: ESP32) {
        if (controllerAddrMap.containsKey(controller.addr))
            throw IllegalStateException("Added controller that already exists")
        controllers.add(controller)
        controllerAddrMap[controller.addr] = controller
        controllerIDMap[controller.dbId] = controller
    }

    fun connectAll() {
        for (i in controllers) {
            i.checkConnection()
        }
    }

    fun getLEDStripByID(uuid: String) : LEDStrip? {
        for (controller in controllers) {
            val strip = controller.getLEDStrip(uuid)
            if (strip != null)
                return strip
        }
        return null
    }
}