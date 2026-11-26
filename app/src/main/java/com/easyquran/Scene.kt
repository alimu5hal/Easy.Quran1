package com.easyquran

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import java.io.File
import java.util.Random

/** Draws original illustrated scenes of holy places (or the user's own photos) and writes the ayah on top
 *  in a colour chosen from the brightness of the area behind the text. */
object Scene {
    private fun argb(v: Long) = v.toInt()
    private class Sty(val sky: IntArray, val ground: Int, val body: Int, val dome: Int, val accent: Int)
    private val gold = argb(0xFFE2B84A)
    private val styles = listOf(
        Sty(intArrayOf(argb(0xFF050A1F), argb(0xFF14285A), argb(0xFF2E4A86)), argb(0xFF22222A), 0, 0, gold),                      // Kaaba, night
        Sty(intArrayOf(argb(0xFF0B3B3C), argb(0xFF1F7A6B), argb(0xFFF2C894)), argb(0xFF2B2417), argb(0xFFE8DCC0), argb(0xFF2E8B57), gold), // Nabawi, dawn
        Sty(intArrayOf(argb(0xFF2B1B4A), argb(0xFFB5476A), argb(0xFFF59E42)), argb(0xFF1E1A22), argb(0xFF2F5FA8), argb(0xFFE0A526), gold), // Dome of the Rock, sunset
        Sty(intArrayOf(argb(0xFF1B1038), argb(0xFF7B3F7A), argb(0xFFF4A261)), argb(0xFF2A1A18), argb(0xFFA23B2A), argb(0xFFEDE6D6), gold), // Badshahi, dusk
        Sty(intArrayOf(argb(0xFF0F172A), argb(0xFF3B4C7A), argb(0xFF9FB3D9)), argb(0xFF151922), argb(0xFFB9C4D6), argb(0xFF7F93B5), gold), // Sultan Ahmed (Blue) Mosque, twilight
        Sty(intArrayOf(argb(0xFF2A1F3D), argb(0xFF9A5B4F), argb(0xFFF7C77E)), argb(0xFF2A211C), argb(0xFFC9A57A), argb(0xFF8A93A3), gold), // Hagia Sophia, golden dusk
        Sty(intArrayOf(argb(0xFF1F3A5F), argb(0xFF5C7FA3), argb(0xFFE9C9A0)), argb(0xFF1C2630), argb(0xFFF1F1EE), argb(0xFFF1F1EE), gold), // Faisal Mosque, Islamabad
        Sty(intArrayOf(argb(0xFF1A1330), argb(0xFF6A4C93), argb(0xFFFFB88C)), argb(0xFF241E2A), argb(0xFFF4F1EA), argb(0xFFFFFFFF), gold), // Sheikh Zayed Mosque
        Sty(intArrayOf(argb(0xFF0D1B2A), argb(0xFF3D5A80), argb(0xFFEE9B6A)), argb(0xFF262019), argb(0xFFD8C7A3), argb(0xFF9AA3AE), gold)  // Al-Aqsa (Qibli) Mosque, dawn
    )
    val places = listOf("Kaaba, Makkah", "Masjid an-Nabawi, Madinah", "Dome of the Rock, Jerusalem", "Badshahi Mosque, Lahore", "Sultan Ahmed (Blue) Mosque, Istanbul", "Hagia Sophia, Istanbul", "Faisal Mosque, Islamabad", "Sheikh Zayed Mosque, Abu Dhabi", "Al-Aqsa Mosque, Jerusalem")

    private fun photos(ctx: Context) = File(ctx.filesDir, "bg").listFiles()?.filter { it.isFile }?.sortedBy { it.name } ?: emptyList()

    fun render(ctx: Context, w: Int, h: Int, ay: Ayah, seed: Int): Pair<Bitmap, String> {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val ph = photos(ctx)
        val pick = Math.floorMod(seed, styles.size + ph.size)
        var place: String
        if (pick < styles.size) { draw(c, w, h, pick, Random(seed.toLong() * 31 + 7)); place = places[pick] }
        else if (drawPhoto(c, w, h, ph[pick - styles.size])) place = "Your photo"
        else { draw(c, w, h, 0, Random(1)); place = places[0] }

        // text box (below the lock-screen clock)
        val boxW = (w * 0.84f).toInt()
        val top = h * 0.27f
        val maxH = h * 0.40f
        var k = 1f
        var ar = lay(ay.ar, w * 0.062f, Color.WHITE, boxW, Fonts.arabic(ctx)); var ur = lay(ay.ur, w * 0.040f, Color.WHITE, boxW, Fonts.urdu(ctx))
        val gap = h * 0.02f
        while (true) {
            ar = lay(ay.ar, w * 0.062f * k, Color.WHITE, boxW, Fonts.arabic(ctx))
            ur = lay(ay.ur, w * 0.040f * k, Color.WHITE, boxW, Fonts.urdu(ctx))
            if (ar.height + ur.height + gap <= maxH || k < 0.3f) break
            k *= 0.93f
        }
        val total = ar.height + ur.height + gap
        val region = Rect(((w - boxW) / 2), top.toInt(), ((w + boxW) / 2), (top + total).toInt())
        val dark = luminance(bmp, region) > 0.6f
        val col = if (dark) argb(0xFF101010) else Color.WHITE
        ar = lay(ay.ar, w * 0.062f * k, col, boxW, Fonts.arabic(ctx))
        ur = lay(ay.ur, w * 0.040f * k, col, boxW, Fonts.urdu(ctx))

        val scrim = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (dark) argb(0x66FFFFFF) else argb(0x66000000) }
        val pad = w * 0.04f
        c.drawRoundRect(region.left - pad, region.top - pad, region.right + pad, region.bottom + pad + w * 0.07f, 40f, 40f, scrim)
        c.save(); c.translate(region.left.toFloat(), top); ar.draw(c)
        c.translate(0f, ar.height + gap); ur.draw(c); c.restore()
        val rp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = col; textSize = w * 0.032f; textAlign = Paint.Align.CENTER; alpha = 200 }
        val ref = "${Quran.names[ay.s - 1]}  ${ay.s}:${ay.a}"
        c.drawText(ref, w / 2f, region.bottom + pad + w * 0.035f, rp)
        return Pair(bmp, place)
    }

    private fun lay(text: String, size: Float, color: Int, width: Int, tf: Typeface): StaticLayout {
        val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; textSize = size; typeface = tf }
        return StaticLayout.Builder.obtain(text, 0, text.length, p, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER).setTextDirection(TextDirectionHeuristics.RTL)
            .setLineSpacing(0f, 1.35f).build()
    }

    private fun luminance(b: Bitmap, r: Rect): Float {
        var sum = 0f; var n = 0
        for (i in 0 until 16) for (j in 0 until 16) {
            val x = (r.left + (r.width() - 1) * i / 15).coerceIn(0, b.width - 1)
            val y = (r.top + (r.height() - 1) * j / 15).coerceIn(0, b.height - 1)
            val p = b.getPixel(x, y)
            sum += (0.299f * Color.red(p) + 0.587f * Color.green(p) + 0.114f * Color.blue(p)) / 255f; n++
        }
        return sum / n
    }

    private fun drawPhoto(c: Canvas, w: Int, h: Int, f: File): Boolean = try {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, o)
        val o2 = BitmapFactory.Options().apply { inSampleSize = maxOf(1, minOf(o.outWidth / w, o.outHeight / h)) }
        val b = BitmapFactory.decodeFile(f.path, o2)!!
        val s = maxOf(w / b.width.toFloat(), h / b.height.toFloat())
        val m = Matrix().apply { postScale(s, s); postTranslate((w - b.width * s) / 2, (h - b.height * s) / 2) }
        c.drawBitmap(b, m, Paint(Paint.FILTER_BITMAP_FLAG)); true
    } catch (e: Throwable) { false }

    private fun draw(c: Canvas, w: Int, h: Int, kind: Int, rnd: Random) {
        val st = styles[kind]; val W = w.toFloat(); val H = h.toFloat()
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, 0f, H * 0.8f, st.sky, floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, W, H, p); p.shader = null
        p.color = Color.WHITE
        repeat(80) { p.alpha = 60 + rnd.nextInt(170); c.drawCircle(rnd.nextFloat() * W, rnd.nextFloat() * H * 0.5f, 1f + rnd.nextFloat() * 2.2f, p) }
        p.alpha = 255
        // crescent moon
        val mc = Path().apply { addCircle(W * 0.8f, H * 0.12f, W * 0.055f, Path.Direction.CW) }
        val cut = Path().apply { addCircle(W * 0.8f + W * 0.03f, H * 0.12f - W * 0.01f, W * 0.05f, Path.Direction.CW) }
        mc.op(cut, Path.Op.DIFFERENCE); p.color = argb(0xFFF6EBC0); c.drawPath(mc, p)
        val gy = H * 0.78f
        p.color = st.ground; c.drawRect(0f, gy, W, H, p)
        when (kind) { 0 -> kaaba(c, W, H, gy, st); 6 -> faisal(c, W, H, gy, st); else -> mosque(c, W, H, gy, st, kind) }
        p.shader = LinearGradient(0f, gy, 0f, H, argb(0x33FFFFFF), argb(0x00FFFFFF), Shader.TileMode.CLAMP)
        c.drawRect(0f, gy, W, H, p)
    }

    private fun kaaba(c: Canvas, W: Float, H: Float, gy: Float, st: Sty) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val bw = W * 0.34f; val bh = W * 0.40f; val x = (W - bw) / 2; val y = gy - bh
        p.color = argb(0x33000000); // far minarets
        for (mx in listOf(W * 0.1f, W * 0.9f, W * 0.2f, W * 0.8f)) c.drawRect(mx - W * 0.012f, gy - H * 0.18f, mx + W * 0.012f, gy, p)
        p.shader = LinearGradient(x, 0f, x + bw, 0f, intArrayOf(argb(0xFF0A0A0A), argb(0xFF1C1C1C), argb(0xFF0A0A0A)), null, Shader.TileMode.CLAMP)
        c.drawRect(x, y, x + bw, gy, p); p.shader = null
        p.color = argb(0xFF2A2A2A); val roof = Path().apply { moveTo(x, y); lineTo(x + bw * 0.08f, y - bh * 0.05f); lineTo(x + bw * 0.92f, y - bh * 0.05f); lineTo(x + bw, y); close() }; c.drawPath(roof, p)
        p.color = st.accent; c.drawRect(x, y + bh * 0.16f, x + bw, y + bh * 0.26f, p)       // gold belt
        p.color = argb(0x99E2B84A); for (i in 0 until 5) c.drawRect(x + bw * (0.1f + 0.17f * i), y + bh * 0.17f, x + bw * (0.1f + 0.17f * i) + bw * 0.1f, y + bh * 0.25f, p)
        p.color = st.accent; c.drawRoundRect(x + bw * 0.62f, y + bh * 0.52f, x + bw * 0.86f, gy, 6f, 6f, p) // door
        p.color = argb(0x33FFFFFF); c.drawOval(x - bw * 0.3f, gy - 8f, x + bw * 1.3f, gy + 24f, p)
    }

    private fun mosque(c: Canvas, W: Float, H: Float, gy: Float, st: Sty, kind: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val hallTop = gy - H * 0.09f
        val cx = W / 2
        val r = when (kind) { 1 -> W * 0.15f; 2 -> W * 0.14f; 3 -> W * 0.16f; 5 -> W * 0.19f; 7 -> W * 0.15f; 8 -> W * 0.12f; else -> W * 0.17f }
        // glow
        p.shader = RadialGradient(cx, hallTop - r * 0.5f, r * 2.2f, argb(0x55FFE9A8), argb(0x00FFE9A8), Shader.TileMode.CLAMP)
        c.drawCircle(cx, hallTop - r * 0.5f, r * 2.2f, p); p.shader = null
        // minarets
        val xs = when (kind) { 1 -> listOf(0.12f, 0.88f); 2 -> emptyList<Float>(); 3 -> listOf(0.1f, 0.9f, 0.2f, 0.8f); 5 -> listOf(0.1f, 0.9f, 0.2f, 0.8f); 7 -> listOf(0.07f, 0.93f, 0.17f, 0.83f); 8 -> listOf(0.88f); else -> listOf(0.08f, 0.92f, 0.18f, 0.82f) }
        for (f in xs) {
            val mx = W * f; val mw = W * 0.026f; val mt = gy - H * (if (kind == 3) 0.2f else 0.24f)
            p.color = st.body; c.drawRect(mx - mw, mt, mx + mw, gy, p)
            p.color = st.accent; c.drawRect(mx - mw * 1.6f, mt + H * 0.03f, mx + mw * 1.6f, mt + H * 0.038f, p)
            p.color = st.dome; c.drawArc(RectF(mx - mw * 1.3f, mt - mw * 2.6f, mx + mw * 1.3f, mt + mw * 2.6f), 180f, 180f, true, p)
            p.color = st.accent; c.drawRect(mx - 2f, mt - mw * 4.2f, mx + 2f, mt - mw * 2.4f, p)
        }
        // hall
        val hl = if (kind == 2) cx - r * 1.1f else W * 0.2f; val hr = if (kind == 2) cx + r * 1.1f else W * 0.8f
        val top = if (kind == 2) hallTop - H * 0.05f else hallTop
        p.color = st.body; c.drawRect(hl, top, hr, gy, p)
        p.color = argb(0x66FFD27A); val n = 7
        for (i in 0 until n) { val ax = hl + (hr - hl) * (i + 0.5f) / n; c.drawRoundRect(ax - W * 0.012f, top + H * 0.02f, ax + W * 0.012f, gy - H * 0.01f, 10f, 10f, p) }
        // side domes
        if (kind != 2) for (f in listOf(0.3f, 0.7f)) { p.color = st.dome; c.drawArc(RectF(W * f - r * 0.45f, top - r * 0.45f, W * f + r * 0.45f, top + r * 0.45f), 180f, 180f, true, p) }
        // main dome
        p.color = st.dome; c.drawArc(RectF(cx - r, top - r, cx + r, top + r), 180f, 180f, true, p)
        p.color = argb(0x22000000); c.drawArc(RectF(cx - r, top - r, cx + r, top + r), 180f, 90f, true, p)
        p.color = st.accent; c.drawRect(cx - 3f, top - r - H * 0.035f, cx + 3f, top - r + 4f, p); c.drawCircle(cx, top - r - H * 0.035f, 7f, p)
        if (kind == 5) { p.color = st.dome; for (d in listOf(-0.95f, 0.95f)) c.drawArc(RectF(cx + r * d - r * 0.6f, top - r * 0.6f + r * 0.1f, cx + r * d + r * 0.6f, top + r * 0.6f + r * 0.1f), 180f, 180f, true, p)
            p.color = st.body; for (d in listOf(-1.7f, 1.7f)) c.drawRect(cx + r * d - r * 0.18f, top - r * 0.4f, cx + r * d + r * 0.18f, gy, p) }
        if (kind == 7) { p.color = st.dome; for (f in listOf(0.22f, 0.78f, 0.38f, 0.62f)) c.drawArc(RectF(W * f - r * 0.33f, top - r * 0.33f, W * f + r * 0.33f, top + r * 0.33f), 180f, 180f, true, p) }
        if (kind == 4) { p.color = st.dome; for (d in listOf(-0.62f, 0.62f)) c.drawArc(RectF(cx + r * d - r * 0.5f, top - r * 0.5f, cx + r * d + r * 0.5f, top + r * 0.5f), 180f, 180f, true, p) }
    }

    private fun faisal(c: Canvas, W: Float, H: Float, gy: Float, st: Sty) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = argb(0xFF2B3A4F)   // Margalla hills
        c.drawPath(Path().apply { moveTo(0f, gy); lineTo(0f, gy - H * 0.12f); lineTo(W * 0.25f, gy - H * 0.2f); lineTo(W * 0.5f, gy - H * 0.14f); lineTo(W * 0.78f, gy - H * 0.23f); lineTo(W, gy - H * 0.15f); lineTo(W, gy); close() }, p)
        val cx = W / 2
        fun tri(x1: Float, x2: Float, apexX: Float, apexY: Float, color: Int) {
            p.color = color; c.drawPath(Path().apply { moveTo(x1, gy); lineTo(apexX, apexY); lineTo(x2, gy); close() }, p)
        }
        tri(cx - W * 0.34f, cx + W * 0.34f, cx, gy - H * 0.19f, argb(0xFFE4E4E0))
        tri(cx - W * 0.24f, cx + W * 0.24f, cx, gy - H * 0.14f, st.body)
        p.color = argb(0x33000000); c.drawPath(Path().apply { moveTo(cx, gy - H * 0.19f); lineTo(cx + W * 0.34f, gy); lineTo(cx, gy); close() }, p)
        for (f in listOf(0.1f, 0.9f, 0.19f, 0.81f)) {   // four slender minarets
            val mx = W * f; val mw = W * 0.012f; val top = gy - H * 0.3f
            p.color = st.body; c.drawRect(mx - mw, top, mx + mw, gy, p)
            c.drawPath(Path().apply { moveTo(mx - mw, top); lineTo(mx, top - H * 0.04f); lineTo(mx + mw, top); close() }, p)
            p.color = st.accent; c.drawRect(mx - mw * 1.8f, top + H * 0.05f, mx + mw * 1.8f, top + H * 0.056f, p)
        }
        p.color = argb(0x66FFD27A); c.drawRoundRect(cx - W * 0.03f, gy - H * 0.06f, cx + W * 0.03f, gy, 12f, 12f, p)
    }
}
