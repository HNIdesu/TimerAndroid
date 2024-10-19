package com.hnidesu.timer.component

import java.util.concurrent.ScheduledFuture

class TimerTask {
    var endTimeMs: Long = 0
    var packageName: String? = null
    var scheduledFuture: ScheduledFuture<*>? = null
}
