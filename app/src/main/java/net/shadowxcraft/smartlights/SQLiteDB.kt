package net.shadowxcraft.smartlights

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val DATABASE_VERSION = 2
const val DATABASE_NAME = "sample_database"

class SQLiteDB(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase?) {

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

    }
}