package com.why.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val why = i.getStringExtra("why") ?: "Alarm"
        val song = i.getStringExtra("song")
        c.startForegroundService(Intent(c, AlarmService::class.java).putExtra("why", why).putExtra("song", song))
        val day = i.getIntExtra("day", 0)
        if (day != 0) { // repeating: schedule the same weekday for next week
            val h = i.getIntExtra("h", 0); val m = i.getIntExtra("m", 0)
            Scheduler.schedule(c, day, Scheduler.next(day, h, m), h, m, why, song)
        }
    }
}
