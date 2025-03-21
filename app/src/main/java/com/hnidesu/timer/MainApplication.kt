package com.hnidesu.timer

import android.app.Application
import com.hnidesu.timer.manager.AppPrefManager
import com.topjohnwu.superuser.Shell

class MainApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        AppPrefManager.init(this)
        Shell.setDefaultBuilder(Shell.Builder.create().setSuCommand(AppPrefManager.getSuCommand()))
    }
}