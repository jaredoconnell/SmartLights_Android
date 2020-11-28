package net.shadowxcraft.smartlights

class ControllerSetting private constructor(
    val settingName: String, val strVal: String, val intVal: Int, val isString: Boolean)
{
    constructor(settingName: String, strVal: String)
            : this(settingName, strVal, 0, true)
    {}
    constructor(settingName: String, intVal: Int)
            : this(settingName, "", intVal, false)
    {}

}