package com.cq61tech.chilledandroid

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tum efektleri cizen view. Zamani [clock] (ms) verir; kare = ms / 33.33333.
 */
class ChilledView(
    context: Context,
    private val shot: Bitmap,
    private val alt: Bitmap,
) : View(context) {

    var clock: () -> Int = { 0 }

    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val dst = RectF()
    private var zoom = 1.66f
    private var videoTop = 0f
    private var videoH = 0f

    // alttab.jpg (854x480) icindeki sol kucuk resimdeki iki sutun
    private val altLeft = RectF(118f, 169f, 241f, 310f)
    private val altRight = RectF(243f, 169f, 365f, 310f)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        videoH = w * 480f / 854f
        videoTop = (h - videoH) / 2f
        zoom = solveZoom(w.toFloat(), h.toFloat(), Timeline.ROT_DEG, Timeline.BLACK_CORNER_FRACTION)
    }

    override fun onDraw(c: Canvas) {
        val t = clock()
        if (t >= Timeline.VIDEO_START_MS) {
            // Seffaf birak: alttaki video gorunsun. Tekrar cizmeye gerek yok.
            return
        }
        val frame = (t / Timeline.FRAME_MS).toInt()
        val w = width.toFloat()
        val h = height.toFloat()
        c.drawColor(Color.BLACK)

        when {
            frame < Timeline.PHASE1_END -> drawPhase1(c, frame, w, h)
            else -> {
                drawPhase2(c, frame, w, h)
                if (frame >= Timeline.ALTTAB) {
                    val a = ((t - Timeline.ALTTAB * Timeline.FRAME_MS) / Timeline.ALT_FADE_MS)
                        .toFloat().coerceIn(0f, 1f)
                    drawAltTab(c, w, h, a)
                }
            }
        }
        postInvalidateOnAnimation()
    }

    /** 0 - 14.6 sn: tam ekran, ayna cevirme + donus/zoom */
    private fun drawPhase1(c: Canvas, frame: Int, w: Float, h: Float) {
        val flips = Timeline.countLE(Timeline.A, frame)
        val sx = if (flips % 2 == 0) 1f else -1f
        var rot = 0f
        var z = 1f
        if (frame >= Timeline.ROT_START) {
            z = zoom
            rot = if (frame < Timeline.ROT_SWITCH) Timeline.ROT_DEG else -Timeline.ROT_DEG
        }
        c.save()
        c.translate(w / 2f, h / 2f)
        c.rotate(rot)
        c.scale(sx * z, z)
        c.translate(-w / 2f, -h / 2f)
        dst.set(0f, 0f, w, h)
        c.drawBitmap(shot, null, dst, paint)
        c.restore()
    }

    /** 14.6 - 20.7 sn: iki sutun, sol B, sag C ile ayna cevirir */
    private fun drawPhase2(c: Canvas, frame: Int, w: Float, h: Float) {
        val gap = 6f * w / 854f
        val tileW = (w - gap) / 2f
        val leftFlip = Timeline.countLE(Timeline.B, frame) % 2 == 1
        val rightFlip = Timeline.countLE(Timeline.C, frame) % 2 == 1
        drawTile(c, RectF(0f, 0f, tileW, h), leftFlip)
        drawTile(c, RectF(tileW + gap, 0f, w, h), rightFlip)
    }

    private fun drawTile(c: Canvas, r: RectF, flip: Boolean) {
        c.save()
        c.clipRect(r)
        if (flip) c.scale(-1f, 1f, r.centerX(), r.centerY())
        c.drawBitmap(shot, null, r, paint)
        c.restore()
    }

    /** Alt+Tab ekrani: soldaki kucuk resimlere bizim ekran goruntusu konur */
    private fun drawAltTab(c: Canvas, w: Float, h: Float, alpha: Float) {
        c.saveLayerAlpha(0f, 0f, w, h, (alpha * 255f).toInt())
        c.drawColor(Color.BLACK)
        dst.set(0f, videoTop, w, videoTop + videoH)
        c.drawBitmap(alt, null, dst, paint)
        val sx = w / 854f
        val sy = videoH / 480f
        for (r in arrayOf(altLeft, altRight)) {
            dst.set(r.left * sx, videoTop + r.top * sy, r.right * sx, videoTop + r.bottom * sy)
            c.drawBitmap(shot, null, dst, paint)
        }
        c.restore()
    }

    /**
     * Videodaki siyah kose orani (~%2.3) ayni kalsin diye, ekran boyutuna gore
     * zoom degerini bulur (video 16:9, telefon dikey).
     */
    private fun solveZoom(w: Float, h: Float, deg: Float, target: Float): Float {
        val rad = Math.toRadians(deg.toDouble())
        val cs = cos(rad).toFloat()
        val sn = sin(rad).toFloat()
        fun frac(s: Float): Float {
            var out = 0
            val nx = 48
            val ny = 96
            for (i in 0 until nx) for (j in 0 until ny) {
                val x = (i + 0.5f) / nx * w - w / 2f
                val y = (j + 0.5f) / ny * h - h / 2f
                val u = (x * cs + y * sn) / s
                val v = (-x * sn + y * cs) / s
                if (abs(u) > w / 2f || abs(v) > h / 2f) out++
            }
            return out / (nx * ny).toFloat()
        }
        var lo = 1f
        var hi = 6f
        repeat(24) {
            val mid = (lo + hi) / 2f
            if (frac(mid) > target) lo = mid else hi = mid
        }
        return hi
    }
}
