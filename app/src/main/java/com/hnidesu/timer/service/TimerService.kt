package com.hnidesu.timer.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hnidesu.timer.component.TimerTask
import com.hnidesu.timer.eventbus.AddTaskEvent
import com.hnidesu.timer.eventbus.CancelTaskEvent
import com.hnidesu.timer.eventbus.ListTaskEvent
import com.hnidesu.timer.eventbus.TaskStatusEvent
import com.topjohnwu.superuser.Shell
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class TimerService : Service() {
    private val mTaskList: HashMap<String, TimerTask> = HashMap()
    private var mScheduledExecutorService: ScheduledExecutorService? = null

    @Subscribe(threadMode = ThreadMode.POSTING)
    fun onMessageEvent(event: Any?) {
        when(event){
            is AddTaskEvent->{
                val scheduledExecutorService=mScheduledExecutorService?:return
                if (mTaskList.containsKey(event.packageName)) {
                    mTaskList.get(event.packageName)!!.also { timerTask->
                        if(!timerTask.scheduledFuture.isDone)
                            timerTask.scheduledFuture.cancel(true)
                    }
                }
                val timeToStop = System.currentTimeMillis() + (event.timeoutInSeconds * 1000)
                val timerTask = TimerTask(event.packageName, timeToStop,scheduledExecutorService.schedule(
                {
                    Shell.cmd(String.format("am force-stop %s", event.packageName)).exec()
                    EventBus.getDefault().post(TaskStatusEvent(event.packageName, TaskStatusEvent.Status.Completed, timeToStop))
                }, event.timeoutInSeconds, TimeUnit.SECONDS))
                mTaskList.put(event.packageName, timerTask)
            }
            is TaskStatusEvent->{
                if(event.status==TaskStatusEvent.Status.Completed)
                    mTaskList.remove(event.packageName)
            }
            is CancelTaskEvent->{
                if (mTaskList.containsKey(event.packageName)) {
                    mTaskList.get(event.packageName)!!.also { timerTask->
                        if(!timerTask.scheduledFuture.isDone){
                            timerTask.scheduledFuture.cancel(true)
                            EventBus.getDefault().post(TaskStatusEvent(event.packageName, TaskStatusEvent.Status.Canceled, timerTask.endTimeMs))
                        }
                        mTaskList.remove(event.packageName)
                    }
                }
            }
            is ListTaskEvent->{
                for (entry in mTaskList.entries) {
                    EventBus.getDefault().post(TaskStatusEvent(entry.key, TaskStatusEvent.Status.Running, entry.value.endTimeMs))
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        mTaskList.clear()
        mScheduledExecutorService?.shutdown()
        EventBus.getDefault().unregister(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        throw NotImplementedError()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNELID,
                "Timer Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager.createNotificationChannel(channel)
        }
        startForeground(NOTIFICATIONID, NotificationCompat.Builder(this, CHANNELID).build())
        mScheduledExecutorService = Executors.newScheduledThreadPool(0)
        EventBus.getDefault().register(this)
    }

    companion object {
        val CHANNELID: String = "hnidesu_timer"
        val NOTIFICATIONID: Int = 3375
    }
}
