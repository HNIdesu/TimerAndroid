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
            val restTime = (info.deadline - System.currentTimeMillis()) / 1000
            if (info.deadline < 0 || restTime < 0) {
                holder.mDeadline.visibility = View.INVISIBLE
                return
            }
            holder.mDeadline.text = GetDeadlineText(restTime)
            holder.mDeadline.visibility = View.VISIBLE
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

    fun UpdateItem(item: AppItem, options: List<UpdateOption>?) {
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

    fun GetDeadlineText(intervalInSeconds: Long): CharSequence {
        val outStr: String
        val builder = SpannableStringBuilder()
        var span: Any? = null
        if (intervalInSeconds == 0L) {
            outStr = mContext.getString(R.string.stopped)
        } else if (intervalInSeconds <= 60) {
            outStr =
                String.format(mContext.getString(R.string.remaining_seconds), intervalInSeconds)
            span = ForegroundColorSpan(mContext.getColor(R.color.red))
        } else if (intervalInSeconds <= 300) {
            outStr = String.format(
                mContext.getString(R.string.remaining_minutes),
                intervalInSeconds / 60,
                intervalInSeconds % 60
            )
            span = ForegroundColorSpan(mContext.getColor(R.color.orange))
        } else if (intervalInSeconds <= 600) {
            outStr = String.format(
                mContext.getString(R.string.remaining_minutes),
                intervalInSeconds / 60,
                intervalInSeconds % 60
            )
            span = ForegroundColorSpan(mContext.getColor(R.color.yellow))
        } else {
            outStr = String.format(
                mContext.getString(R.string.remaining_minutes),
                intervalInSeconds / 60,
                intervalInSeconds % 60
            )
            span = ForegroundColorSpan(mContext.getColor(R.color.green))
        }
        builder.append(outStr)
        if (span != null) builder.setSpan(span, 0, outStr.length, 17)
        return builder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val info = mApplicationList!![position]
        holder.mTvPackageName.text = info.packageName
        holder.mTvAppName.text = info.appName
        holder.mTvVersion.text = info.version
        holder.mIvIcon.setImageDrawable(info.icon)
        holder.mContentView.setOnClickListener { view: View? ->
            if (info.deadline == -1L) {
                val dialog = SetTimerDialog(
                    mContext, info,
                    object:SetTimerListener {
                        override fun onSetTimer(packageName: String, intervalInSeconds: Long): Boolean {
                            return mTaskOperationListener.onAddTask(
                                packageName,
                                intervalInSeconds
                            )
                        }
                    }
                )
                dialog.show()
                return@setOnClickListener
            }
            AlertDialog.Builder(mContext).setMessage(R.string.weather_cancel_timer)
                .setPositiveButton(R.string.ok) { dialogInterface: DialogInterface?, i: Int ->
                    if (mTaskOperationListener.onCancelTask(info.packageName!!)) {
                        Toast.makeText(mContext, R.string.the_task_is_canceled, Toast.LENGTH_LONG)
                            .show()
                        info.deadline = -1
                        UpdateItem(info, listOf(UpdateOption.UpdateDeadline))
                    }
                }.setNegativeButton(R.string.cancel) { dialogInterface: DialogInterface?, i: Int ->
                val context = mContext
                Toast.makeText(context, context.getString(R.string.canceled), Toast.LENGTH_SHORT)
                    .show()
            }.setOnDismissListener { dialogInterface: DialogInterface? ->
                val context = mContext
                Toast.makeText(context, context.getString(R.string.canceled), Toast.LENGTH_SHORT)
                    .show()
            }.create().show()
        }
        val restTime = (info.deadline - System.currentTimeMillis()) / 1000
        if (info.deadline < 0 || restTime < 0) {
            holder.mDeadline.visibility = View.INVISIBLE
            return
        }
        holder.mDeadline.text = GetDeadlineText(restTime)
        holder.mDeadline.visibility = View.VISIBLE
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
