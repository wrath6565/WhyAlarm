package com.why.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val id = i.getIntExtra("id", -1); val day = i.getIntExtra("day", 0)
        val a = Store.load(c).find { it.id == id } ?: return
        val snooze = day == 8
        if (!snooze && !a.enabled) return
        c.startForegroundService(Intent(c, AlarmService::class.java)
            .putExtra("id", a.id).putExtra("why", a.why).putExtra("song", a.song))
        if (snooze) return
        if (day != 0) Scheduler.scheduleOne(c, a, day, Scheduler.next(day, a.h, a.m)) // repeat next week
        else { a.enabled = false; Store.upsert(c, a) } // one-time alarm turns itself off
    }
}
