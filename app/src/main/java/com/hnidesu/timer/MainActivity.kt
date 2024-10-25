package com.hnidesu.timer

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.hnidesu.timer.adapter.AppListAdapter
import com.hnidesu.timer.adapter.AppListAdapter.TaskOperationListener
import com.hnidesu.timer.adapter.AppListAdapter.UpdateOption
import com.hnidesu.timer.component.AppItem
import com.hnidesu.timer.service.TimerService
import com.hnidesu.timer.service.TimerService.LocalBinder
import com.topjohnwu.superuser.Shell
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.Semaphore
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), ServiceConnection {
    private val mApplicationCollection = HashMap<String?, AppItem>()
    private val mApplicationList: MutableList<AppItem> = arrayListOf()
    private var mTimer:Timer?=null
    private var mTimerService:TimerService?=null
    private var mViewHolder: ViewHolder? = null
    private val mLoadPackageSemaphore=Semaphore(1)

    private fun loadApplicationList() {
        thread {
            mLoadPackageSemaphore.acquire()
            val pm = packageManager
            for (info in pm.getInstalledApplications(0)) {
                val item = AppItem()
                mApplicationCollection[info.packageName] = item
                item.packageName = info.packageName
                item.appName = pm.getApplicationLabel(info).toString()
                try {
                    item.version = pm.getPackageInfo(item.packageName!!, 0).versionName
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                    item.version = ""
                }
                item.icon = packageManager.getApplicationIcon(info)
                mApplicationList.add(item)
            }
            mLoadPackageSemaphore.release()
        }
    }

    override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
        mLoadPackageSemaphore.acquire()
        val timerService = (iBinder as LocalBinder).service
        mTimerService=timerService
        enableTimer()
        val viewHolder = ViewHolder(object : TaskOperationListener {
            override fun onAddTask(packageName: String, timeout: Long): Boolean {
                timerService.addTask(packageName, timeout)
                return true
            }
            override fun onCancelTask(packageName: String): Boolean {
                timerService.cancelTask(packageName)
                return true
            }
        })
        this.mViewHolder = viewHolder
        viewHolder.bindViews()
        mViewHolder!!.mAppListAdapter.setList(this.mApplicationList)
    }

    override fun onServiceDisconnected(componentName: ComponentName) {
        mTimerService=null
        disableTimer()
        mLoadPackageSemaphore.release()
    }

    private fun enableTimer(){
        if(mTimer!=null)
            return
        mTimer=Timer().also {
            it.schedule(object:TimerTask() {
                override fun run() {
                    val runningTasks = mTimerService?.getRunningTasks() ?: return
                    for (task in runningTasks) {
                        val item = mApplicationCollection[task.packageName]
                        item!!.deadline = task.endTimeMs
                        this@MainActivity.runOnUiThread {
                            mViewHolder!!.mAppListAdapter.UpdateItem(
                                item, listOf(UpdateOption.UpdateDeadline)
                            )
                        }
                    }
                }
            }, 0L, 500L)
        }
    }

    private fun disableTimer(){
        if(mTimer!=null){
            mTimer?.cancel()
            mTimer=null
        }
    }

    override fun onPause() {
        super.onPause()
        disableTimer()
    }

    override fun onResume() {
        super.onResume()
        enableTimer()
    }

    public override fun onDestroy() {
        unbindService(this)
        super.onDestroy()
    }

    inner class ViewHolder(listener: TaskOperationListener?) {
        val mAppListAdapter: AppListAdapter =
            AppListAdapter(this@MainActivity, listener!!)
        private val mApplicationListRecyclerView: RecyclerView = findViewById(R.id.app_list)

        fun bindViews() {
            mApplicationListRecyclerView.adapter = this.mAppListAdapter
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!Shell.getShell().isRoot) {
            Toast.makeText(this, R.string.the_device_is_not_rooted, Toast.LENGTH_SHORT).show()
            System.exit(0)
        }
        loadApplicationList()
        val serviceIntent = Intent(this, TimerService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, this, 1)
    }
}
