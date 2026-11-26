package com.easyquran

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

/** Full Quran reader: one surah at a time, every ayah with Arabic and Urdu translation. */
class ReaderActivity : Activity() {
    private var surah = 1
    private var firstIdx = 0
    private var ayahCount = 0
    private var showBism = false
    private var playing = -1
    private var continuous = false
    private lateinit var list: ListView
    private lateinit var titleTv: TextView
    private lateinit var playBtn: Button

    private val off get() = if (showBism) 1 else 0

    private val adapter = object : BaseAdapter() {
        override fun getCount() = ayahCount + off
        override fun getItem(p: Int): Any = p
        override fun getItemId(p: Int) = p.toLong()
        override fun getViewTypeCount() = 2
        override fun getItemViewType(p: Int) = if (showBism && p == 0) 1 else 0
        override fun getView(p: Int, v: View?, parent: ViewGroup?): View {
            val size = Prefs.readerSize(this@ReaderActivity).toFloat()
            if (showBism && p == 0) {
                return (v as? TextView) ?: TextView(this@ReaderActivity).apply {
                    text = Quran.ayat[Quran.first[1]].ar; typeface = Fonts.arabic(context); gravity = Gravity.CENTER; setTextColor(Color.parseColor("#E2B84A"))
                    setPadding(0, px(16), 0, px(8))
                }.also { it.setTextSize(TypedValue.COMPLEX_UNIT_SP, size + 2) }
            }
            val idx = firstIdx + p - off
            val a = Quran.ayat[idx]
            val box = (v as? LinearLayout) ?: LinearLayout(this@ReaderActivity).apply {
                orientation = LinearLayout.VERTICAL; setPadding(px(14), px(10), px(14), px(10))
                addView(TextView(context).apply { tag = "ref"; setTextColor(Color.parseColor("#B8C7C1")); setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f) })
                addView(TextView(context).apply { tag = "ar"; setTextColor(Color.WHITE); typeface = Fonts.arabic(context); textDirection = View.TEXT_DIRECTION_RTL; gravity = Gravity.END; setLineSpacing(0f, 1.5f) })
                addView(TextView(context).apply { tag = "ur"; setTextColor(Color.parseColor("#D7E7E0")); typeface = Fonts.urdu(context); textDirection = View.TEXT_DIRECTION_RTL; gravity = Gravity.END; setLineSpacing(0f, 1.1f); setPadding(0, px(6), 0, 0) })
                addView(TextView(context).apply { tag = "ex"; setTextColor(Color.parseColor("#9FC3B5")); setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f); setPadding(0, px(6), 0, 0) })
            }
            val marked = Prefs.bookmarks(this@ReaderActivity).contains(idx.toString())
            box.findViewWithTag<TextView>("ref").text = "${a.s}:${a.a}" + (if (marked) "   ★" else "") + (if (playing == idx) "   ▶ playing" else "")
            box.findViewWithTag<TextView>("ar").apply { text = a.ar; setTextSize(TypedValue.COMPLEX_UNIT_SP, size) }
            box.findViewWithTag<TextView>("ur").apply { text = a.ur; setTextSize(TypedValue.COMPLEX_UNIT_SP, size * 0.62f) }
            val note = Prefs.note(this@ReaderActivity, idx)
            box.findViewWithTag<TextView>("ex").apply {
                val parts = listOf(a.tf, if (note.isNotEmpty()) "📝 $note" else "").filter { it.isNotEmpty() }
                text = parts.joinToString("\n\n"); visibility = if (parts.isEmpty()) View.GONE else View.VISIBLE
            }
            box.setBackgroundColor(if (playing == idx) Color.parseColor("#22E2B84A") else Color.TRANSPARENT)
            return box
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        Quran.load(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#0E1F1A")); fitSystemBars() }
        setContentView(root)

        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(px(6), px(6), px(6), px(6)) }
        fun small(t: String, f: () -> Unit) = Button(this).apply { text = t; isAllCaps = false; minWidth = 0; minimumWidth = 0; setPadding(px(10), 0, px(10), 0); setOnClickListener { f() } }
        bar.addView(small("◀") { go(surah - 1) })
        titleTv = TextView(this).apply { setTextColor(Color.WHITE); setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f); gravity = Gravity.CENTER; setOnClickListener { startActivity(Intent(this@ReaderActivity, QuranIndexActivity::class.java)) } }
        bar.addView(titleTv, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        bar.addView(small("▶") { go(surah + 1) })
        bar.addView(small("A−") { resize(-2) })
        bar.addView(small("A+") { resize(2) })
        playBtn = small("🔊 Play") { togglePlay() }
        bar.addView(playBtn)
        root.addView(bar)

        list = ListView(this).apply {
            divider = android.graphics.drawable.ColorDrawable(Color.parseColor("#22FFFFFF")); dividerHeight = 1
            adapter = this@ReaderActivity.adapter
            setOnItemClickListener { _, _, p, _ -> if (!(showBism && p == 0)) actions(firstIdx + p - off) }
        }
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val start = intent.getIntExtra("i", Prefs.lastRead(this)).coerceIn(0, Quran.ayat.size - 1)
        load(Quran.ayat[start].s, start)
    }

    private fun load(s: Int, scrollToIdx: Int?) {
        Audio.stop(); playing = -1; continuous = false; playBtn.text = "🔊 Play"
        surah = s.coerceIn(1, 114)
        firstIdx = Quran.first[surah]; ayahCount = Quran.surahs[surah - 1].total
        showBism = surah != 1 && surah != 9
        val m = Quran.surahs[surah - 1]
        titleTv.text = "$surah. ${Quran.names[surah - 1]}  ${m.ar}\n${if (m.type == "meccan") "Meccan" else "Medinan"} · ${m.total} ayat"
        adapter.notifyDataSetChanged()
        list.setSelection(if (scrollToIdx != null) (scrollToIdx - firstIdx + off) else 0)
        Prefs.setLastRead(this, scrollToIdx ?: firstIdx)
    }

    private fun go(s: Int) { if (s in 1..114) load(s, null) }
    private fun resize(d: Int) { Prefs.setReaderSize(this, (Prefs.readerSize(this) + d).coerceIn(18, 56)); adapter.notifyDataSetChanged() }

    private fun togglePlay() {
        if (continuous) { stopPlay(); return }
        val p = (list.firstVisiblePosition - off).coerceIn(0, ayahCount - 1)
        continuous = true; playBtn.text = "⏹ Stop"
        playFrom(firstIdx + p)
    }

    private fun playFrom(idx: Int) {
        if (idx >= firstIdx + ayahCount) { stopPlay(); return }
        playing = idx; adapter.notifyDataSetChanged()
        list.smoothScrollToPosition(idx - firstIdx + off)
        Audio.play(Quran.ayat[idx], idx,
            onDone = { if (continuous) playFrom(idx + 1) else { playing = -1; adapter.notifyDataSetChanged() } },
            onError = { Toast.makeText(this, "Audio unavailable (check internet)", Toast.LENGTH_SHORT).show(); stopPlay() })
    }

    private fun stopPlay() { Audio.stop(); continuous = false; playing = -1; playBtn.text = "🔊 Play"; adapter.notifyDataSetChanged() }

    private fun actions(idx: Int) {
        val a = Quran.ayat[idx]
        val marked = Prefs.bookmarks(this).contains(idx.toString())
        val items = arrayOf("▶ Play from here", if (marked) "★ Remove bookmark" else "☆ Bookmark", "📝 Note", "📋 Copy", "📤 Share", "🖼️ Use as wallpaper ayah")
        AlertDialog.Builder(this).setTitle("${Quran.names[a.s - 1]} ${a.s}:${a.a}").setItems(items) { _, w ->
            when (w) {
                0 -> { continuous = true; playBtn.text = "⏹ Stop"; playFrom(idx) }
                1 -> { val s = Prefs.bookmarks(this); if (!s.remove(idx.toString())) s.add(idx.toString()); Prefs.setBookmarks(this, s); adapter.notifyDataSetChanged() }
                2 -> noteDialog(idx)
                3 -> { (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("ayah", shareText(a))); Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show() }
                4 -> startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText(a)), "Share ayah"))
                5 -> {
                    Prefs.setIndex(this, idx); Prefs.setSeed(this, Prefs.seed(this) + 1)
                    Toast.makeText(this, "Setting wallpaper…", Toast.LENGTH_SHORT).show()
                    Thread { try { Wall.apply(this); runOnUiThread { Toast.makeText(this, "Wallpaper updated", Toast.LENGTH_SHORT).show() } } catch (e: Exception) { runOnUiThread { Toast.makeText(this, "Could not set wallpaper", Toast.LENGTH_SHORT).show() } } }.start()
                }
            }
        }.show()
    }

    private fun shareText(a: Ayah) = "${a.ar}\n\n${a.ur}\n\n— ${Quran.names[a.s - 1]} ${a.s}:${a.a} (Easy Quran)"

    private fun noteDialog(idx: Int) {
        val et = EditText(this).apply { setText(Prefs.note(this@ReaderActivity, idx)); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE }
        AlertDialog.Builder(this).setTitle("My note").setView(et).setPositiveButton("Save") { _, _ -> Prefs.setNote(this, idx, et.text.toString().trim()); adapter.notifyDataSetChanged() }.setNegativeButton("Cancel", null).show()
    }

    override fun onPause() {
        super.onPause()
        Prefs.setLastRead(this, firstIdx + (list.firstVisiblePosition - off).coerceIn(0, maxOf(0, ayahCount - 1)))
    }
    override fun onDestroy() { Audio.stop(); super.onDestroy() }
}
