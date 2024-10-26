package com.hnidesu.timer.manager

import android.content.Context
import androidx.room.Room
import com.hnidesu.timer.database.MyDatabase

object DatabaseManager {
    fun getMyDatabase(context: Context): MyDatabase {
        return Room.databaseBuilder(context, MyDatabase::class.java, "database").build()
    }
}