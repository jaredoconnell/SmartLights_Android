package net.shadowxcraft.smartlights

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQLTableData.SQL_CREATE_CONTROLLER_TABLE)
        db.execSQL(SQLTableData.SQL_CREATE_LEDSTRIP_TABLE)
        db.execSQL(SQLTableData.SQL_CREATE_LEDSTRIP_COMPONENT_TABLE)
        db.execSQL(SQLTableData.SQL_CREATE_LEDSTRIP_GROUP_TABLE)
        db.execSQL(SQLTableData.SQL_CREATE_LEDSTRIP_GROUP_ITEM_TABLE)
        db.execSQL(SQLTableData.SQL_CREATE_SCHEDULED_CHANGE_TABLE)
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // No upgrades known right now
    }
    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }
    companion object {
        // If you change the database schema, you must increment the database version.
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "SmartLightsDB.db"
    }

}