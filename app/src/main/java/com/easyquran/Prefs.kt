package com.easyquran

import android.content.Context

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("eq", Context.MODE_PRIVATE)
    fun index(c: Context) = sp(c).getInt("i", 0)
    fun setIndex(c: Context, i: Int) = sp(c).edit().putInt("i", i).apply()
    fun seed(c: Context) = sp(c).getInt("seed", 0)
    fun setSeed(c: Context, v: Int) = sp(c).edit().putInt("seed", v).apply()
    fun unlock(c: Context) = sp(c).getBoolean("unlock", false)
    fun setUnlock(c: Context, v: Boolean) = sp(c).edit().putBoolean("unlock", v).apply()
    fun target(c: Context) = sp(c).getInt("target", 0) // 0 both, 1 lock, 2 home
    fun setTarget(c: Context, v: Int) = sp(c).edit().putInt("target", v).apply()
    fun reminder(c: Context) = sp(c).getInt("rem", -1) // minutes after midnight, -1 = off
    fun setReminder(c: Context, v: Int) = sp(c).edit().putInt("rem", v).apply()
    fun bookmarks(c: Context): MutableSet<String> = HashSet(sp(c).getStringSet("bm", emptySet())!!)
    fun setBookmarks(c: Context, s: Set<String>) = sp(c).edit().putStringSet("bm", s).apply()
    fun note(c: Context, i: Int) = sp(c).getString("n$i", "") ?: ""
    fun setNote(c: Context, i: Int, t: String) = sp(c).edit().putString("n$i", t).apply()
    fun lastRead(c: Context) = sp(c).getInt("last", 0)
    fun setLastRead(c: Context, i: Int) = sp(c).edit().putInt("last", i).apply()
    fun readerSize(c: Context) = sp(c).getInt("rsize", 28)
    fun setReaderSize(c: Context, v: Int) = sp(c).edit().putInt("rsize", v).apply()
    fun trigger(c: Context) = sp(c).getInt("trig", 0) // 0 = on unlock, 1 = on screen on
    fun setTrigger(c: Context, v: Int) = sp(c).edit().putInt("trig", v).apply()
    fun applied(c: Context) = sp(c).getInt("applied", -1)
    fun setApplied(c: Context, v: Int) = sp(c).edit().putInt("applied", v).apply()
    fun unlocks(c: Context) = sp(c).getInt("unlocks", 0)
    fun lastUnlock(c: Context) = sp(c).getLong("lastUnlock", 0L)
    fun countUnlock(c: Context) = sp(c).edit().putInt("unlocks", unlocks(c) + 1).putLong("lastUnlock", System.currentTimeMillis()).apply()
}
