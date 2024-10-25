package com.hnidesu.timer.manager

import android.content.Context
import android.content.SharedPreferences

object SettingManager {
    fun getDefault(context: Context):SharedPreferences {
        return context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }
}