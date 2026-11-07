package com.cq61tech.chilledandroid

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import java.io.File
import kotlin.concurrent.thread

class MainActivity : Activity(), TextureView.SurfaceTextureListener {

    private var shot: Bitmap? = null
    private var player: MediaPlayer? = null
    private var texture: AspectTextureView? = null
    private var chilled: ChilledView? = null

    private var uiBuilt = false
    private var surfaceReady = false
    private var prepared = false
    private var started = false

    private var lastPos = 0
    private var lastNano = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        noTransition()

        // Pencere seffaf: altta ana ekran gorunur. Hemen root ile ekran goruntusu al.
        thread(name = "screencap") {
            val bmp = captureScreen()
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                if (bmp == null) {
                    Toast.makeText(this, "Ekran goruntusu alinamadi (root izni?)", Toast.LENGTH_LONG).show()
                    finishAndRemoveTask()
                } else {
                    shot = bmp
                    buildUi()
                }
            }
        }
    }

    // ---------- Root ile ekran goruntusu ----------

    private fun captureScreen(): Bitmap? {
        // 1) stdout uzerinden
        try {
            val p = ProcessBuilder("su", "-c", "screencap -p").start()
            val bytes = p.inputStream.readBytes()
            p.waitFor()
            if (bytes.size > 1000) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { return it }
            }
        } catch (_: Exception) {
        }
        // 2) dosya uzerinden (yedek)
        try {
            val f = File(cacheDir, "shot.png")
            val cmd = "screencap -p ${f.absolutePath}; chmod 666 ${f.absolutePath}"
            val p = ProcessBuilder("su", "-c", cmd).start()
            p.waitFor()
            if (f.exists() && f.length() > 1000) {
                val bmp = BitmapFactory.decodeFile(f.absolutePath)
                f.delete()
                if (bmp != null) return bmp
            }
        } catch (_: Exception) {
        }
        return null
    }

    // ---------- Arayuz ve oynatma ----------

    private fun buildUi() {
        if (uiBuilt) return
        uiBuilt = true
        hideSystemBars()

        val alt = BitmapFactory.decodeResource(
            resources, R.drawable.alttab,
            BitmapFactory.Options().apply { inScaled = false }
        )

        val tex = AspectTextureView(this).apply { surfaceTextureListener = this@MainActivity }
        val view = ChilledView(this, shot!!, alt).apply { clock = { clockMs() } }
        texture = tex
        chilled = view

        val root = FrameLayout(this)
        root.setBackgroundColor(Color.BLACK)
        root.addView(
            tex,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        )
        root.addView(
            view,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        setContentView(root)

        // Video (sesi de icinde) ana saat: animasyon bunun konumuna gore calisir.
        val mp = MediaPlayer()
        player = mp
        resources.openRawResourceFd(R.raw.chilled).use { afd ->
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        }
        mp.setOnPreparedListener {
            prepared = true
            maybeStart()
        }
        mp.setOnCompletionListener { finishAndRemoveTask() }
        mp.setOnErrorListener { _, _, _ ->
            finishAndRemoveTask()
            true
        }
        mp.prepareAsync()
    }

    private fun maybeStart() {
        if (started || !prepared || !surfaceReady) return
        started = true
        player?.start()
        chilled?.invalidate()
    }

    /** MediaPlayer konumu kaba guncellenir; aralarini sistem saatiyle doldur. */
    private fun clockMs(): Int {
        val mp = player ?: return 0
        if (!started) return 0
        val pos = try { mp.currentPosition } catch (_: Exception) { return lastPos }
        val now = System.nanoTime()
        if (pos != lastPos) {
            lastPos = pos
            lastNano = now
        }
        val extra = ((now - lastNano) / 1_000_000L).toInt().coerceIn(0, 40)
        return lastPos + extra
    }

    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
        player?.setSurface(Surface(st))
        surfaceReady = true
        maybeStart()
    }

    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {}
    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean = true
    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}

    // ---------- Yardimcilar ----------

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        }
    }

    @Suppress("DEPRECATION")
    private fun noTransition() {
        overridePendingTransition(0, 0)
    }

    override fun onPause() {
        super.onPause()
        // Uygulamadan cikilirsa her sey dursun
        if (started) finishAndRemoveTask()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null
        noTransition()
    }
}
