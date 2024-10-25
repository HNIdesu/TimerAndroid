package com.hnidesu.timer

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.hnidesu.timer.adapter.AppListAdapter
import com.hnidesu.timer.adapter.AppListAdapter.TaskOperationListener
import com.hnidesu.timer.adapter.AppListAdapter.UpdateOption
import com.hnidesu.timer.component.AppItem
import com.hnidesu.timer.eventbus.AddTaskEvent
import com.hnidesu.timer.eventbus.CancelTaskEvent
import com.hnidesu.timer.eventbus.ListTaskEvent
import com.hnidesu.timer.eventbus.TaskStatusEvent
import com.hnidesu.timer.manager.SettingManager
import com.hnidesu.timer.service.TimerService
import com.topjohnwu.superuser.Shell
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.Timer
import java.util.TimerTask

class MainActivity : AppCompatActivity() {
    private val mApplicationCollection = HashMap<String?, AppItem>()
    private val mApplicationList: MutableList<AppItem> = arrayListOf()
    private var mTimer:Timer?=null
    private var mViewHolder: ViewHolder? = null

    private fun loadApplicationList() {
        val pm = packageManager
        for (info in pm.getInstalledApplications(0)) {
            val version=try {
                pm.getPackageInfo(info.packageName!!, 0).versionName
            } catch (e: PackageManager.NameNotFoundException) {
                ""
            }
            val item = AppItem(
                info.packageName,
                pm.getApplicationLabel(info).toString(),
                version,
                packageManager.getApplicationIcon(info),
                -1,
                AppItem.Status.None
            )
            mApplicationCollection[info.packageName] = item
            mApplicationList.add(item)
        }
    }

    private fun enableTimer(){
        if(mTimer!=null)
            return
        mTimer=Timer().also {
            it.schedule(object:TimerTask() {
                override fun run() {
                    EventBus.getDefault().post(ListTaskEvent())
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
        super.onDestroy()
        EventBus.getDefault().unregister(this)
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
        val viewHolder = ViewHolder(object : TaskOperationListener {
            override fun onAddTask(packageName: String, timeout: Long): Boolean {
                EventBus.getDefault().post(AddTaskEvent(packageName, timeout))
                SettingManager.getDefault(this@MainActivity).edit().putLong("timeout",timeout).apply()
                return true
            }
            override fun onCancelTask(packageName: String): Boolean {
                EventBus.getDefault().post(CancelTaskEvent(packageName))
                return true
            }
        })
        this.mViewHolder = viewHolder
        viewHolder.bindViews()
        mViewHolder!!.mAppListAdapter.setList(this.mApplicationList)
        EventBus.getDefault().register(this)
    }

    @Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.MAIN)
    fun onMessageEvent(event: Any?) {
        when(event){
            is TaskStatusEvent->{
                val item = mApplicationCollection[event.packageName]!!
                when(event.status){
                    TaskStatusEvent.Status.Running->{
                        item.deadline = event.endTimeMs
                        item.status=AppItem.Status.Running
                        mViewHolder!!.mAppListAdapter.updateItem(
                            item, listOf(UpdateOption.UpdateDeadline)
                        )
                    }
                    TaskStatusEvent.Status.Canceled->{
                        item.deadline = -1
                        item.status=AppItem.Status.Canceled
                        mViewHolder!!.mAppListAdapter.updateItem(
                            item, listOf(UpdateOption.UpdateDeadline)
                        )
                    }
                    TaskStatusEvent.Status.Completed->{
                        item.deadline = -1
                        item.status=AppItem.Status.Completed
                        mViewHolder!!.mAppListAdapter.updateItem(
                            item, listOf(UpdateOption.UpdateDeadline))
                    }
                    TaskStatusEvent.Status.Error-> {
                        item.deadline = -1
                        item.status = AppItem.Status.Error
                        mViewHolder!!.mAppListAdapter.updateItem(
                            item, listOf(UpdateOption.UpdateDeadline)
                        )
                    }
                }
            }
        }
    }
}
