package net.shadowxcraft.smartlights.packets

import net.shadowxcraft.smartlights.*

class SetSettingPacket(controller: ESP32, private val setting: ControllerSetting) : SendablePacket(controller) {
    override fun send() {
        val output = ArrayList<Byte>();
        output.add(18) // packet ID 18

        output.addAll(strToByteList(setting.settingName))
        if (setting.isString) {
            output.add(1)
            output.addAll(strToByteList(setting.strVal))
        } else {
            output.add(0)
            output.addAll(shortToByteList(setting.intVal))
        }
        sendData(output.toByteArray())
    }
}