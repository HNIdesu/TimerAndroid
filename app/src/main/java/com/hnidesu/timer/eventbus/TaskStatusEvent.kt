package com.hnidesu.timer.eventbus

class TaskStatusEvent(
    val packageName: String,
    val status: Status,
    val endTimeMs: Long,
    val error: Exception?=null
) {
    enum class Status{
        Canceled,Running,Completed,Error
    }
}