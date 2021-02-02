package net.shadowxcraft.smartlights

import android.provider.BaseColumns

object SQLTableData {
    const val SQL_CREATE_CONTROLLER_TABLE = "CREATE TABLE `${ControllerEntry.TABLE_NAME}` (" +
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT," +
            "`${ControllerEntry.COLUMN_NAME_NAME}` TEXT," +
            "`${ControllerEntry.COLUMN_NAME_BLE_ADDR}` TEXT)"
    // Eventually, there will also be a ip address column

    object ControllerEntry {
        const val TABLE_NAME = "controllers"
        const val COLUMN_NAME_NAME = "name"
        const val COLUMN_NAME_BLE_ADDR = "ble_address"
    }

    const val SQL_CREATE_LEDSTRIP_TABLE = "CREATE TABLE `${LEDStripEntry.TABLE_NAME}` (" +
            "`uuid` TEXT PRIMARY KEY," +
            "`${LEDStripEntry.COLUMN_NAME_NAME}` TEXT," +
            "`${LEDStripEntry.COLUMN_NAME_CUR_SEQ}` TEXT," +
            "`${LEDStripEntry.COLUMN_NAME_ON_STATE}` INTEGER," +
            "`${LEDStripEntry.COLUMN_NAME_BRIGHTNESS}` INTEGER," +
            "`${LEDStripEntry.COLUMN_NAME_RGB}` INTEGER," +
            "`${LEDStripEntry.COLUMN_NAME_CONTROLLER}` INTEGER)"

    object LEDStripEntry {
        const val TABLE_NAME = "led_strips"
        const val COLUMN_NAME_NAME = "name"
        const val COLUMN_NAME_CUR_SEQ = "current_sequence"
        const val COLUMN_NAME_ON_STATE = "on_state"
        const val COLUMN_NAME_BRIGHTNESS = "brightness"
        const val COLUMN_NAME_RGB = "rgba_color"
        const val COLUMN_NAME_CONTROLLER = "controller_id"
    }


    const val SQL_CREATE_LEDSTRIP_COMPONENT_TABLE = "CREATE TABLE `${LEDStripComponentEntry.TABLE_NAME}` (" +
            "`${LEDStripComponentEntry.COLUMN_NAME_LED_STRIP_ID}` TEXT," +
            "`${LEDStripComponentEntry.COLUMN_NAME_RGB}` INTEGER," +
            "`${LEDStripComponentEntry.COLUMN_NAME_DRIVER_ID}` INTEGER," +
            "`${LEDStripComponentEntry.COLUMN_NAME_DRIVER_PIN}` INTEGER)"


    object LEDStripComponentEntry {
        const val TABLE_NAME = "led_strip_components"
        const val COLUMN_NAME_LED_STRIP_ID = "led_strip_id"
        const val COLUMN_NAME_RGB = "rgb_color"
        const val COLUMN_NAME_DRIVER_ID = "driver_id"
        const val COLUMN_NAME_DRIVER_PIN = "pin"
    }

    const val SQL_CREATE_LEDSTRIP_GROUP_TABLE = "CREATE TABLE `${LEDStripGroupEntry.TABLE_NAME}` (" +
            "`uuid` TEXT PRIMARY KEY," +
            "`${LEDStripGroupEntry.COLUMN_NAME_NAME}` TEXT," +
            "`${LEDStripGroupEntry.COLUMN_NAME_CUR_SEQ}` TEXT," +
            "`${LEDStripGroupEntry.COLUMN_NAME_ON_STATE}` INTEGER," +
            "`${LEDStripGroupEntry.COLUMN_NAME_BRIGHTNESS}` INTEGER," +
            "`${LEDStripGroupEntry.COLUMN_NAME_RGB}` INTEGER," +
            "`${LEDStripGroupEntry.COLUMN_NAME_CONTROLLER}` INTEGER)"

    object LEDStripGroupEntry {
        const val TABLE_NAME = "led_strip_groups"
        const val COLUMN_NAME_NAME = "name"
        const val COLUMN_NAME_CUR_SEQ = "current_sequence"
        const val COLUMN_NAME_ON_STATE = "on_state"
        const val COLUMN_NAME_BRIGHTNESS = "brightness"
        const val COLUMN_NAME_RGB = "rgba_color"
        const val COLUMN_NAME_CONTROLLER = "controller_id"
    }

    const val SQL_CREATE_LEDSTRIP_GROUP_ITEM_TABLE = "CREATE TABLE `${LEDStripGroupItemEntry.TABLE_NAME}` (" +
            "`${LEDStripGroupItemEntry.COLUMN_NAME_LED_STRIP_GROUP}` TEXT," +
            "`${LEDStripGroupItemEntry.COLUMN_NAME_LED_STRIP}` TEXT)"

    object LEDStripGroupItemEntry {
        const val TABLE_NAME = "led_strip_group_components"
        const val COLUMN_NAME_LED_STRIP_GROUP = "group"
        const val COLUMN_NAME_LED_STRIP = "strip"
    }

    const val SQL_CREATE_SCHEDULED_CHANGE_TABLE = "CREATE TABLE `${ScheduledChangeEntry.TABLE_NAME}` (" +
            "`${ScheduledChangeEntry.COLUMN_NAME_NAME}` TEXT," +
            "`${ScheduledChangeEntry.COLUMN_NAME_STRIP_ID}` TEXT," +
            "`${ScheduledChangeEntry.COLUMN_NAME_YEAR}` INTEGER," +
            "`${ScheduledChangeEntry.COLUMN_NAME_MONTH}` INTEGER," +
            "`${ScheduledChangeEntry.COLUMN_NAME_DAY}` INTEGER," +
            "`${ScheduledChangeEntry.COLUMN_NAME_HOUR}` INTEGER," +
            "`${ScheduledChangeEntry.COLUMN_NAME_MINUTE}` INTEGER," +
            "`${ScheduledChangeEntry.COLUMN_NAME_IS_SPECIFIC_DATE}` INTEGER," +
            "`${ScheduledChangeEntry.COLUMN_NAME_DAYS_OF_WEEK}` INTEGER," +
            "`${ScheduledChangeEntry.COLUMN_NAME_SECONDS_UNTIL_OFF}` INTEGER," +
            "`${ScheduledChangeEntry.COLUMN_NAME_TURN_ON}` INTEGER," +
            "`${ScheduledChangeEntry.COLUMN_NAME_NEW_BRIGHTNESS}` INTEGER," +
            "`${ScheduledChangeEntry.COLUMN_NAME_NEW_RGB}` INTEGER," +
            "`${ScheduledChangeEntry.COLUMN_NAME_NEW_COLOR_SEQUENCE_ID}` TEXT," +
            "`${ScheduledChangeEntry.COLUMN_NAME_REPEAT_INTERVAL}` INTEGER)"

    object ScheduledChangeEntry {
        const val TABLE_NAME = "scheduled_change"
        const val COLUMN_NAME_NAME = "name"
        const val COLUMN_NAME_STRIP_ID = "strip_id" // or group
        const val COLUMN_NAME_YEAR = "year"
        const val COLUMN_NAME_MONTH = "month"
        const val COLUMN_NAME_DAY = "day"
        const val COLUMN_NAME_HOUR = "hour"
        const val COLUMN_NAME_MINUTE = "minute"
        const val COLUMN_NAME_IS_SPECIFIC_DATE = "is_specific_date"
        const val COLUMN_NAME_DAYS_OF_WEEK = "days_of_week"
        const val COLUMN_NAME_REPEAT_INTERVAL = "repeat_interval"
        const val COLUMN_NAME_SECONDS_UNTIL_OFF = "sec_until_off"
        const val COLUMN_NAME_TURN_ON = "turn_on"
        const val COLUMN_NAME_NEW_BRIGHTNESS = "new_brightness"
        const val COLUMN_NAME_NEW_RGB = "new_rgba_color"
        const val COLUMN_NAME_NEW_COLOR_SEQUENCE_ID = "new_sequence"
    }
}