package com.hnidesu.timer.shell

import rikka.shizuku.Shizuku

class ShizukuShellAdapter:IShell {
    override fun execute(cmd: Array<String>) {
        Shizuku.newProcess(cmd,null,null)
    }
}