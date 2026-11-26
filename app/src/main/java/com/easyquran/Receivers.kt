package com.easyquran

import android.app.*
import android.content.*
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (Prefs.unlock(c)) c.startForegroundService(Intent(c, UnlockService::class.java))
        Reminder.schedule(c)
        Prayer.schedule(c)
    }
}

object Reminder {
    private fun pi(c: Context) = PendingIntent.getBroadcast(c, 1, Intent(c, ReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun schedule(c: Context) {
        val am = c.getSystemService(AlarmManager::class.java)
        am.cancel(pi(c))
        val m = Prefs.reminder(c); if (m < 0) return
        val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, m / 60); set(Calendar.MINUTE, m % 60); set(Calendar.SECOND, 0) }
        if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pi(c))
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Quran.load(c)
        if (!Prefs.unlock(c)) Wall.advance(c)   // when unlock mode is off, move on once a day
        val a = Quran.ayat[Prefs.index(c)]
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("remind", "Daily ayah", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(c, 2, Intent(c, HomeActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(c, "remind").setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("${Quran.names[a.s - 1]}  ${a.s}:${a.a}")
            .setContentText(a.ur)
            .setStyle(Notification.BigTextStyle().bigText(a.ar + "\n\n" + a.ur))
            .setContentIntent(open).setAutoCancel(true).build()
        nm.notify(2, n)
    }
}
