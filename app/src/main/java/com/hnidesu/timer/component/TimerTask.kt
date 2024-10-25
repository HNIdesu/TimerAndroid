package com.hnidesu.timer.component

import java.util.concurrent.ScheduledFuture

class TimerTask(
    var packageName:String,
    var endTimeMs:Long,
    var scheduledFuture:ScheduledFuture<*>
)
