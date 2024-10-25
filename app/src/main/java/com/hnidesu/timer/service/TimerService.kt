package com.hnidesu.timer.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import androidx.core.app.NotificationCompat
import com.hnidesu.timer.component.TimerTask
import com.topjohnwu.superuser.Shell
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class TimerService : Service() {
    private var mHandler: MyHandler? = null
    private var mWorkingThread: HandlerThread? = null

    class MyHandler(looper: Looper) : Handler(looper) {
        private val mScheduledExecutorService: ScheduledExecutorService = Executors.newScheduledThreadPool(0)
        val taskList: HashMap<String?, TimerTask> = HashMap()

        override fun handleMessage(msg: Message) {
            if (msg.what == 3321) {
                val bundle = msg.obj as Bundle
                val timeout = bundle.getLong("timeout")
                val timeToStop = System.currentTimeMillis() + (timeout * 1000)
                val packageName = bundle.getString("package_name")
                synchronized(this.taskList) {
                    if (taskList.containsKey(packageName) && !taskList.get(packageName)!!.scheduledFuture!!.isDone()) {
                        taskList.get(packageName)!!.scheduledFuture!!.cancel(true)
                    }
                }
                val timerTask = TimerTask()
                timerTask.scheduledFuture = mScheduledExecutorService.schedule(
                    {
                        Shell.cmd(String.format("am force-stop %s", packageName)).exec()
                        synchronized(taskList) {
                            taskList.remove(packageName)
                        }
                    }, timeout, TimeUnit.SECONDS
                )
                timerTask.packageName = packageName
                timerTask.endTimeMs = timeToStop
                synchronized(this.taskList) {
                    taskList.put(packageName, timerTask)
                }
                return
            }
            super.handleMessage(msg)
        }
    }

    fun getRunningTasks(): List<TimerTask> {
        val tasks: MutableList<TimerTask> = mutableListOf()
        val taskList= mHandler?.taskList ?: return tasks
        synchronized(taskList) {
            for (entry in taskList)
                tasks.add(entry.value)
        }
        return tasks
    }

    fun queryTask(packageName: String): TimerTask? {
        val taskList= mHandler?.taskList ?: return null
        synchronized(taskList) {
            return if (taskList.containsKey(packageName))
                taskList[packageName]
            else null
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return LocalBinder()
    }

    inner class LocalBinder : Binder() {
        val service: TimerService
            get() = this@TimerService
    }

    fun addTask(packageName: String, timeout: Long) {
        val msg = Message()
        msg.what = 3321
        val bundle = Bundle()
        bundle.putString("package_name", packageName)
        bundle.putLong("timeout", timeout)
        msg.obj = bundle
        mHandler!!.sendMessage(msg)
    }

    fun cancelTask(packageName: String) {
        val taskList= mHandler?.taskList ?: return
        synchronized(taskList) {
            if (taskList.containsKey(packageName)) {
                taskList[packageName]?.scheduledFuture?.cancel(true)
                taskList.remove(packageName)
            }
        }
    }

    override fun onDestroy() {
        val handlerThread = this.mWorkingThread
        if (handlerThread != null && handlerThread.isAlive) {
            mWorkingThread!!.looper.quit()
        }
        stopForeground(true)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onCreate() {
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
        this.mWorkingThread = object : HandlerThread("TimerThread") {
            override fun onLooperPrepared() {
                super.onLooperPrepared()
                this@TimerService.mHandler = MyHandler(mWorkingThread!!.looper)
            }
        }.also {
            it.start()
        }
    }

    companion object {
        val CHANNELID: String = "hnidesu_timer"
        val NOTIFICATIONID: Int = 3375
    }
}
