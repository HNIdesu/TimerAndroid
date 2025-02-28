package com.hnidesu.timer.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class Timer {
    private var mJob: Job? = null
    private var mTask: (() -> Unit)? = null

    fun schedule(intervalMillis: Long,task:()->Unit){
        mJob?.cancel()
        mTask = task
        mJob = CoroutineScope(Dispatchers.Default).launch {
            try{
                while(true){
                    task()
                    delay(intervalMillis)
                }
            }catch (_:CancellationException){
            }
        }
    }

    fun cancel(){
        mJob?.cancel()
        mTask = null
        mTask = null
    }
}