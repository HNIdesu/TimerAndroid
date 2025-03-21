package com.hnidesu.timer.shell

import com.topjohnwu.superuser.Shell
import rikka.shizuku.Shizuku

object ShellManager {
    fun getShell(): IShell? {
        return when{
            Shizuku.pingBinder() -> return object : IShell {
                override fun execute(cmd: Array<String>) {
                    Shizuku.newProcess(cmd, null, null)
                }
            }

            Shell.getShell().isRoot -> {
                return object : IShell {
                    override fun execute(cmd: Array<String>) {
                        Shell.cmd(*cmd).exec()
                    }
                }
            }
            else -> null
        }
    }
}