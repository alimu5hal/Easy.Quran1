package com.easyquran

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class City(val name: String, val lat: Double, val lng: Double)

/** Prayer times computed on the phone (no internet needed). Default = University of Islamic Sciences, Karachi
 *  (Fajr 18°, Isha 18°), the method used in Pakistan, with Hanafi Asr (shadow = 2x). Pakistan time, UTC+5. */
object Prayer {
    val names = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    val urdu = listOf("فجر", "ظہر", "عصر", "مغرب", "عشاء")
    val cities = listOf(
        City("Lahore", 31.5497, 74.3436), City("Karachi", 24.8607, 67.0011), City("Islamabad", 33.6844, 73.0479),
        City("Rawalpindi", 33.5651, 73.0169), City("Faisalabad", 31.4504, 73.1350), City("Multan", 30.1575, 71.5249),
        City("Peshawar", 34.0151, 71.5249), City("Quetta", 30.1798, 66.9750), City("Hyderabad", 25.3960, 68.3578),
        City("Gujranwala", 32.1877, 74.1945), City("Sialkot", 32.4945, 74.5229), City("Sargodha", 32.0836, 72.6711),
        City("Bahawalpur", 29.3956, 71.6836), City("Sukkur", 27.7052, 68.8574), City("Abbottabad", 34.1688, 73.2215),
        City("Mardan", 34.1989, 72.0231), City("Gilgit", 35.9208, 74.3080), City("Muzaffarabad", 34.3700, 73.4711)
    )
    private val kaaba = City("Kaaba", 21.4225, 39.8262)
    private const val TZ = 5.0

    private fun r(x: Double) = Math.toRadians(x)
    private fun fix(x: Double, a: Double) = x - a * floor(x / a)

    private fun sun(jd: Double): Pair<Double, Double> { // declination, equation of time
        val d = jd - 2451545.0
        val g = fix(357.529 + 0.98560028 * d, 360.0); val q = fix(280.459 + 0.98564736 * d, 360.0)
        val l = fix(q + 1.915 * sin(r(g)) + 0.020 * sin(r(2 * g)), 360.0)
        val e = 23.439 - 0.00000036 * d
        val ra = Math.toDegrees(atan2(cos(r(e)) * sin(r(l)), cos(r(l)))) / 15
        return Pair(Math.toDegrees(asin(sin(r(e)) * sin(r(l)))), q / 15 - fix(ra, 24.0))
    }

    private fun julian(y0: Int, m0: Int, d: Int): Double {
        var y = y0; var m = m0
        if (m <= 2) { y -= 1; m += 12 }
        val a = y / 100; val b = 2 - a + a / 4
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + d + b - 1524.5
    }

    /** Returns minutes-from-midnight for Fajr, Dhuhr, Asr, Maghrib, Isha and sunrise (last). */
    fun times(y: Int, m: Int, d: Int, c: City): DoubleArray {
        val j = julian(y, m, d) - c.lng / 360.0
        fun mid(t: Double) = fix(12 - sun(j + t / 24).second, 24.0)
        fun ang(a: Double, t: Double, ccw: Boolean): Double {
            val decl = sun(j + t / 24).first
            val v = ((-sin(r(a)) - sin(r(decl)) * sin(r(c.lat))) / (cos(r(decl)) * cos(r(c.lat)))).coerceIn(-1.0, 1.0)
            val h = Math.toDegrees(acos(v)) / 15
            return if (ccw) -h else h
        }
        fun asr(t: Double): Double {
            val decl = sun(j + t / 24).first
            val a = -Math.toDegrees(atan(1 / (2 + tan(r(abs(c.lat - decl))))))
            return ang(a, t, false)
        }
        var t = doubleArrayOf(5.0, 12.0, 13.0, 18.0, 18.0, 6.0) // fajr dhuhr asr maghrib isha sunrise
        repeat(2) {
            t = doubleArrayOf(
                mid(t[0]) + ang(18.0, t[0], true), mid(t[1]), mid(t[2]) + asr(t[2]),
                mid(t[3]) + ang(0.833, t[3], false), mid(t[4]) + ang(18.0, t[4], false), mid(t[5]) + ang(0.833, t[5], true))
        }
        val adj = TZ - c.lng / 15
        return DoubleArray(6) { (t[it] + adj) * 60 }
    }

    fun city(ctx: Context) = cities[sp(ctx).getInt("city", 0).coerceIn(0, cities.size - 1)]
    fun setCity(ctx: Context, i: Int) { sp(ctx).edit().putInt("city", i).apply() }
    private fun sp(c: Context) = c.getSharedPreferences("eq_prayer", Context.MODE_PRIVATE)

    /** Time in epoch millis for prayer index (0..4) on a Pakistan-time calendar day. */
    fun millis(dayOffset: Int, idx: Int, ctx: Context): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Karachi"))
        cal.add(Calendar.DAY_OF_YEAR, dayOffset)
        val t = times(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), city(ctx))
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis + (t[idx] * 60000).roundToLong()
    }

    fun fmt(ms: Long): String = SimpleDateFormat("h:mm a", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Karachi") }.format(Date(ms))
    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Karachi") }.format(Date())
    fun dayKey(back: Int): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Karachi") }.format(Date(System.currentTimeMillis() - back * 86400000L))

    // ---- alarms ----
    fun alarmOn(ctx: Context, i: Int) = sp(ctx).getBoolean("al$i", true)
    fun setAlarmOn(ctx: Context, i: Int, v: Boolean) { sp(ctx).edit().putBoolean("al$i", v).apply() }

    /** Returns (dayOffset, idx) of the next enabled prayer, or null. */
    fun next(ctx: Context, onlyEnabled: Boolean): Triple<Int, Int, Long>? {
        val now = System.currentTimeMillis()
        for (d in 0..1) for (i in 0..4) {
            if (onlyEnabled && !alarmOn(ctx, i)) continue
            val ms = millis(d, i, ctx); if (ms > now + 1000) return Triple(d, i, ms)
        }
        return null
    }

    fun canExact(ctx: Context) = android.os.Build.VERSION.SDK_INT < 31 || ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = { i: Int -> PendingIntent.getBroadcast(ctx, 100, Intent(ctx, PrayerReceiver::class.java).putExtra("i", i), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) }
        am.cancel(pi(0))
        val n = next(ctx, true) ?: return
        if (canExact(ctx)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, n.third, pi(n.second))
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, n.third, pi(n.second))
    }

    // ---- tracker ----
    fun prayed(ctx: Context, day: String, i: Int) = sp(ctx).getBoolean("p_${day}_$i", false)
    fun setPrayed(ctx: Context, day: String, i: Int, v: Boolean) { sp(ctx).edit().putBoolean("p_${day}_$i", v).apply() }
    fun count(ctx: Context, day: String) = (0..4).count { prayed(ctx, day, it) }
    fun streak(ctx: Context): Int { // consecutive fully-prayed days; today counts once it is complete
        var s = 0; var b = if (count(ctx, dayKey(0)) == 5) 0 else 1
        while (b < 400 && count(ctx, dayKey(b)) == 5) { s++; b++ }
        return s
    }
    fun tasbeeh(ctx: Context) = sp(ctx).getInt("tasbeeh", 0)
    fun setTasbeeh(ctx: Context, v: Int) { sp(ctx).edit().putInt("tasbeeh", v).apply() }

    /** Direction of the Qibla from a city, degrees clockwise from true north. */
    fun qibla(c: City): Double {
        val p1 = r(c.lat); val p2 = r(kaaba.lat); val dl = r(kaaba.lng - c.lng)
        return fix(Math.toDegrees(atan2(sin(dl), cos(p1) * tan(p2) - sin(p1) * cos(dl))), 360.0)
    }
}
