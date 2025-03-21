package com.hnidesu.timer

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.hnidesu.timer.activity.SettingsActivity
import com.hnidesu.timer.adapter.AppListAdapter
import com.hnidesu.timer.adapter.AppListAdapter.TaskOperationListener
import com.hnidesu.timer.component.AppItem
import com.hnidesu.timer.component.TaskStatus
import com.hnidesu.timer.database.AppRecordEntity
import com.hnidesu.timer.manager.AppPrefManager
import com.hnidesu.timer.manager.DatabaseManager
import com.hnidesu.timer.service.TimerService
import com.hnidesu.timer.util.Timer
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import rikka.shizuku.Shizuku
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity(), ServiceConnection {
    private val mApplicationCollection = HashMap<String?, AppItem>()
    private lateinit var mTimer: Timer
    private var mViewHolder: ViewHolder? = null
    private var mIncludeSystemApps = false

    private fun loadApplicationList(includeSystemApps: Boolean = false) {
        val pm = packageManager
        mApplicationCollection.clear()
        for (info in pm.getInstalledApplications(0)) {
            if (!includeSystemApps && (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0)
                continue
            val packageInfo = try {
                pm.getPackageInfo(info.packageName!!, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                continue
            }
            val version = packageInfo.versionName
            val item = AppItem(
                info.packageName,
                pm.getApplicationLabel(info).toString(),
                version,
                0,
                TaskStatus.None
            )
            mApplicationCollection[info.packageName] = item
        }
        val recordMap = runBlocking {
            val result = mutableMapOf<String, Long>()
            DatabaseManager.getMyDatabase(this@MainActivity).getAppRecordDao().getAll().forEach {
                result[it.packageName] = it.lastAccessTime
            }
            return@runBlocking result
        }
        mViewHolder?.mAppListAdapter?.setList(mApplicationCollection.values.sortedByDescending { recordMap[it.packageName] ?: 0 })
    }

    override fun onStop() {
        super.onStop()
        unbindService(this)
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, TimerService::class.java), this, BIND_AUTO_CREATE)
    }

    private inner class ViewHolder(listener: TaskOperationListener?) {
        val mAppListAdapter: AppListAdapter =
            AppListAdapter(this@MainActivity, listener!!)
        private val mApplicationListRecyclerView: RecyclerView = findViewById(R.id.app_list)

        fun bindViews() {
            mApplicationListRecyclerView.adapter = this.mAppListAdapter
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mTimer.cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.exit -> exitProcess(0)
            R.id.settings -> startActivity(Intent(this, SettingsActivity::class.java))
            else -> return false
        }
        return true
    }

    private fun checkPermission(){
        try{
            if(Shizuku.isPreV11())return
            if(Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED){
                return
            }else if(Shizuku.shouldShowRequestPermissionRationale())
                return
            else
                Shizuku.requestPermission(1)
        }catch (ex:Exception){
            ex.printStackTrace()
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shell.getShell().isRoot
        checkPermission()
        mTimer = Timer()
        setContentView(R.layout.activity_main)
        startService(Intent(this, TimerService::class.java))
        val recordDao = DatabaseManager.getMyDatabase(this@MainActivity).getAppRecordDao()
        val viewHolder = ViewHolder(object : TaskOperationListener {
            override fun onAddTask(packageName: String, timeout: Long): Boolean {
                val intent = Intent(this@MainActivity, TimerService::class.java)
                intent.putExtra("command", "addTask")
                intent.putExtra("packageName", packageName)
                intent.putExtra("shutdownDelay", timeout * 1000)
                startService(intent)
                AppPrefManager.setTimeout(timeout)
                CoroutineScope(Dispatchers.IO).launch {
                    val entity = AppRecordEntity(packageName, System.currentTimeMillis())
                    recordDao.insertOrUpdate(entity)
                }
                return true
            }

            override fun onCancelTask(packageName: String): Boolean {
                val intent = Intent(this@MainActivity, TimerService::class.java)
                intent.putExtra("command", "cancelTask")
                intent.putExtra("packageName", packageName)
                startService(intent)
                return true
            }
        })
        this.mViewHolder = viewHolder
        viewHolder.bindViews()
        mIncludeSystemApps = AppPrefManager.getShowSystemApps()
        loadApplicationList(includeSystemApps = mIncludeSystemApps)
    }

    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        val includeSystemApps = AppPrefManager.getShowSystemApps()
        if (includeSystemApps != mIncludeSystemApps){
            loadApplicationList(includeSystemApps = includeSystemApps)
            mIncludeSystemApps = includeSystemApps
        }

        mTimer.schedule(500){
            val adapter = mViewHolder?.mAppListAdapter ?: return@schedule
            if (binder is TimerService.LocalBinder) {
                val service = binder.service
                service.getTaskList().get().forEach {
                    val item = mApplicationCollection[it.packageName]
                    if (item != null) {
                        item.deadline = it.endTimeMs
                        item.status = it.status
                        runOnUiThread {
                            adapter.updateItem(
                                item,
                                listOf(AppListAdapter.UpdateOption.UpdateDeadline)
                            )
                        }
                    }
                }
            }
        }

    }

    override fun onServiceDisconnected(name: ComponentName?) {
        mTimer.cancel()
    }
}
