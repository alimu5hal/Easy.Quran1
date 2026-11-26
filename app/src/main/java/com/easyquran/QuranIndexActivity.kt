package com.easyquran

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*

class Row(val title: String, val sub: String, val right: String, val target: Int)

/** Surah list, juz (para) list, bookmarks and search results. */
class QuranIndexActivity : Activity() {
    private var tab = 0
    private var results: List<Int>? = null
    private lateinit var list: ListView
    private lateinit var search: EditText
    private lateinit var tabBtns: List<Button>
    private var rows: List<Row> = emptyList()

    private val adapter = object : BaseAdapter() {
        override fun getCount() = rows.size
        override fun getItem(p: Int) = rows[p]
        override fun getItemId(p: Int) = p.toLong()
        override fun getView(p: Int, v: View?, parent: ViewGroup?): View {
            val r = rows[p]
            val box = (v as? LinearLayout) ?: LinearLayout(this@QuranIndexActivity).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(px(14), px(12), px(14), px(12))
                val left = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; tag = "left" }
                left.addView(TextView(context).apply { tag = "t"; setTextColor(Color.WHITE); setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f) })
                left.addView(TextView(context).apply { tag = "s"; setTextColor(Color.parseColor("#B8C7C1")); setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f); maxLines = 2 })
                addView(left, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(TextView(context).apply { tag = "r"; setTextColor(Color.WHITE); typeface = Fonts.arabic(context); setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f); textDirection = View.TEXT_DIRECTION_RTL })
            }
            box.findViewWithTag<TextView>("t").text = r.title
            box.findViewWithTag<TextView>("s").text = r.sub
            box.findViewWithTag<TextView>("r").text = r.right
            return box
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        Quran.load(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#0E1F1A")); setPadding(px(10), px(10), px(10), 0); fitSystemBars() }
        setContentView(root)
        search = EditText(this).apply {
            hint = "Search surah, or a word (press Search for ayah text)"; setHintTextColor(Color.GRAY); setTextColor(Color.WHITE)
            imeOptions = EditorInfo.IME_ACTION_SEARCH; setSingleLine(true)
            setOnEditorActionListener { _, id, _ ->
                if (id == EditorInfo.IME_ACTION_SEARCH) { val q = text.toString(); results = if (q.isBlank()) null else Quran.search(q); show(); true } else false
            }
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) { results = null; show() }
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            })
        }
        root.addView(search)
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tabBtns = listOf("Surah", "Juz", "Bookmarks").mapIndexed { i, n -> Button(this).apply { text = n; isAllCaps = false; setOnClickListener { tab = i; results = null; show() } } }
        tabBtns.forEach { bar.addView(it, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)) }
        root.addView(bar)
        list = ListView(this).apply {
            adapter = this@QuranIndexActivity.adapter; divider = android.graphics.drawable.ColorDrawable(Color.parseColor("#22FFFFFF")); dividerHeight = 1
            setOnItemClickListener { _, _, p, _ -> startActivity(Intent(this@QuranIndexActivity, ReaderActivity::class.java).putExtra("i", rows[p].target)) }
        }
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        tab = intent.getIntExtra("tab", 0)
        if (intent.getBooleanExtra("focusSearch", false)) search.requestFocus()
        show()
    }

    override fun onResume() { super.onResume(); if (tab == 2 && results == null) show() }

    private fun show() {
        val q = search.text.toString().trim()
        val res = results
        tabBtns.forEachIndexed { i, bt -> bt.alpha = if (res == null && i == tab) 1f else 0.55f }
        rows = when {
            res != null -> res.map { val a = Quran.ayat[it]; Row("${Quran.names[a.s - 1]}  ${a.s}:${a.a}", a.ur.take(110), "", it) }
            tab == 0 -> Quran.surahs.indices.filter { i ->
                q.isEmpty() || (i + 1).toString() == q || Quran.names[i].replace("-", "").replace("’", "").replace("'", "").contains(q.replace("-", "").replace("'", ""), true) || Quran.surahs[i].ar.contains(q)
            }.map { i ->
                val s = Quran.surahs[i]
                Row("${i + 1}. ${Quran.names[i]}", "${if (s.type == "meccan") "Meccan" else "Medinan"} · ${s.total} ayat · ${s.ur}", s.ar, Quran.first[i + 1])
            }
            tab == 1 -> Quran.juzStarts.mapIndexed { j, st ->
                val idx = Quran.indexOf(st.first, st.second)
                Row("Juz ${j + 1}", "Starts at ${Quran.names[st.first - 1]} ${st.first}:${st.second}", Quran.ayat[idx].ar.take(18), idx)
            }
            else -> Prefs.bookmarks(this).mapNotNull { it.toIntOrNull() }.sorted().map { val a = Quran.ayat[it]; Row("${Quran.names[a.s - 1]}  ${a.s}:${a.a}", a.ur.take(110), "", it) }
        }
        adapter.notifyDataSetChanged()
        if (rows.isEmpty()) Toast.makeText(this, if (tab == 2 && res == null) "No bookmarks yet" else "Nothing found", Toast.LENGTH_SHORT).show()
    }
}
