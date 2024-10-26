package com.hnidesu.timer.component

data class AppItem(
    var packageName: String,
    var appName: String,
    var version: String,
    var deadline: Long,
    var status:Status
){
    enum class Status{
        None,
        Running,
        Canceled,
        Completed,
        Error
    }
}
