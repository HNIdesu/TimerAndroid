package com.hnidesu.timer.manager

import android.content.Context
import android.content.SharedPreferences

object AppPrefManager {
    private lateinit var mShedPreferences:SharedPreferences
    fun init(context: Context){
        mShedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }
    fun getSuCommand():String = mShedPreferences.getString("su_command", "su")!!
    fun setSuCommand(suCommand:String){
        mShedPreferences.edit().putString("su_command", suCommand).apply()
    }
    fun getShowSystemApps():Boolean = mShedPreferences.getBoolean("show_system_apps", false)
    fun setShowSystemApps(showSystemApps:Boolean){
        mShedPreferences.edit().putBoolean("show_system_apps", showSystemApps).apply()
    }
    fun getAutoShutdown():Boolean = mShedPreferences.getBoolean("auto_shutdown", false)
    fun setAutoShutdown(autoShutdown:Boolean){
        mShedPreferences.edit().putBoolean("auto_shutdown", autoShutdown).apply()
    }
    fun getTimeout():Long = mShedPreferences.getLong("timeout", 300)
    fun setTimeout(timeout:Long){
        mShedPreferences.edit().putLong("timeout", timeout).apply()
    }
}