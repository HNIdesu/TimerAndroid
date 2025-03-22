package com.hnidesu.timer.shell

import com.topjohnwu.superuser.Shell

class MagiskShellAdapter:IShell {
    override fun execute(cmd: Array<String>) {
        Shell.cmd(cmd.joinToString(" ")).exec()
    }
}