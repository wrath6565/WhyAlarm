package com.why.alarm

import android.app.*
import android.content.Intent
import android.media.*
import android.net.Uri
import android.os.IBinder

class AlarmService : Service() {
    private var player: MediaPlayer? = null

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        val why = i?.getStringExtra("why") ?: "Alarm"
        val song = i?.getStringExtra("song")
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("alarm", "Alarms", NotificationManager.IMPORTANCE_HIGH).apply { setSound(null, null) })

        val open = PendingIntent.getActivity(this, 0,
            Intent(this, AlarmActivity::class.java).putExtra("why", why).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, "alarm")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Reminder").setContentText(why)
            .setCategory(Notification.CATEGORY_ALARM)
            .setFullScreenIntent(open, true).setContentIntent(open).setOngoing(true).build()
        startForeground(1, n)

        val uri = song?.let { Uri.parse(it) } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        player = try {
            MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                setDataSource(this@AlarmService, uri); isLooping = true; prepare(); start()
            }
        } catch (e: Exception) { // song missing/deleted: fall back to default tone
            MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
                setDataSource(this@AlarmService, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                isLooping = true; prepare(); start()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() { player?.release(); super.onDestroy() }
    override fun onBind(i: Intent?): IBinder? = null
}
