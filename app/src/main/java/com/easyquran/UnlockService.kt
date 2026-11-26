package com.easyquran

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Keeps a receiver alive so the wallpaper moves to the next ayah on every unlock (or every screen-on). */
class UnlockService : Service() {
    private val exec = Executors.newSingleThreadExecutor()
    private val running = AtomicBoolean(false)
    private val dirty = AtomicBoolean(false)
    private var lastEvent = 0L

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            val wanted = if (Prefs.trigger(c) == 0) Intent.ACTION_USER_PRESENT else Intent.ACTION_SCREEN_ON
            if (i.action != wanted) return
            val now = System.currentTimeMillis()
            if (now - lastEvent < 3000) return          // ignore double events
            lastEvent = now
            Prefs.countUnlock(c)
            try { Wall.advance(c) } catch (e: Exception) { }   // progress is saved at once, even if drawing fails later
            kick(c)
        }
    }

    private fun kick(c: Context) {
        dirty.set(true)
        if (!running.compareAndSet(false, true)) return
        exec.execute {
            val wl = (c.getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "easyquran:wall")
            try {
                wl.acquire(60_000)
                while (dirty.getAndSet(false)) { try { Wall.apply(c) } catch (e: Throwable) { } }
            } finally { if (wl.isHeld) wl.release(); running.set(false) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("svc", "Wallpaper service", NotificationManager.IMPORTANCE_MIN))
        val open = PendingIntent.getActivity(this, 0, Intent(this, HomeActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, "svc").setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("Easy Quran").setContentText("Next ayah on every unlock is on").setContentIntent(open).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(1, n)
        val f = IntentFilter().apply { addAction(Intent.ACTION_USER_PRESENT); addAction(Intent.ACTION_SCREEN_ON) }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, f, Context.RECEIVER_EXPORTED) else registerReceiver(receiver, f)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onTaskRemoved(rootIntent: Intent?) {
        // some phones kill the service when the app is swiped away; ask to be started again
        if (Prefs.unlock(this)) try { startForegroundService(Intent(this, UnlockService::class.java)) } catch (e: Exception) { }
        super.onTaskRemoved(rootIntent)
    }
    override fun onBind(i: Intent?): IBinder? = null
    override fun onDestroy() { try { unregisterReceiver(receiver) } catch (e: Exception) { }; exec.shutdown(); super.onDestroy() }
}
