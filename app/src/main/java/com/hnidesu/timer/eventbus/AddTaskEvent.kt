package com.hnidesu.timer.eventbus

class AddTaskEvent(
    val packageName: String,
    val timeoutInSeconds: Long
)