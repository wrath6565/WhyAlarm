package com.why.alarm

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*

class MainActivity : Activity() {
    private lateinit var list: LinearLayout
    private lateinit var setup: LinearLayout
    private val names = mapOf(2 to "Mon", 3 to "Tue", 4 to "Wed", 5 to "Thu", 6 to "Fri", 7 to "Sat", 1 to "Sun")
    private val order = listOf(2, 3, 4, 5, 6, 7, 1)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 80, 40, 40) }
        root.addView(TextView(this).apply { text = "My alarms"; textSize = 26f })
        setup = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(setup)
        val add = Button(this).apply {
            text = "+ Add alarm"
            setOnClickListener { startActivity(Intent(this@MainActivity, EditActivity::class.java)) }
        }
        root.addView(add)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list)
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() { super.onResume(); refreshSetup(); refresh() }

    private fun refreshSetup() {
        setup.removeAllViews()
        val uri = Uri.parse("package:$packageName")
        val items = mutableListOf<Pair<String, Intent>>()
        if (!Settings.canDrawOverlays(this))
            items.add("1. Allow display over other apps" to Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri))
        if (Build.VERSION.SDK_INT >= 34 && !getSystemService(NotificationManager::class.java).canUseFullScreenIntent())
            items.add("2. Allow full-screen alarms" to Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, uri))
        if (!getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName))
            items.add("3. Turn off battery optimisation" to Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, uri))
        if (items.isEmpty()) return
        setup.addView(TextView(this).apply { text = "\nSetup needed so alarms cover your screen. Tap each button and allow:"; textSize = 15f })
        items.forEach { (label, intent) ->
            setup.addView(Button(this).apply { text = label; setOnClickListener { startActivity(intent) } })
        }
    }

    private fun refresh() {
        list.removeAllViews()
        val alarms = Store.load(this).sortedWith(compareBy({ it.h }, { it.m }))
        if (alarms.isEmpty()) list.addView(TextView(this).apply { text = "\nNo alarms yet. Tap + Add alarm."; textSize = 16f })
        alarms.forEach { a ->
            val hh = if (a.h % 12 == 0) 12 else a.h % 12
            val time = "%d:%02d %s".format(hh, a.m, if (a.h < 12) "AM" else "PM")
            val daysText = when {
                a.days.isEmpty() -> "One time"
                a.days.size == 7 -> "Every day"
                else -> order.filter { it in a.days }.joinToString(" ") { names[it]!! }
            }
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 24, 0, 24) }
            val info = TextView(this).apply {
                text = "$time\n${a.why}\n$daysText"; textSize = 16f
                alpha = if (a.enabled) 1f else 0.4f
                setOnClickListener { startActivity(Intent(this@MainActivity, EditActivity::class.java).putExtra("id", a.id)) }
            }
            val sw = Switch(this).apply { isChecked = a.enabled }
            sw.setOnCheckedChangeListener { _, on ->
                a.enabled = on
                if (on && a.days.isEmpty()) a.once = Scheduler.next(0, a.h, a.m)
                Scheduler.cancel(this, a.id); Store.upsert(this, a); Scheduler.schedule(this, a)
                info.alpha = if (on) 1f else 0.4f
            }
            val del = Button(this).apply {
                text = "✕"
                setOnClickListener {
                    AlertDialog.Builder(this@MainActivity).setMessage("Delete this alarm?")
                        .setPositiveButton("Delete") { _, _ -> Scheduler.cancel(this@MainActivity, a.id); Store.delete(this@MainActivity, a.id); refresh() }
                        .setNegativeButton("Cancel", null).show()
                }
            }
            row.addView(info, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            row.addView(sw); row.addView(del)
            list.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
    }
}
