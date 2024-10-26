package com.hnidesu.timer.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AppRecordEntity::class], version = 1)
abstract class MyDatabase: RoomDatabase() {
    abstract fun getAppRecordDao(): AppInfoDao
}