package com.easyquran

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

/** Main screen: continue reading, today's ayah and tiles for every feature. */
class HomeActivity : Activity() {
    private lateinit var col: LinearLayout
    private lateinit var contAr: TextView
    private lateinit var contRef: TextView
    private lateinit var todayAr: TextView
    private lateinit var todayUr: TextView
    private lateinit var todayRef: TextView
    private lateinit var prayerTv: TextView

    private fun text(t: String, sp: Float, a: Float = 1f) = TextView(this).apply { text = t; setTextSize(TypedValue.COMPLEX_UNIT_SP, sp); alpha = a; setTextColor(Color.WHITE) }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        Quran.load(this)
        col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(px(16), px(16), px(16), px(28)) }
        setContentView(ScrollView(this).apply { setBackgroundColor(Color.parseColor("#0E1F1A")); addView(col); fitSystemBars() })

        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(ImageView(this).apply { setImageResource(R.drawable.ic_logo) }, LinearLayout.LayoutParams(px(56), px(56)))
        head.addView(text("Easy Quran", 26f).apply { setPadding(px(12), 0, 0, 0) })
        col.addView(head)
        col.addView(text(hijri(), 13f, .75f))

        // continue reading
        val cont = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = cardBg(0xFF1E5E4A); setPadding(px(16), px(14), px(16), px(14)); setOnClickListener { openReader(Prefs.lastRead(this@HomeActivity)) } }
        cont.addView(text("📖 Continue reading", 14f, .85f))
        contRef = text("", 18f).apply { setPadding(0, px(4), 0, px(4)) }; cont.addView(contRef)
        contAr = text("", 22f).apply { typeface = Fonts.arabic(this@HomeActivity); gravity = Gravity.END; textDirection = View.TEXT_DIRECTION_RTL; maxLines = 2 }; cont.addView(contAr)
        col.addView(cont, lp(top = 14))

        // today's ayah
        val today = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = cardBg(); setPadding(px(16), px(14), px(16), px(14)); setOnClickListener { startActivity(Intent(this@HomeActivity, MainActivity::class.java)) } }
        today.addView(text("✨ Today’s ayah (your wallpaper)", 14f, .85f))
        todayRef = text("", 14f, .7f).apply { setPadding(0, px(4), 0, px(4)) }; today.addView(todayRef)
        todayAr = text("", 24f).apply { typeface = Fonts.arabic(this@HomeActivity); gravity = Gravity.CENTER; textDirection = View.TEXT_DIRECTION_RTL }; today.addView(todayAr)
        todayUr = text("", 15f).apply { typeface = Fonts.urdu(this@HomeActivity); gravity = Gravity.CENTER; textDirection = View.TEXT_DIRECTION_RTL; setLineSpacing(0f, 1.2f); setPadding(0, px(6), 0, 0) }; today.addView(todayUr)
        col.addView(today, lp(top = 12))

        prayerTv = text("", 15f).apply { background = cardBg(); setPadding(px(16), px(12), px(16), px(12)); setOnClickListener { startActivity(Intent(this@HomeActivity, PrayerActivity::class.java)) } }
        col.addView(prayerTv, lp(top = 12))

        val tiles = listOf(
            Tile("📖", "Quran") { openReader(Prefs.lastRead(this)) },
            Tile("📑", "Surah & Juz") { startActivity(Intent(this, QuranIndexActivity::class.java)) },
            Tile("🔍", "Search") { startActivity(Intent(this, QuranIndexActivity::class.java).putExtra("focusSearch", true)) },
            Tile("🔖", "Bookmarks") { startActivity(Intent(this, QuranIndexActivity::class.java).putExtra("tab", 2)) },
            Tile("🖼️", "Wallpaper & daily ayah") { startActivity(Intent(this, MainActivity::class.java)) },
            Tile("🕌", "Namaz & tracker") { startActivity(Intent(this, PrayerActivity::class.java)) },
            Tile("🧭", "Qibla") { startActivity(Intent(this, PrayerActivity::class.java).putExtra("focus", "qibla")) },
            Tile("📿", "Tasbeeh") { startActivity(Intent(this, PrayerActivity::class.java).putExtra("focus", "tasbeeh")) }
        )
        var i = 0
        while (i < tiles.size) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (k in 0..1) {
                val t = tiles[i + k]
                row.addView(TextView(this).apply {
                    text = "${t.icon}\n${t.label}"; setTextColor(Color.WHITE); setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f); gravity = Gravity.CENTER
                    background = cardBg(); setOnClickListener { t.go() }
                }, LinearLayout.LayoutParams(0, px(88), 1f).apply { setMargins(px(4), px(6), px(4), px(0)) })
            }
            col.addView(row); i += 2
        }
        col.addView(text("About & credits", 13f, .6f).apply { setPadding(0, px(22), 0, 0); setOnClickListener { about() } })
    }

    private fun lp(top: Int) = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = px(top) }

    private fun hijri(): String {
        val months = listOf("Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Shaban", "Ramadan", "Shawwal", "Dhul Qadah", "Dhul Hijjah")
        return try { val h = HijrahDate.now(); "${h.get(ChronoField.DAY_OF_MONTH)} ${months[h.get(ChronoField.MONTH_OF_YEAR) - 1]} ${h.get(ChronoField.YEAR)} AH (approx.)" } catch (e: Exception) { "" }
    }

    private fun openReader(i: Int) = startActivity(Intent(this, ReaderActivity::class.java).putExtra("i", i))

    override fun onResume() {
        super.onResume()
        val l = Prefs.lastRead(this).coerceIn(0, Quran.ayat.size - 1); val a = Quran.ayat[l]
        contRef.text = "${Quran.names[a.s - 1]}  ${a.s}:${a.a}"; contAr.text = a.ar
        val t = Prefs.index(this).coerceIn(0, Quran.ayat.size - 1); val d = Quran.ayat[t]
        todayRef.text = "${Quran.names[d.s - 1]}  ${d.s}:${d.a}"; todayAr.text = d.ar; todayUr.text = d.ur
        val n = Prayer.next(this, false)
        prayerTv.text = if (n == null) "🕌 Namaz times" else "🕌 Next: ${Prayer.names[n.second]} ${Prayer.urdu[n.second]} at ${Prayer.fmt(n.third)}  (${Prayer.city(this).name})"
        // catch up if the wallpaper is behind (for example the phone killed the service)
        if (Prefs.unlock(this) && Prefs.applied(this) != Prefs.index(this)) Thread { try { Wall.apply(this) } catch (e: Exception) { } }.start()
        if (Prefs.unlock(this)) try { startForegroundService(Intent(this, UnlockService::class.java)) } catch (e: Exception) { }
    }

    private fun about() {
        android.app.AlertDialog.Builder(this).setTitle("About Easy Quran").setMessage(
            "Quran text (Uthmani) and Urdu translation by Syed Abul A’la Maududi, from Tanzil (tanzil.net) via the quran-json project (CC BY-SA 4.0).\n\n" +
            "Fonts: Amiri Quran and Noto Nastaliq Urdu (SIL Open Font License).\n\n" +
            "Recitation: Mishary Alafasy, streamed from everyayah.com.\n\n" +
            "Prayer times are calculated on your phone using the University of Islamic Sciences, Karachi method (Hanafi Asr) and can differ from your local mosque by a few minutes. Please follow your mosque’s announced times.\n\n" +
            "Background scenes are original illustrations. The app collects no personal data.\n\n" +
            "If you notice any error in the Quran text or translation, please report it so it can be corrected."
        ).setPositiveButton("OK", null).show()
    }
}

class Tile(val icon: String, val label: String, val go: () -> Unit)
