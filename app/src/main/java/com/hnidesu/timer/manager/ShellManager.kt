package com.hnidesu.timer.manager

import com.hnidesu.timer.shell.IShell
import com.hnidesu.timer.shell.MagiskShellAdapter
import com.hnidesu.timer.shell.ShizukuShellAdapter

object ShellManager {
    fun getShell(provider: String?): IShell? {
        return when(provider){
            "shizuku"-> ShizukuShellAdapter()
            "magisk" -> MagiskShellAdapter()
            else -> null
        }
    }
}