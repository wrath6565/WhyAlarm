package com.why.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

// day: Calendar.SUNDAY..SATURDAY (1..7), or 0 for a one-time alarm
object Scheduler {
    private fun am(c: Context) = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private fun prefs(c: Context) = c.getSharedPreferences("cfg", Context.MODE_PRIVATE)

    fun next(day: Int, h: Int, m: Int): Long {
        val cal = Calendar.getInstance().apply {
            if (day != 0) set(Calendar.DAY_OF_WEEK, day)
            set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, if (day == 0) 1 else 7)
        return cal.timeInMillis
    }

    fun schedule(c: Context, day: Int, at: Long, h: Int, m: Int, why: String, song: String?) {
        val i = Intent(c, AlarmReceiver::class.java)
            .putExtra("day", day).putExtra("h", h).putExtra("m", m).putExtra("why", why).putExtra("song", song)
        val pi = PendingIntent.getBroadcast(c, day, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val show = PendingIntent.getActivity(c, 100, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        am(c).setAlarmClock(AlarmManager.AlarmClockInfo(at, show), pi)
    }

    fun cancelAll(c: Context) {
        for (d in 0..7) {
            val pi = PendingIntent.getBroadcast(c, d, Intent(c, AlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            if (pi != null) am(c).cancel(pi)
        }
    }

    fun save(c: Context, why: String, song: String?, h: Int, m: Int, days: List<Int>) {
        prefs(c).edit().putString("why", why).putString("song", song).putInt("h", h).putInt("m", m)
            .putString("days", days.joinToString(","))
            .putLong("once", if (days.isEmpty()) next(0, h, m) else 0L).apply()
    }

    /** Schedules whatever is saved. Returns the soonest trigger time, or null. */
    fun scheduleFromPrefs(c: Context): Long? {
        val p = prefs(c)
        if (!p.contains("h")) return null
        val h = p.getInt("h", 0); val m = p.getInt("m", 0)
        val why = p.getString("why", "Alarm")!!; val song = p.getString("song", null)
        val days = p.getString("days", "")!!.split(",").filter { it.isNotEmpty() }.map { it.toInt() }
        if (days.isEmpty()) {
            val once = p.getLong("once", 0)
            if (once > System.currentTimeMillis()) { schedule(c, 0, once, h, m, why, song); return once }
            return null
        }
        val times = days.map { d -> next(d, h, m).also { schedule(c, d, it, h, m, why, song) } }
        return times.minOrNull()
    }
}
