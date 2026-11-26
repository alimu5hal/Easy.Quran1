package com.easyquran

import android.os.Build
import android.view.View
import android.view.WindowInsets

/** Android 15+ draws apps edge-to-edge; keep content clear of the status and navigation bars. */
fun View.fitSystemBars() {
    setOnApplyWindowInsetsListener { v, ins ->
        if (Build.VERSION.SDK_INT >= 30) {
            val b = ins.getInsets(WindowInsets.Type.systemBars())
            v.setPadding(b.left, b.top, b.right, b.bottom)
        }
        ins
    }
}

fun android.content.Context.px(v: Int) = (v * resources.displayMetrics.density).toInt()
fun android.content.Context.cardBg(color: Long = 0xFF14302A, radius: Int = 16) =
    android.graphics.drawable.GradientDrawable().apply { setColor(color.toInt()); cornerRadius = px(radius).toFloat() }
