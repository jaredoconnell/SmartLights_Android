package net.shadowxcraft.smartlights

import android.provider.BaseColumns

object SQLTableData {
    const val SQL_CREATE_CONTROLLER_TABLE = "CREATE TABLE `${ControllerEntry.TABLE_NAME}` (" +
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT," +
            "`${ControllerEntry.COLUMN_NAME_NAME}` TEXT NOT NULL," +
            "`${ControllerEntry.COLUMN_NAME_BLE_ADDR}` TEXT NOT NULL)"
    // Eventually, there will also be a ip address column

    object ControllerEntry {
        const val TABLE_NAME = "controllers"
        const val COLUMN_NAME_NAME = "name"
        const val COLUMN_NAME_BLE_ADDR = "ble_address"
    }

    const val SQL_CREATE_PWM_DRIVER_TABLE = "CREATE TABLE `${PWMDriverEntry.TABLE_NAME}` (" +
            "`${PWMDriverEntry.COLUMN_NAME_CONTROLLER_ID}` INTEGER NOT NULL," +
            "`${PWMDriverEntry.COLUMN_NAME_ADDRESS}` INTEGER NOT NULL," +
            "PRIMARY KEY(${PWMDriverEntry.COLUMN_NAME_CONTROLLER_ID}," +
            "${PWMDriverEntry.COLUMN_NAME_ADDRESS}))"

    object PWMDriverEntry {
        const val TABLE_NAME = "pwm_drivers"
        const val COLUMN_NAME_CONTROLLER_ID = "controller_id"
        const val COLUMN_NAME_ADDRESS = "address"
    }

    const val SQL_CREATE_LEDSTRIP_TABLE = "CREATE TABLE `${LEDStripEntry.TABLE_NAME}` (" +
            "`uuid` TEXT PRIMARY KEY NOT NULL," +
            "`${LEDStripEntry.COLUMN_NAME_NAME}` TEXT NOT NULL," +
            "`${LEDStripEntry.COLUMN_NAME_CUR_SEQ}` TEXT," +
            "`${LEDStripEntry.COLUMN_NAME_ON_STATE}` INTEGER NOT NULL," +
            "`${LEDStripEntry.COLUMN_NAME_BRIGHTNESS}` INTEGER NOT NULL," +
            "`${LEDStripEntry.COLUMN_NAME_RGB}` INTEGER NOT NULL," +
            "`${LEDStripEntry.COLUMN_NAME_CONTROLLER}` INTEGER NOT NULL)"

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
            "`${LEDStripComponentEntry.COLUMN_NAME_LED_STRIP_ID}` TEXT NOT NULL," +
            "`${LEDStripComponentEntry.COLUMN_NAME_RGB}` INTEGER NOT NULL," +
            "`${LEDStripComponentEntry.COLUMN_NAME_DRIVER_ID}` INTEGER NOT NULL," +
            "`${LEDStripComponentEntry.COLUMN_NAME_DRIVER_PIN}` INTEGER NOT NULL," +
            "PRIMARY KEY(${LEDStripComponentEntry.COLUMN_NAME_LED_STRIP_ID}," +
            "${LEDStripComponentEntry.COLUMN_NAME_DRIVER_ID}," +
            "${LEDStripComponentEntry.COLUMN_NAME_DRIVER_PIN}))"


    object LEDStripComponentEntry {
        const val TABLE_NAME = "led_strip_components"
        const val COLUMN_NAME_LED_STRIP_ID = "led_strip_id"
        const val COLUMN_NAME_RGB = "rgb_color"
        const val COLUMN_NAME_DRIVER_ID = "driver_id"
        const val COLUMN_NAME_DRIVER_PIN = "pin"
    }

    const val SQL_CREATE_LEDSTRIP_DISPLAY_OPTIONS_TABLE = "CREATE TABLE `${LEDStripDisplayOptionsEntry.TABLE_NAME}` (" +
            "`${LEDStripDisplayOptionsEntry.COLUMN_NAME_LEDSTRIP_ID}` TEXT NOT NULL," +
            "`${LEDStripDisplayOptionsEntry.COLUMN_NAME_POSITION}` INTEGER PRIMARY KEY NOT NULL)"

    object LEDStripDisplayOptionsEntry {
        const val TABLE_NAME = "led_strip_display_options"
        const val COLUMN_NAME_LEDSTRIP_ID = "id"
        const val COLUMN_NAME_POSITION = "position"
    }

    const val SQL_CREATE_LEDSTRIP_GROUP_DISPLAY_OPTIONS_TABLE = "CREATE TABLE `${LEDStripGroupDisplayOptionsEntry.TABLE_NAME}` (" +
            "`${LEDStripGroupDisplayOptionsEntry.COLUMN_NAME_LEDSTRIP_ID}` TEXT NOT NULL," +
            "`${LEDStripGroupDisplayOptionsEntry.COLUMN_NAME_POSITION}` INTEGER PRIMARY KEY NOT NULL)"

    object LEDStripGroupDisplayOptionsEntry {
        const val TABLE_NAME = "led_strip_group_display_options"
        const val COLUMN_NAME_LEDSTRIP_ID = "id"
        const val COLUMN_NAME_POSITION = "position"
    }

    const val SQL_CREATE_COLOR_SEQUENCE_DISPLAY_OPTIONS_TABLE = "CREATE TABLE `${ColorSequenceDisplayOptionsEntry.TABLE_NAME}` (" +
            "`${ColorSequenceDisplayOptionsEntry.COLUMN_NAME_COLOR_SEQUENCE_ID}` TEXT NOT NULL," +
                    "`${ColorSequenceDisplayOptionsEntry.COLUMN_NAME_POSITION}` INTEGER PRIMARY KEY NOT NULL)"

    object ColorSequenceDisplayOptionsEntry {
        const val TABLE_NAME = "color_sequence_display_options"
        const val COLUMN_NAME_COLOR_SEQUENCE_ID = "id"
        const val COLUMN_NAME_POSITION = "position"

    }

    const val SQL_CREATE_LEDSTRIP_GROUP_TABLE = "CREATE TABLE `${LEDStripGroupEntry.TABLE_NAME}` (" +
            "`uuid` TEXT PRIMARY KEY NOT NULL," +
            "`${LEDStripGroupEntry.COLUMN_NAME_NAME}` TEXT NOT NULL," +
            "`${LEDStripGroupEntry.COLUMN_NAME_CUR_SEQ}` TEXT," +
            "`${LEDStripGroupEntry.COLUMN_NAME_ON_STATE}` INTEGER," +
            "`${LEDStripGroupEntry.COLUMN_NAME_BRIGHTNESS}` INTEGER," +
            "`${LEDStripGroupEntry.COLUMN_NAME_RGB}` INTEGER," +
            "`${LEDStripGroupEntry.COLUMN_NAME_CONTROLLER}` INTEGER NOT NULL)"

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
            "`${LEDStripGroupItemEntry.COLUMN_NAME_LED_STRIP_GROUP}` TEXT NOT NULL," +
            "`${LEDStripGroupItemEntry.COLUMN_NAME_LED_STRIP}` TEXT NOT NULL," +
            "PRIMARY KEY(${LEDStripGroupItemEntry.COLUMN_NAME_LED_STRIP_GROUP}," +
            "${LEDStripGroupItemEntry.COLUMN_NAME_LED_STRIP}))"

    object LEDStripGroupItemEntry {
        const val TABLE_NAME = "led_strip_group_components"
        const val COLUMN_NAME_LED_STRIP_GROUP = "led_strip_group"
        const val COLUMN_NAME_LED_STRIP = "led_strip"
    }

    const val SQL_CREATE_SCHEDULED_CHANGE_TABLE = "CREATE TABLE `${ScheduledChangeEntry.TABLE_NAME}` (" +
            "`${ScheduledChangeEntry.COLUMN_NAME_NAME}` TEXT NOT NULL," +
            "`${ScheduledChangeEntry.COLUMN_NAME_STRIP_ID}` TEXT," +
            "`${ScheduledChangeEntry.COLUMN_NAME_YEAR}` INTEGER NOT NULL," +
            "`${ScheduledChangeEntry.COLUMN_NAME_MONTH}` INTEGER NOT NULL," +
            "`${ScheduledChangeEntry.COLUMN_NAME_DAY}` INTEGER NOT NULL," +
            "`${ScheduledChangeEntry.COLUMN_NAME_HOUR}` INTEGER NOT NULL," +
            "`${ScheduledChangeEntry.COLUMN_NAME_MINUTE}` INTEGER NOT NULL," +
            "`${ScheduledChangeEntry.COLUMN_NAME_IS_SPECIFIC_DATE}` INTEGER NOT NULL," +
            "`${ScheduledChangeEntry.COLUMN_NAME_DAYS_OF_WEEK}` INTEGER," +
            "`${ScheduledChangeEntry.COLUMN_NAME_SECONDS_UNTIL_OFF}` INTEGER," +
            "`${ScheduledChangeEntry.COLUMN_NAME_TURN_ON}` INTEGER NOT NULL," +
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

    const val SQL_CREATE_COLOR_SEQUENCE_TABLE = "CREATE TABLE `${ColorSequenceEntry.TABLE_NAME}` (" +
            "`uuid` TEXT PRIMARY KEY," +
            "`${ColorSequenceEntry.COLUMN_NAME_NAME}` TEXT NOT NULL," +
            "`${ColorSequenceEntry.COLUMN_NAME_SEQUENCE_TYPE}` INTEGER NOT NULL," +
            "`${ColorSequenceEntry.COLUMN_NAME_SUSTAIN_TIME}` INTEGER NOT NULL," +
            "`${ColorSequenceEntry.COLUMN_NAME_TRANSITION_TIME}` INTEGER NOT NULL," +
            "`${ColorSequenceEntry.COLUMN_NAME_TRANSITION_TYPE}` INTEGER NOT NULL)"

    object ColorSequenceEntry {
        const val TABLE_NAME = "color_sequences"
        const val COLUMN_NAME_NAME = "name"
        const val COLUMN_NAME_SEQUENCE_TYPE = "type"
        const val COLUMN_NAME_SUSTAIN_TIME = "sustain_time"
        const val COLUMN_NAME_TRANSITION_TIME = "transition_time"
        const val COLUMN_NAME_TRANSITION_TYPE = "transition_type"
    }

    const val SQL_CREATE_COLOR_SEQUENCE_COLORS_TABLE = "CREATE TABLE" +
            "`${ColorSequenceColorEntry.TABLE_NAME}` (" +
            "`${ColorSequenceColorEntry.COLUMN_NAME_SEQUENCE_ID}` TEXT NOT NULL," +
            "`${ColorSequenceColorEntry.COLUMN_NAME_ORDER_INDEX}` INTEGER NOT NULL," +
            "`${ColorSequenceColorEntry.COLUMN_NAME_COLOR_ARGB}` INTEGER NOT NULL)"

    object ColorSequenceColorEntry {
        const val TABLE_NAME = "color_sequence_colors"
        const val COLUMN_NAME_SEQUENCE_ID = "sequence_id"
        const val COLUMN_NAME_ORDER_INDEX = "order_index"
        const val COLUMN_NAME_COLOR_ARGB = "color_argb"
    }
}