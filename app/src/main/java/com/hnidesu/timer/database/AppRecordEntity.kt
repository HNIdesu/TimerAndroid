package com.hnidesu.timer.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "records")
data class AppRecordEntity(
    @PrimaryKey(autoGenerate = false)
    var packageName: String,
    @ColumnInfo
    var lastAccessTime: Long
)