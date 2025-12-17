package com.cogniter.watchaccuracychecker.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.cogniter.watchaccuracychecker.model.ListItem
import com.cogniter.watchaccuracychecker.model.Subitem

class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "MyDatabase"
        private const val TABLE_ITEMS = "Items"
        private const val TABLE_SUBITEMS = "SubItems"
        private const val KEY_ID = "id"
        private const val KEY_TITLE = "title"
        private const val KEY_WATCH_IMAGE = "watch_image"
        private const val KEY_ADDED_WATCH_TIME = "added_watch_time"
        private const val KEY_IS_WATCH_RUNNING = "is_watch_running"
        private const val KEY_ITEM_ID = "item_id"
        private const val KEY_SUBITEM_ID = "subitem_id"
        private const val KEY_NAME = "name"
        private const val KEY_IMAGE = "image"
        private const val KEY_DATE = "date"

        private const val WATCH_TABLE_ITEMS = "WatchItems"
        private const val WATCH_TABLE_ID = "WatchItemsId"
        private const val KEY_WATCH_NAME = "watchName"
        private const val KEY_WATCH_ID = "watchID"
        private const val TABLE_NAME = "boolean_table"
        private const val KEY_VALUE = "value"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createItemsTableQuery = "CREATE TABLE $TABLE_ITEMS ($KEY_ID INTEGER PRIMARY KEY, $KEY_TITLE TEXT, $KEY_WATCH_IMAGE TEXT, $KEY_ADDED_WATCH_TIME TEXT, $KEY_IS_WATCH_RUNNING TEXT)"
        val createSubItemsTableQuery = "CREATE TABLE $TABLE_SUBITEMS ($KEY_ID INTEGER PRIMARY KEY, $KEY_ITEM_ID INTEGER, $KEY_SUBITEM_ID INTEGER,$KEY_NAME TEXT, $KEY_IMAGE TEXT, $KEY_DATE TEXT)"
        val createTableQuery = "CREATE TABLE $WATCH_TABLE_ITEMS ($KEY_ID INTEGER PRIMARY KEY, $KEY_WATCH_NAME TEXT)"
        val createTableQueryId = "CREATE TABLE $WATCH_TABLE_ID ($KEY_ID INTEGER PRIMARY KEY, $KEY_WATCH_ID   TEXT)"
        val createTable = ("CREATE TABLE " + TABLE_NAME + "(" + KEY_ID + " INTEGER PRIMARY KEY," + KEY_VALUE + " INTEGER" + ")")
        db.execSQL(createTable)
        db.execSQL(createTableQuery)
        db.execSQL(createTableQueryId)
        db.execSQL(createItemsTableQuery)
        db.execSQL(createSubItemsTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ITEMS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SUBITEMS")
        db.execSQL("DROP TABLE IF EXISTS $WATCH_TABLE_ITEMS")
        db.execSQL("DROP TABLE IF EXISTS $WATCH_TABLE_ID")
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME)
        onCreate(db)
    }
    fun addSubItem(itemId: Long, subItemid: Long, subItem: Subitem) {
        val db = this.writableDatabase
        val subItemValues = ContentValues()
        subItemValues.put(KEY_SUBITEM_ID, subItemid)
        subItemValues.put(KEY_ITEM_ID, itemId)
        subItemValues.put(KEY_NAME, subItem.name)
        subItemValues.put(KEY_IMAGE, subItem.image)
        subItemValues.put(KEY_DATE, subItem.date)
        db.insert(TABLE_SUBITEMS, null, subItemValues)
        db.close()
    }

    fun removeSubItem(subItemId: Long) {
        val db = this.writableDatabase
        db.delete(TABLE_SUBITEMS, "$KEY_SUBITEM_ID = ?", arrayOf(subItemId.toString()))
        db.close()
    }
    fun addItem(title: String, image: String, date: String,iswatchrunning: Boolean, subItems: List<Subitem>): Long {
        val db = this.writableDatabase
        val itemValues = ContentValues()
        itemValues.put(KEY_TITLE, title)
        itemValues.put(KEY_WATCH_IMAGE, image)
        itemValues.put(KEY_ADDED_WATCH_TIME,date)
        itemValues.put(KEY_IS_WATCH_RUNNING,iswatchrunning)
        val itemId = db.insert(TABLE_ITEMS, null, itemValues)

        for (subItem in subItems) {
            val subItemValues = ContentValues()
            subItemValues.put(KEY_ITEM_ID, itemId)
            subItemValues.put(KEY_NAME, subItem.name)
            subItemValues.put(KEY_IMAGE, subItem.image)
            subItemValues.put(KEY_DATE, subItem.date)
            db.insert(TABLE_SUBITEMS, null, subItemValues)
        }
        db.close()

        return itemId // Return the itemId
    }
    fun updateItemWatchRunning(itemId: Long, iswatchrunning: Boolean) {
        val db = this.writableDatabase
        val itemValues = ContentValues()
        itemValues.put(KEY_IS_WATCH_RUNNING, iswatchrunning)
        db.update(TABLE_ITEMS, itemValues, "$KEY_ID = ?", arrayOf(itemId.toString()))
        db.close()

    }
    fun updateItemTitle(itemId: Long, newTitle: String, newWatchImage: String): Int {
        val db = this.writableDatabase
        val itemValues = ContentValues()
        itemValues.put(KEY_TITLE, newTitle)
        itemValues.put(KEY_WATCH_IMAGE, newWatchImage)
        val rowsAffected = db.update(TABLE_ITEMS, itemValues, "$KEY_ID = ?", arrayOf(itemId.toString()))
        db.close()
        return rowsAffected
    }

    fun deleteItem(itemId: Long) {
        val db = this.writableDatabase
        db.delete(TABLE_ITEMS, "$KEY_ID = ?", arrayOf(itemId.toString()))
        db.delete(TABLE_SUBITEMS, "$KEY_ITEM_ID = ?", arrayOf(itemId.toString()))
        db.close()
    }
    fun getAllItems(): List<ListItem> {

        val itemList = mutableListOf<ListItem>()
        val selectQuery = "SELECT * FROM $TABLE_ITEMS"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val itemId = cursor.getLong(cursor.getColumnIndex(KEY_ID))
                val title = cursor.getString(cursor.getColumnIndex(KEY_TITLE))
                val watchimage = cursor.getString(cursor.getColumnIndex(KEY_WATCH_IMAGE))
                val addedWatchTime = cursor.getString(cursor.getColumnIndex(KEY_ADDED_WATCH_TIME))
                val columnIndex = cursor.getColumnIndex(KEY_IS_WATCH_RUNNING)
                var isWatchRunning = false
                if (columnIndex != -1) {
                    val isWatchRunningValue = cursor.getString(columnIndex)
                    isWatchRunning = isWatchRunningValue == "1"
                    // Now you can use the value of isWatchRunning as needed
                } else {
                    isWatchRunning = false
                    // Handle the case where the column is not found in the cursor
                }
                val subItems = getSubItemsForItem(itemId)
                itemList.add(ListItem(itemId, title,watchimage,addedWatchTime, isWatchRunning,subItems))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return itemList
    }

     fun getSubItemsForItem(itemId: Long): List<Subitem> {
        val subItemList = mutableListOf<Subitem>()
        val selectQuery = "SELECT * FROM $TABLE_SUBITEMS WHERE $KEY_ITEM_ID = $itemId"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val name = cursor.getString(cursor.getColumnIndex(KEY_NAME))
                val image = cursor.getString(cursor.getColumnIndex(KEY_IMAGE))
                val date = cursor.getString(cursor.getColumnIndex(KEY_DATE))
                val subitemId = cursor.getLong(cursor.getColumnIndex(KEY_SUBITEM_ID))

                subItemList.add(Subitem(subitemId,name, image,date))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return subItemList
    }

    fun setStringValue(value: String) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_WATCH_NAME, value)
        db.insert(WATCH_TABLE_ITEMS, null, values)
        db.close()
    }

    fun getStringValue(): String? {
        val selectQuery = "SELECT * FROM $WATCH_TABLE_ITEMS"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        var value: String? = null
        if (cursor.moveToFirst()) {
            value = cursor.getString(cursor.getColumnIndex(KEY_WATCH_NAME))
        }
        cursor.close()
        db.close()
        return value
    }

    fun deleteStringValue() {
        val db = this.writableDatabase
        db.delete(WATCH_TABLE_ITEMS, null, null)
        db.close()
    }

    fun setLongValue(value: Long) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_WATCH_ID, value)
        db.insert(WATCH_TABLE_ID, null, values)
        db.close()
    }

    fun getLongValue(): Long {
        val selectQuery = "SELECT * FROM $WATCH_TABLE_ID"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        var value: Long = 0
        if (cursor.moveToFirst()) {
            value = cursor.getLong(cursor.getColumnIndex(KEY_WATCH_ID))
        }
        cursor.close()
        db.close()
        return value
    }

    fun deleteLongValue() {
        val db = this.writableDatabase
        db.delete(WATCH_TABLE_ID, null, null)
        db.close()
    }
    fun getWatchNameForSubcategoryId(subitemId: Long): String? {
        val selectQuery = "SELECT $KEY_TITLE FROM $TABLE_ITEMS INNER JOIN $TABLE_SUBITEMS ON $TABLE_ITEMS.$KEY_ID = $TABLE_SUBITEMS.$KEY_ITEM_ID WHERE $TABLE_SUBITEMS.$KEY_SUBITEM_ID = $subitemId"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        var watchName: String? = null
        if (cursor.moveToFirst()) {
            watchName = cursor.getString(cursor.getColumnIndex(KEY_TITLE))
        }
        cursor.close()
        db.close()
        return watchName
    }
    fun getTitleFromItemId(itemId: Long): String? {
        val db = this.readableDatabase
        val cursor = db.query(TABLE_ITEMS, arrayOf(KEY_TITLE), "$KEY_ID = ?", arrayOf(itemId.toString()), null, null, null)

        var title: String? = null
        if (cursor.moveToFirst()) {
            title = cursor.getString(cursor.getColumnIndex(KEY_TITLE))
        }

        cursor.close()
        db.close()

        return title
    }
}
