package com.hnidesu.timer.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hnidesu.timer.component.TaskStatus
import com.hnidesu.timer.component.TimerTask
import com.hnidesu.timer.manager.AppPrefManager
import com.hnidesu.timer.manager.ShellManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

class TimerService : Service() {
    private val mTaskList: HashMap<String, TimerTask> = HashMap()
    private var mAutoShutdownFuture: ScheduledFuture<*>? = null
    private lateinit var mWorkingExecutorService: ExecutorService
    private lateinit var mScheduledExecutorService: ScheduledExecutorService

    private fun checkAllTasksCompleted() {
        if (!AppPrefManager.getAutoShutdown()) return
        mWorkingExecutorService.execute {
            val isAllTasksCompleted = mTaskList.values.all {
                it.status == TaskStatus.Completed || it.status == TaskStatus.Canceled
            }
            if (isAllTasksCompleted) {
                if (mAutoShutdownFuture == null) {
                    Log.i(TAG, "程序将在10分钟后关闭")
                    mAutoShutdownFuture = mScheduledExecutorService.schedule({
                        exitProcess(0)
                    }, 10, TimeUnit.MINUTES)
                }
            } else {
                val future = mAutoShutdownFuture
                if (future != null) {
                    future.cancel(true)
                    mAutoShutdownFuture = null
                    Log.i(TAG, "自动关闭任务已取消")
                }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            @Suppress("DEPRECATION") stopForeground(true)
        else
            stopForeground(STOP_FOREGROUND_REMOVE)
        mScheduledExecutorService.shutdown()
        mWorkingExecutorService.shutdown()
    }

    inner class LocalBinder : Binder() {
        val service: TimerService
            get() = this@TimerService
    }

    /***
     * Add a task to the list of tasks
     *
     * @param packageName The package name of the application to be timed out
     * @param shutdownDelay The delay duration before shutting down the task in milliseconds
     */
    fun addTask(packageName: String, shutdownDelay: Long): Future<Boolean> {
        return mWorkingExecutorService.submit<Boolean> {
            mTaskList[packageName]?.also { timerTask ->//if the task is already running, cancel it
                if (!timerTask.scheduledFuture.isDone)
                    timerTask.scheduledFuture.cancel(true)
            }
            val timeToStop = System.currentTimeMillis() + shutdownDelay
            val timerTask = TimerTask(
                packageName = packageName,
                endTimeMs = timeToStop,
                scheduledFuture = mScheduledExecutorService.schedule(
                    {
                        ShellManager.getShell(AppPrefManager.getShellProvider())?.execute(
                            arrayOf(
                                "am", "force-stop", packageName
                            )
                        )
                        mWorkingExecutorService.execute {
                            mTaskList[packageName]?.status = TaskStatus.Completed
                            checkAllTasksCompleted()
                        }
                    },
                    shutdownDelay,
                    TimeUnit.MILLISECONDS
                ),
                status = TaskStatus.Running
            )
            mTaskList[packageName] = timerTask
            checkAllTasksCompleted()
            return@submit true
        }
    }

    /**
     * Cancel a task
     *
     * @param packageName The package name of the application to be timed out
     */
    fun cancelTask(packageName: String): Future<Boolean> {
        return mWorkingExecutorService.submit<Boolean> {
            val timerTask = mTaskList[packageName]
            val result = if (timerTask != null) {
                if (!timerTask.scheduledFuture.isDone && timerTask.scheduledFuture.cancel(false)) {
                    timerTask.status = TaskStatus.Canceled
                    true
                } else false
            } else
                false
            checkAllTasksCompleted()
            result
        }
    }

    /**
     * Get the list of tasks
     *
     */
    fun getTaskList(): Future<List<TimerTask>> {
        return mWorkingExecutorService.submit<List<TimerTask>> {
            return@submit mTaskList.values.toList()
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return LocalBinder()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val extras = intent.extras
        if (extras?.containsKey("command") == true) {
            when (extras.getString("command")) {
                "addTask" -> {
                    val packageName = extras.getString("packageName")
                    val shutdownDelay = extras.getLong("shutdownDelay")
                    if (packageName != null)
                        addTask(packageName, shutdownDelay)
                }

                "cancelTask" -> {
                    val packageName = extras.getString("packageName")
                    if (packageName != null)
                        cancelTask(packageName)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
        mWorkingExecutorService =
            ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, LinkedBlockingDeque())
    }

    companion object {
        val CHANNELID: String = "hnidesu_timer"
        val NOTIFICATIONID: Int = 3375
        val TAG = "TimerService"
    }
}
