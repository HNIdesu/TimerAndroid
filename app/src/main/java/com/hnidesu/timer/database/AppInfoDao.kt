package com.hnidesu.timer.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AppInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: AppRecordEntity)
    @Delete
    suspend fun delete(entity: AppRecordEntity)
    @Query("SELECT * FROM records")
    suspend fun getAll(): List<AppRecordEntity>
    @Query("SELECT * FROM records WHERE packageName = :packageName")
    suspend fun find(packageName: String): AppRecordEntity?
}