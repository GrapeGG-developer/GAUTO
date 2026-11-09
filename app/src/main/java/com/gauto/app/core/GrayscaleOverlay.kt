package com.gauto.app.core

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RenderEffect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.WindowManager

/**
 * Монохромный слой без ADB и root.
 *
 * Сначала пробуем настоящий системный далтонизатор (WRITE_SECURE_SETTINGS).
 * Обычному приложению его не выдают, поэтому при SecurityException включается
 * «приглушающий» оверлей: полупрозрачный серый слой с нулевой насыщенностью,
 * который не кликается, не фокусируется и снимается по окончании правила.
 */
object GrayscaleOverlay {

    @Volatile private var overlay: View? = null
    @Volatile private var manager: WindowManager? = null

    fun show(context: Context) {
        if (overlay != null) return
        if (!Settings.canDrawOverlays(context)) return
        val app = context.applicationContext
        Handler(Looper.getMainLooper()).post {
            if (overlay != null) return@post
            val view = DesaturateView(app)
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            )
            val wm = app.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return@post
            runCatching {
                wm.addView(view, lp)
                overlay = view
                manager = wm
            }
        }
    }

    fun hide() {
        Handler(Looper.getMainLooper()).post {
            val view = overlay ?: return@post
            runCatching { manager?.removeViewImmediate(view) }
            overlay = null
            manager = null
        }
    }

    private class DesaturateView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(96, 128, 128, 128)
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        }

        init {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                runCatching {
                    setRenderEffect(
                        RenderEffect.createColorFilterEffect(
                            ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                        )
                    )
                }
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawPaint(paint)
        }
    }
}

object GrayscaleController {
    private const val KEY_ENABLED = "accessibility_display_daltonizer_enabled"
    private const val KEY_MODE = "accessibility_display_daltonizer"

    fun apply(context: Context, enabled: Boolean) {
        if (enabled) {
            val system = trySystem(context, true)
            if (system) GrayscaleOverlay.hide() else GrayscaleOverlay.show(context)
        } else {
            trySystem(context, false)
            GrayscaleOverlay.hide()
        }
    }

    private fun trySystem(context: Context, on: Boolean): Boolean = runCatching {
        val cr = context.contentResolver
        Settings.Secure.putInt(cr, KEY_ENABLED, if (on) 1 else 0)
        if (on) Settings.Secure.putInt(cr, KEY_MODE, 0)
        true
    }.getOrDefault(false)
}
