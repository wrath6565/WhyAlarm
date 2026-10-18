package com.why.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

// day: Calendar.SUNDAY..SATURDAY (1..7), or 0 for a one-time alarm
object Scheduler {
    private fun am(c: Context) = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private fun code(id: Int, day: Int) = id * 10 + day

    fun next(day: Int, h: Int, m: Int): Long {
        val cal = Calendar.getInstance().apply {
            if (day != 0) set(Calendar.DAY_OF_WEEK, day)
            set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, if (day == 0) 1 else 7)
        return cal.timeInMillis
    }

    fun scheduleOne(c: Context, a: Alarm, day: Int, at: Long) {
        val i = Intent(c, AlarmReceiver::class.java).putExtra("id", a.id).putExtra("day", day)
        val pi = PendingIntent.getBroadcast(c, code(a.id, day), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val show = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        am(c).setAlarmClock(AlarmManager.AlarmClockInfo(at, show), pi)
    }

    fun schedule(c: Context, a: Alarm) {
        if (!a.enabled) return
        if (a.days.isEmpty()) { if (a.once > System.currentTimeMillis()) scheduleOne(c, a, 0, a.once) }
        else a.days.forEach { scheduleOne(c, a, it, next(it, a.h, a.m)) }
    }

    fun cancel(c: Context, id: Int) {
        for (d in 0..8) {
            val pi = PendingIntent.getBroadcast(c, code(id, d), Intent(c, AlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            if (pi != null) am(c).cancel(pi)
        }
    }

    // day code 8 = snooze
    fun snooze(c: Context, a: Alarm, minutes: Int) = scheduleOne(c, a, 8, System.currentTimeMillis() + minutes * 60_000L)

    fun scheduleAll(c: Context) = Store.load(c).forEach { schedule(c, it) }
}
