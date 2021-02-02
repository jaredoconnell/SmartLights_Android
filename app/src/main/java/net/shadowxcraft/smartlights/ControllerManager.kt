package net.shadowxcraft.smartlights

object ControllerManager {
    val controllerMap = HashMap<String, ESP32>() // map addr to controller
    val controllers = ArrayList<ESP32>()

    fun addController(controller: ESP32) {
        if (controllerMap.containsKey(controller.addr))
            throw IllegalStateException("Added controller that already exists")
        controllers.add(controller)
        controllerMap[controller.addr] = controller
    }

    fun connectAll() {
        for (i in controllers) {
            i.checkConnection()
        }
    }
}