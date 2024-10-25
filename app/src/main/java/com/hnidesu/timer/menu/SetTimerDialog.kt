package com.hnidesu.timer.menu

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.hnidesu.timer.R
import com.hnidesu.timer.component.AppItem
import com.hnidesu.timer.databinding.DialogSetTimerBinding
import it.sephiroth.android.library.numberpicker.doOnProgressChanged

class SetTimerDialog(ctx: Context, item: AppItem, listener: SetTimerListener):Dialog(ctx,false,null) {
    private var mDialogSetTimerBinding:DialogSetTimerBinding?=null
    private var mHour:Int=0
        set(value) {
            field=value
            mDialogSetTimerBinding?.hourPicker?.progress=value
        }
    private var mMinute:Int=0
        set(value) {
            field=value
            mDialogSetTimerBinding?.minutePicker?.progress=value
        }
    private var mSecond:Int=0
        set(value) {
            field=value
            mDialogSetTimerBinding?.secondPicker?.progress=value
        }
    var timeout:Long
        get() = mHour*3600L+mMinute*60L+mSecond
        set(value) {
            mHour=(value/3600).toInt()
            mMinute=((value%3600)/60).toInt()
            mSecond=(value%60).toInt()
        }
    interface SetTimerListener {
        fun onSetTimer(packageName: String, intervalInSeconds: Long): Boolean
    }

    init {
        val binding=DialogSetTimerBinding.inflate(LayoutInflater.from(ctx)).also {
            mDialogSetTimerBinding=it
        }
        mMinute=5
        setContentView(binding.root)
        binding.cancelButton.setOnClickListener { view: View? ->
            Toast.makeText(ctx, R.string.canceled, Toast.LENGTH_SHORT).show()
            dismiss()
        }
        binding.okButton.setOnClickListener { view: View? ->
            if (listener.onSetTimer(item.packageName!!, timeout)) {
                Toast.makeText(
                    ctx,
                    String.format(ctx.getString(R.string.set_time_success), timeout, item.appName),
                    Toast.LENGTH_LONG
                ).show()
            } else
                Toast.makeText(ctx, R.string.set_time_failed, Toast.LENGTH_LONG).show()
            dismiss()
        }
        binding.hourPicker.doOnProgressChanged { _, progress, fromUser ->
            if(fromUser)
                mHour=progress
        }
        binding.minutePicker.doOnProgressChanged { _, progress, fromUser ->
            if(fromUser)
                mMinute=progress
        }
        binding.secondPicker.doOnProgressChanged { _, progress, fromUser ->
            if(fromUser)
                mSecond=progress
        }
    }
}
