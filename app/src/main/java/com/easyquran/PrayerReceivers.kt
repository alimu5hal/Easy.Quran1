package com.easyquran

import android.app.*
import android.content.*
import android.media.AudioAttributes
import android.media.RingtoneManager

class PrayerReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val idx = i.getIntExtra("i", 0).coerceIn(0, 4)
        val nm = c.getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel("namaz", "Namaz time", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
            enableVibration(true)
        }
        nm.createNotificationChannel(ch)
        val open = PendingIntent.getActivity(c, 3, Intent(c, PrayerActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val done = PendingIntent.getBroadcast(c, 10 + idx, Intent(c, PrayedReceiver::class.java).putExtra("i", idx), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(c, "namaz").setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("${Prayer.names[idx]} time  ${Prayer.urdu[idx]}")
            .setContentText("It is time for ${Prayer.names[idx]} in ${Prayer.city(c).name}")
            .setContentIntent(open).setAutoCancel(true)
            .addAction(Notification.Action.Builder(null, "I prayed ✓", done).build()).build()
        nm.notify(20 + idx, n)
        Prayer.schedule(c)
    }
}

class PrayedReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val idx = i.getIntExtra("i", 0)
        Prayer.setPrayed(c, Prayer.today(), idx, true)
        c.getSystemService(NotificationManager::class.java).cancel(20 + idx)
    }
}
