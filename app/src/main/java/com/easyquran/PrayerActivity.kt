package com.easyquran

import android.app.Activity
import android.app.AlertDialog
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.util.*
import kotlin.math.roundToInt

class CompassView(c: Context) : View(c) {
    var bearing = 0f   // degrees the arrow points to, relative to the top of the phone
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(cv: Canvas) {
        val cx = width / 2f; val cy = height / 2f; val r = minOf(cx, cy) - 8f
        p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = Color.WHITE; cv.drawCircle(cx, cy, r, p)
        cv.save(); cv.rotate(bearing, cx, cy)
        p.style = Paint.Style.FILL; p.color = Color.parseColor("#E2B84A")
        cv.drawPath(Path().apply { moveTo(cx, cy - r + 6); lineTo(cx - r * .18f, cy + r * .1f); lineTo(cx + r * .18f, cy + r * .1f); close() }, p)
        p.color = Color.WHITE; p.textSize = r * .22f; p.textAlign = Paint.Align.CENTER; cv.drawText("🕋", cx, cy - r + r * .42f, p)
        cv.restore()
    }
}

class PrayerActivity : Activity(), SensorEventListener {
    private val ui = Handler(Looper.getMainLooper())
    private lateinit var nextTv: TextView
    private lateinit var cityBtn: Button
    private lateinit var statsTv: TextView
    private lateinit var weekRow: LinearLayout
    private lateinit var compass: CompassView
    private lateinit var qiblaTv: TextView
    private lateinit var exactBtn: Button
    private lateinit var timeViews: List<TextView>
    private lateinit var checks: List<CheckBox>
    private var heading = 0f
    private lateinit var qiblaH: View
    private lateinit var tasbeehH: View
    private lateinit var sv: ScrollView
    private val tick = object : Runnable { override fun run() { updateNext(); ui.postDelayed(this, 1000) } }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun tv(t: String, size: Float, a: Float = 1f) = TextView(this).apply { text = t; textSize = size; alpha = a; setTextColor(Color.WHITE) }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(32)) }
        sv = ScrollView(this).apply { setBackgroundColor(Color.parseColor("#0E1F1A")); addView(col); fitSystemBars() }
        setContentView(sv)

        col.addView(tv("🕌 Namaz", 22f))
        col.addView(tv(hijri(), 14f, .8f))
        cityBtn = Button(this).apply { isAllCaps = false; setOnClickListener { cityDialog() } }; col.addView(cityBtn)
        nextTv = tv("", 18f).apply { gravity = Gravity.CENTER; setPadding(0, dp(10), 0, dp(10)) }; col.addView(nextTv)

        val tvs = ArrayList<TextView>(); val cbs = ArrayList<CheckBox>()
        for (i in 0..4) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(6), 0, dp(6)) }
            row.addView(tv("${Prayer.names[i]}\n${Prayer.urdu[i]}", 16f), LinearLayout.LayoutParams(0, WRAP_CONTENT, 1.2f))
            val t = tv("", 16f); tvs.add(t); row.addView(t, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            val sw = Switch(this).apply { text = "Alarm"; setTextColor(Color.WHITE); isChecked = Prayer.alarmOn(this@PrayerActivity, i)
                setOnCheckedChangeListener { _, on -> Prayer.setAlarmOn(this@PrayerActivity, i, on); Prayer.schedule(this@PrayerActivity) } }
            row.addView(sw, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1.3f))
            val cb = CheckBox(this).apply { text = "Prayed"; setTextColor(Color.WHITE)
                setOnCheckedChangeListener { _, on -> if (isPressed) { Prayer.setPrayed(this@PrayerActivity, Prayer.today(), i, on); stats() } } }
            cbs.add(cb); row.addView(cb, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1.3f))
            col.addView(row)
        }
        timeViews = tvs; checks = cbs
        exactBtn = Button(this).apply { text = "Allow exact alarms (needed for on-time reminders)"; isAllCaps = false
            setOnClickListener { if (android.os.Build.VERSION.SDK_INT >= 31) startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)) } }
        col.addView(exactBtn)

        statsTv = tv("", 16f).apply { setPadding(0, dp(14), 0, dp(4)) }; col.addView(statsTv)
        weekRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }; col.addView(weekRow)

        qiblaH = tv("🧭 Qibla", 18f).apply { setPadding(0, dp(22), 0, dp(4)) }; col.addView(qiblaH)
        qiblaTv = tv("", 14f, .85f); col.addView(qiblaTv)
        compass = CompassView(this); col.addView(compass, LinearLayout.LayoutParams(dp(220), dp(220)).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(8) })

        tasbeehH = tv("📿 Tasbeeh", 18f).apply { setPadding(0, dp(22), 0, dp(4)) }; col.addView(tasbeehH)
        val count = Button(this).apply { textSize = 36f; text = Prayer.tasbeeh(this@PrayerActivity).toString()
            setOnClickListener { val n = Prayer.tasbeeh(this@PrayerActivity) + 1; Prayer.setTasbeeh(this@PrayerActivity, n); text = n.toString()
                if (n % 33 == 0) { window.decorView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS); Toast.makeText(this@PrayerActivity, "$n ✓", Toast.LENGTH_SHORT).show() } } }
        col.addView(count, LinearLayout.LayoutParams(MATCH_PARENT, dp(110)))
        col.addView(Button(this).apply { text = "Reset"; setOnClickListener { Prayer.setTasbeeh(this@PrayerActivity, 0); count.text = "0" } })
        col.addView(tv("Times are calculated on your phone (University of Islamic Sciences, Karachi method, Hanafi Asr) and can differ from your local mosque by a minute or two. Pakistan Standard Time.", 11f, .55f).apply { setPadding(0, dp(18), 0, 0) })
        refresh()
        intent.getStringExtra("focus")?.let { f -> sv.post { sv.scrollTo(0, (if (f == "tasbeeh") tasbeehH else qiblaH).top) } }
    }

    private fun hijri(): String {
        val months = listOf("Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Shaban", "Ramadan", "Shawwal", "Dhul Qadah", "Dhul Hijjah")
        return try { val h = HijrahDate.now(); "${h.get(ChronoField.DAY_OF_MONTH)} ${months[h.get(ChronoField.MONTH_OF_YEAR) - 1]} ${h.get(ChronoField.YEAR)} AH (approx.)" } catch (e: Exception) { "" }
    }

    private fun cityDialog() {
        AlertDialog.Builder(this).setTitle("Choose your city").setItems(Prayer.cities.map { it.name }.toTypedArray()) { _, w ->
            Prayer.setCity(this, w); Prayer.schedule(this); refresh()
        }.show()
    }

    private fun refresh() {
        val c = Prayer.city(this)
        cityBtn.text = "📍 ${c.name}, Pakistan  (tap to change)"
        for (i in 0..4) { timeViews[i].text = Prayer.fmt(Prayer.millis(0, i, this)) }
        val today = Prayer.today()
        for (i in 0..4) checks[i].apply { setOnCheckedChangeListener(null); isChecked = Prayer.prayed(this@PrayerActivity, today, i)
            setOnCheckedChangeListener { _, on -> Prayer.setPrayed(this@PrayerActivity, Prayer.today(), i, on); stats() } }
        val q = Prayer.qibla(c)
        qiblaTv.text = "From ${c.name} the Qibla is ${q.roundToInt()}° from true north. Hold the phone flat and turn until the Kaaba points up."
        exactBtn.visibility = if (Prayer.canExact(this)) View.GONE else View.VISIBLE
        stats(); updateNext()
    }

    private fun stats() {
        val t = Prayer.count(this, Prayer.today())
        statsTv.text = "Today: $t / 5     🔥 Streak: ${Prayer.streak(this)} day(s)"
        weekRow.removeAllViews()
        val names = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        for (b in 6 downTo 0) {
            val d = Calendar.getInstance(TimeZone.getTimeZone("Asia/Karachi")).apply { add(Calendar.DAY_OF_YEAR, -b) }
            val n = Prayer.count(this, Prayer.dayKey(b))
            weekRow.addView(tv("${names[d.get(Calendar.DAY_OF_WEEK) - 1]}\n$n/5", 13f, if (n == 5) 1f else .7f).apply { gravity = Gravity.CENTER
                setBackgroundColor(if (n == 5) Color.parseColor("#2E7D32") else Color.parseColor("#22000000")) }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { setMargins(3, 3, 3, 3) })
        }
    }

    private fun updateNext() {
        val n = Prayer.next(this, false)
        if (n == null) { nextTv.text = ""; return }
        val s = (n.third - System.currentTimeMillis()) / 1000
        nextTv.text = "Next: ${Prayer.names[n.second]} ${Prayer.urdu[n.second]}  at ${Prayer.fmt(n.third)}\n%02d:%02d:%02d".format(s / 3600, s % 3600 / 60, s % 60)
    }

    override fun onResume() {
        super.onResume(); refresh(); ui.post(tick)
        val sm = getSystemService(SensorManager::class.java)
        sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            ?: run { qiblaTv.append("\n(No compass sensor on this phone.)") }
    }
    override fun onPause() { super.onPause(); ui.removeCallbacks(tick); getSystemService(SensorManager::class.java).unregisterListener(this) }
    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
    override fun onSensorChanged(e: SensorEvent) {
        val m = FloatArray(9); val o = FloatArray(3)
        SensorManager.getRotationMatrixFromVector(m, e.values); SensorManager.getOrientation(m, o)
        heading = ((Math.toDegrees(o[0].toDouble()) + 360) % 360).toFloat()
        val q = Prayer.qibla(Prayer.city(this)).toFloat()
        compass.bearing = q - heading; compass.invalidate()
    }
}
