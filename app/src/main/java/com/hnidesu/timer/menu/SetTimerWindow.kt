package com.hnidesu.timer.menu

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import com.hnidesu.timer.R
import com.hnidesu.timer.component.AppItem
import java.util.Locale

class SetTimerWindow(ctx: Context, item: AppItem, listener: SetTimerListener, parent: ViewGroup?) :
    PopupWindow() {
    private val mEtTimeout:TextView
    private var mTimeout = 0

    interface SetTimerListener {
        fun onSetTimer(packageName: String, intervalInMinutes: Int): Boolean
    }

    fun setTimeout(minutes: Int) {
        var minutes = minutes
        if (minutes < 0) {
            minutes = 0
        }
        if (minutes > 1000) {
            minutes = 1000
        }
        if (minutes != this.mTimeout) {
            this.mTimeout = minutes
            mEtTimeout.setText(String.format(Locale.ENGLISH, "%d", this.mTimeout))
        }
    }

    init {
        val itemView = LayoutInflater.from(ctx).inflate(R.layout.menu_settimer, parent, false)
        contentView = itemView
        width = -1
        height = -1
        this.mEtTimeout = itemView.findViewById(R.id.timeout)
        setTimeout(5)
        itemView.findViewById<View>(R.id.cancel).setOnClickListener { view: View? ->
            Toast.makeText(ctx, R.string.canceled, Toast.LENGTH_SHORT).show()
            dismiss()
        }
        itemView.findViewById<View>(R.id.ok).setOnClickListener { view: View? ->
            if (listener.onSetTimer(item.packageName!!, mTimeout)) {
                Toast.makeText(
                    ctx,
                    String.format(ctx.getString(R.string.set_time_success), mTimeout, item.appName),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(ctx, R.string.set_time_failed, Toast.LENGTH_LONG).show()
            }
            dismiss()
        }
        val btn = itemView.findViewById<View>(R.id.add_a_minute)
        btn.setOnClickListener { view: View? -> setTimeout(mTimeout + 1) }
        btn.setOnLongClickListener { view: View? ->
            setTimeout(mTimeout - 1)
            true
        }
        val btn2 = itemView.findViewById<View>(R.id.add_ten_minutes)
        btn2.setOnClickListener { view: View? -> setTimeout(mTimeout + 10) }
        btn2.setOnLongClickListener { view: View? ->
            setTimeout(mTimeout - 10)
            true
        }
        val btn3 = itemView.findViewById<View>(R.id.add_half_an_hour)
        btn3.setOnClickListener { view: View? -> setTimeout(mTimeout + 30) }
        btn3.setOnLongClickListener { view: View? ->
            setTimeout(mTimeout - 30)
            true
        }
        val btn4 = itemView.findViewById<View>(R.id.add_an_hour)
        btn4.setOnClickListener { view: View? -> setTimeout(mTimeout + 60) }
        btn4.setOnLongClickListener { view: View? ->
            setTimeout(mTimeout - 60)
            true
        }
    }
}
