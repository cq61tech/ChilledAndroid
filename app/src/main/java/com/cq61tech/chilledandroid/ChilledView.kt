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
 *
 *  0.0 - 14.6 sn : tam ekran, ayna cevirme + donus/zoom        (opak)
 * 14.6 - 20.77 sn: iki sutun, sol/sag ayri ayna cevirme          (opak)
 * 20.77 - 22.1 sn: SEFFAF; altta video (zoom-out / alt+tab / BSOD zoom-in) oynar,
 *                  sadece sol pencerenin icine ekran goruntusu cizilir
 * sonrasi        : hicbir sey cizilmez, video gorunur
 */
class ChilledView(
    context: Context,
    private val shot: Bitmap,
) : View(context) {

    var clock: () -> Int = { 0 }

    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val dst = RectF()
    private var zoom = 1.66f
    private var videoTop = 0f
    private var videoH = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        videoH = w * 480f / 854f
        videoTop = (h - videoH) / 2f
        zoom = solveZoom(w.toFloat(), h.toFloat(), Timeline.ROT_DEG, Timeline.BLACK_CORNER_FRACTION)
    }

    override fun onDraw(c: Canvas) {
        val t = clock()
        val w = width.toFloat()
        val h = height.toFloat()
        val frame = (t / Timeline.FRAME_MS).toInt()

        when {
            frame < Timeline.PHASE1_END -> {
                c.drawColor(Color.BLACK)
                drawPhase1(c, frame, w, h)
            }
            frame < Timeline.OVERLAY_START -> {
                c.drawColor(Color.BLACK)
                drawPhase2(c, frame, w, h)
            }
            else -> {
                val vf = ((t - Timeline.VIDEO_LATENCY_MS) / Timeline.FRAME_MS).toInt()
                if (vf > Timeline.OVERLAY_END) return   // bitti, video kendi basina
                drawAltWindow(c, vf, w)
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

    /** Videodaki sol pencerenin (zoom-out ile kuculen) icine iki sutun ekran goruntusu */
    private fun drawAltWindow(c: Canvas, frame: Int, w: Float) {
        val i = frame - Timeline.ALTTAB
        val r = if (i >= 0 && i * 4 + 3 < Timeline.ALT_RECTS.size) {
            Timeline.ALT_RECTS.copyOfRange(i * 4, i * 4 + 4)
        } else {
            Timeline.ALT_REST
        }
        val sx = w / 854f
        val sy = videoH / 480f
        val x0 = r[0] * sx
        val y0 = videoTop + r[1] * sy
        val x1 = r[2] * sx
        val y1 = videoTop + r[3] * sy
        val mid = (x0 + x1) / 2f
        val g = sx
        dst.set(x0, y0, mid - g, y1)
        c.drawBitmap(shot, null, dst, paint)
        dst.set(mid + g, y0, x1, y1)
        c.drawBitmap(shot, null, dst, paint)
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
