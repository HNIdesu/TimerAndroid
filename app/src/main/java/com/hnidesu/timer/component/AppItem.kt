package com.hnidesu.timer.component

data class AppItem(
    var packageName: String,
    var appName: String,
    var version: String,
    var deadline: Long,
    var status: TaskStatus
)
