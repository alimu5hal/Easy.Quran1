package com.easyquran

import android.app.WallpaperManager
import android.content.Context

object Wall {
    /** Moves to the next ayah and a new background. Cheap, so it can run straight inside a broadcast. */
    fun advance(ctx: Context) {
        Quran.load(ctx)
        Prefs.setIndex(ctx, (Prefs.index(ctx) + 1) % Quran.ayat.size)
        Prefs.setSeed(ctx, Prefs.seed(ctx) + 1)
    }

    /** Draws the current ayah and sets it as wallpaper. Slow: call from a background thread. */
    fun apply(ctx: Context) {
        Quran.load(ctx)
        val idx = Prefs.index(ctx)
        val dm = ctx.resources.displayMetrics
        val w = minOf(dm.widthPixels, dm.heightPixels)
        val h = maxOf(dm.widthPixels, dm.heightPixels)
        val (bmp, _) = Scene.render(ctx, w, h, Quran.ayat[idx], Prefs.seed(ctx))
        val flags = when (Prefs.target(ctx)) {
            1 -> WallpaperManager.FLAG_LOCK
            2 -> WallpaperManager.FLAG_SYSTEM
            else -> WallpaperManager.FLAG_LOCK or WallpaperManager.FLAG_SYSTEM
        }
        WallpaperManager.getInstance(ctx).setBitmap(bmp, null, true, flags)
        bmp.recycle()
        Prefs.setApplied(ctx, idx)
    }
}
