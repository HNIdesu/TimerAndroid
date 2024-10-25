package com.hnidesu.timer.adapter

import android.content.Context
import android.content.DialogInterface
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.hnidesu.timer.R
import com.hnidesu.timer.component.AppItem
import com.hnidesu.timer.manager.SettingManager
import com.hnidesu.timer.menu.SetTimerDialog
import com.hnidesu.timer.menu.SetTimerDialog.SetTimerListener

class AppListAdapter(
    private val mContext: Context,
    private val mTaskOperationListener: TaskOperationListener
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>(){
    private var mApplicationList: MutableList<AppItem>? = null
    private val mIndexList = HashMap<String?, Int>()

    interface TaskOperationListener {
        fun onAddTask(packageName: String, timeout: Long): Boolean
        fun onCancelTask(packageName: String): Boolean
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        super.onBindViewHolder(holder, position, payloads)
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
            return
        }
        val info = mApplicationList!![position]
        val payload = payloads[0] as Int
        if ((UpdateOption.UpdateIcon.value and payload) != 0) {
            holder.mIvIcon.setImageDrawable(info.icon)
        } else if ((UpdateOption.UpdateDeadline.value and payload) != 0) {
            holder.mDeadline.text = getDeadlineText(info.deadline,info.status)
        } else if ((UpdateOption.UpdateAppName.value and payload) != 0) {
            holder.mTvAppName.text = info.appName
        } else if ((UpdateOption.UpdateVersion.value and payload) != 0) {
            holder.mTvVersion.text = info.version
        } else if ((UpdateOption.UpdatePackageName.value and payload) != 0) {
            holder.mTvPackageName.text = info.packageName
        }
    }

    enum class UpdateOption {
        UpdateIcon,
        UpdateAppName,
        UpdateVersion,
        UpdateDeadline,
        UpdatePackageName;

        val value: Int
            get() = 1 shl ordinal
    }

    class ViewHolder(val mContentView: View) : RecyclerView.ViewHolder(mContentView) {
        val mDeadline: TextView = itemView.findViewById(R.id.deadline)
        val mIvIcon: ImageView = itemView.findViewById(R.id.icon)
        val mTvAppName: TextView = itemView.findViewById(R.id.app_name)
        val mTvPackageName: TextView = itemView.findViewById(R.id.package_name)
        val mTvVersion: TextView = itemView.findViewById(R.id.version)
    }

    fun updateItem(item: AppItem, options: List<UpdateOption>?) {
        if (mIndexList.containsKey(item.packageName)) {
            val index = mIndexList[item.packageName]!!
            mApplicationList!![index] = item
            if (options == null) {
                notifyItemChanged(index)
                return
            }
            var flag = 0
            for (option in options) {
                flag = flag or option.value
            }
            notifyItemChanged(index, flag)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.app_item, parent, false)
        )
    }

    private fun getDeadlineText(deadline: Long, status:AppItem.Status): CharSequence {
        val builder = SpannableStringBuilder()
        val intervalInSeconds=(deadline - System.currentTimeMillis()) / 1000
        val outStr: String
        val span: Any
        when (status){
            AppItem.Status.Canceled->{
                outStr = mContext.getString(R.string.canceled)
                span = ForegroundColorSpan(mContext.getColor(R.color.black))
            }
            AppItem.Status.Completed->{
                outStr = mContext.getString(R.string.completed)
                span = ForegroundColorSpan(mContext.getColor(R.color.black))
            }
            AppItem.Status.Running->{
                when {
                    intervalInSeconds == 0L -> {
                        outStr = mContext.getString(R.string.completed)
                        span = ForegroundColorSpan(mContext.getColor(R.color.black))
                    }
                    intervalInSeconds <= 60 -> {
                        outStr = "${intervalInSeconds}s"
                        span = ForegroundColorSpan(mContext.getColor(R.color.red))
                    }
                    intervalInSeconds <= 300 -> {
                        outStr = "${intervalInSeconds/60}m${intervalInSeconds%60}s"
                        span = ForegroundColorSpan(mContext.getColor(R.color.orange))
                    }
                    intervalInSeconds <= 600 -> {
                        outStr = "${intervalInSeconds/60}m${intervalInSeconds%60}s"
                        span = ForegroundColorSpan(mContext.getColor(R.color.yellow))
                    }
                    else -> {
                        val hours = intervalInSeconds / 3600
                        val minutes = (intervalInSeconds % 3600) / 60
                        val seconds = intervalInSeconds % 60
                        outStr = if (hours > 0) "${hours}h${minutes}m${seconds}s"
                        else "${minutes}m${seconds}s"
                        span = ForegroundColorSpan(mContext.getColor(R.color.green))
                    }
                }
            }
            else->{
                return ""
            }
        }
        builder.append(outStr)
        builder.setSpan(span, 0, outStr.length, SpannableStringBuilder.SPAN_INCLUSIVE_EXCLUSIVE)
        return builder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val info = mApplicationList!![position]
        holder.mTvPackageName.text = info.packageName
        holder.mTvAppName.text = info.appName
        holder.mTvVersion.text = info.version
        holder.mIvIcon.setImageDrawable(info.icon)
        holder.mContentView.setOnClickListener { _: View? ->
            if (info.status!=AppItem.Status.Running){
                val dialog = SetTimerDialog(
                    mContext, info, object:SetTimerListener {
                        override fun onSetTimer(packageName: String, intervalInSeconds: Long): Boolean {
                            return mTaskOperationListener.onAddTask(packageName, intervalInSeconds)
                        }
                    }
                ).also {
                    it.timeout= SettingManager.getDefault(mContext).getLong("timeout",300)
                }
                dialog.show()
            }else{
                AlertDialog.Builder(mContext).setMessage(R.string.weather_cancel_timer)
                    .setPositiveButton(R.string.ok) { _: DialogInterface?, i: Int ->
                        if (mTaskOperationListener.onCancelTask(info.packageName)) {
                            Toast.makeText(mContext, R.string.the_task_is_canceled, Toast.LENGTH_LONG)
                                .show()
                            info.deadline = -1
                            updateItem(info, listOf(UpdateOption.UpdateDeadline))
                        }
                    }.setNegativeButton(R.string.cancel) { _: DialogInterface?, i: Int ->
                        val context = mContext
                        Toast.makeText(context, context.getString(R.string.canceled), Toast.LENGTH_SHORT)
                            .show()
                    }.setOnDismissListener {
                    }.create().show()
            }
        }
        holder.mDeadline.text = getDeadlineText(info.deadline,info.status)
    }

    override fun getItemCount(): Int {
        val list = this.mApplicationList ?: return 0
        return list.size
    }

    fun setList(list: MutableList<AppItem>) {
        this.mApplicationList = list
        mIndexList.clear()
        val len = list.size
        for (i in 0 until len) {
            mIndexList[mApplicationList!![i].packageName] = i
        }
        notifyItemRangeChanged(0, mApplicationList!!.size)
    }
}
