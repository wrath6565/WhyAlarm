package com.why.alarm

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*

class AlarmActivity : Activity() {
    private val snoozeMinutes = 10

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setShowWhenLocked(true); setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        build()
    }

    override fun onNewIntent(i: Intent) { super.onNewIntent(i); setIntent(i); build() }

    private fun build() {
        val id = intent.getIntExtra("id", -1)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK); setPadding(48, 48, 48, 48)
        }
        root.addView(TextClock(this).apply {
            format12Hour = "h:mm a"; format24Hour = "h:mm a"; setTextColor(Color.WHITE); textSize = 56f; gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "REMEMBER WHY"; setTextColor(Color.parseColor("#FFB300")); textSize = 18f
            gravity = Gravity.CENTER; setPadding(0, 64, 0, 0)
        })
        root.addView(TextView(this).apply {
            text = intent.getStringExtra("why"); setTextColor(Color.WHITE); textSize = 34f
            gravity = Gravity.CENTER; setPadding(0, 32, 0, 96)
        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val snooze = Button(this).apply {
            text = "Snooze $snoozeMinutes min"; textSize = 18f
            setOnClickListener {
                Store.load(this@AlarmActivity).find { it.id == id }?.let { Scheduler.snooze(this@AlarmActivity, it, snoozeMinutes) }
                stopService(Intent(this@AlarmActivity, AlarmService::class.java)); finish()
            }
        }
        val stop = Button(this).apply {
            text = "Stop"; textSize = 18f
            setOnClickListener { stopService(Intent(this@AlarmActivity, AlarmService::class.java)); finish() }
        }
        row.addView(snooze, LinearLayout.LayoutParams(0, 200, 1f).apply { setMargins(8, 0, 8, 0) })
        row.addView(stop, LinearLayout.LayoutParams(0, 200, 1f).apply { setMargins(8, 0, 8, 0) })
        root.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        setContentView(root)
    }

    @Deprecated("Back is disabled so the alarm can't be dismissed by accident")
    override fun onBackPressed() { }
}
