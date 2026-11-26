package com.easyquran

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Surah(val ar: String, val type: String, val total: Int, val ur: String)

data class Ayah(val s: Int, val a: Int, val ar: String, val ur: String, val tf: String = "", val own: Boolean = false)

object Quran {
    var names: List<String> = emptyList()
    private var base: List<Ayah> = emptyList()
    var ayat: List<Ayah> = emptyList()
    var urSource = ""
    var baseSource = ""
    var surahs: List<Surah> = emptyList()
    var first = IntArray(116)
    /** Standard 30 juz (para) starts as surah:ayah. */
    val juzStarts = listOf(1 to 1, 2 to 142, 2 to 253, 3 to 93, 4 to 24, 4 to 148, 5 to 82, 6 to 111, 7 to 88, 8 to 41,
        9 to 93, 11 to 6, 12 to 53, 15 to 2, 17 to 1, 18 to 75, 21 to 1, 23 to 1, 25 to 21, 27 to 56,
        29 to 46, 33 to 31, 36 to 28, 39 to 32, 41 to 47, 46 to 1, 51 to 31, 58 to 1, 67 to 1, 78 to 1)

    @Synchronized
    fun load(ctx: Context) {
        if (base.isNotEmpty()) return
        val o = JSONObject(ctx.assets.open("quran.json").bufferedReader().use { it.readText() })
        val n = o.getJSONArray("names")
        names = List(n.length()) { n.getString(it) }
        val a = o.getJSONArray("ayat")
        base = List(a.length()) { val r = a.getJSONArray(it); Ayah(r.getInt(0), r.getInt(1), r.getString(2), r.getString(3)) }
        baseSource = o.getString("urSource")
        val m = o.getJSONArray("meta")
        surahs = List(m.length()) { val r = m.getJSONArray(it); Surah(r.getString(0), r.getString(1), r.getInt(2), r.getString(3)) }
        for (i in base.indices) if (base[i].a == 1) first[base[i].s] = i
        ayat = base; urSource = baseSource
        val f = File(ctx.filesDir, "custom.json")
        if (f.exists()) try { applyCustom(f.readText()) } catch (e: Exception) { f.delete() }
    }

    /** Accepts [{"s":1,"a":1,"ur":"..","tf":".."}] or {"source":"..","ayat":[...]} . Returns number of ayat matched. */
    fun applyCustom(text: String): Int {
        val t = text.trim()
        var src = "Imported translation"
        val arr = if (t.startsWith("[")) JSONArray(t) else {
            val o = JSONObject(t); src = o.optString("source", src); o.getJSONArray("ayat")
        }
        val map = HashMap<String, JSONObject>()
        for (i in 0 until arr.length()) { val o = arr.getJSONObject(i); map["${o.getInt("s")}:${o.getInt("a")}"] = o }
        var hit = 0
        val res = base.map { b ->
            val o = map["${b.s}:${b.a}"]
            if (o == null) b else {
                hit++
                val ur = o.optString("ur", "").trim()
                Ayah(b.s, b.a, b.ar, if (ur.isEmpty()) b.ur else ur, o.optString("tf", ""), ur.isNotEmpty())
            }
        }
        if (hit == 0) throw IllegalArgumentException("No matching ayat")
        ayat = res; urSource = src
        return hit
    }

    fun reset(ctx: Context) { File(ctx.filesDir, "custom.json").delete(); ayat = base; urSource = baseSource }

    fun indexOf(s: Int, a: Int) = ayat.indexOfFirst { it.s == s && it.a == a }

    fun search(q: String): List<Int> {
        val t = q.trim(); if (t.isEmpty()) return emptyList()
        Regex("^(\\d+)\\s*[:.]\\s*(\\d+)$").find(t)?.let { val i = indexOf(it.groupValues[1].toInt(), it.groupValues[2].toInt()); return if (i >= 0) listOf(i) else emptyList() }
        t.toIntOrNull()?.let { val i = indexOf(it, 1); return if (i >= 0) listOf(i) else emptyList() }
        val byName = names.indexOfFirst { it.replace("-", "").replace("'", "").replace("’", "").contains(t.replace("-", "").replace("'", ""), true) }
        if (byName >= 0) return listOf(indexOf(byName + 1, 1))
        val out = ArrayList<Int>()
        for (i in ayat.indices) { val y = ayat[i]; if (y.ur.contains(t) || y.ar.contains(t) || y.tf.contains(t)) { out.add(i); if (out.size >= 60) break } }
        return out
    }
}
