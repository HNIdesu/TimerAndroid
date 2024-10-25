package com.hnidesu.timer.component

import android.graphics.drawable.Drawable

class AppItem(
    var packageName: String,
    var appName: String,
    var version: String,
    var icon: Drawable,
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
