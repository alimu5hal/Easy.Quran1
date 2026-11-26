package com.easyquran

import android.content.Context
import android.graphics.Typeface

/** Amiri Quran (Arabic, SIL OFL) and Noto Nastaliq Urdu (SIL OFL), bundled so the text looks right on every phone. */
object Fonts {
    private var ar: Typeface? = null
    private var ur: Typeface? = null
    fun arabic(c: Context): Typeface = ar ?: (try { Typeface.createFromAsset(c.assets, "fonts/AmiriQuran.ttf") } catch (e: Exception) { Typeface.DEFAULT }).also { ar = it }
    fun urdu(c: Context): Typeface = ur ?: (try { Typeface.createFromAsset(c.assets, "fonts/NotoNastaliqUrdu.ttf") } catch (e: Exception) { Typeface.DEFAULT }).also { ur = it }
}
