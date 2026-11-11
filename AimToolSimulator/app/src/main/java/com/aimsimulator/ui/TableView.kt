package com.aimsimulator.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import com.aimsimulator.model.SimulatorEngine
import com.aimsimulator.model.TableSpec
import com.aimsimulator.model.Vec2
import kotlin.math.min

/** A distinct billiards game created for the demo. Never reads any external game. */
class TableView(context: Context) : View(context) {
    val engine = SimulatorEngine()
    var onAimChange: (() -> Unit)? = null
    var onPhysicsChange: (() -> Unit)? = null

    private val brush = Paint(Paint.ANTI_ALIAS_FLAG)
    private var lastFrameTime = 0L
    private var animating = false

    private val ballColors = mapOf(
        "cue" to Color.rgb(245, 247, 250),
        "red" to Color.rgb(240, 91, 85),
        "yellow" to Color.rgb(248, 199, 73),
        "blue" to Color.rgb(74, 153, 243),
        "purple" to Color.rgb(155, 114, 232),
        "orange" to Color.rgb(245, 162, 75),
        "pink" to Color.rgb(218, 111, 169),
        "dark" to Color.rgb(92, 102, 121)
    )
    private val ballLabels = mapOf("red" to "3", "yellow" to "1", "blue" to "2",
        "purple" to "4", "orange" to "5", "pink" to "6", "dark" to "8")

    private fun transform(canvas: Canvas) {
        val scale = min(width / TableSpec.WIDTH, height / TableSpec.HEIGHT).toFloat()
        val dx = (width - TableSpec.WIDTH * scale).toFloat() * 0.5f
        val dy = (height - TableSpec.HEIGHT * scale).toFloat() * 0.5f
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(13, 23, 32))
        canvas.save()
        transform(canvas)
        renderWorld(canvas)
        canvas.restore()
    }

    /** Deterministic 1000x500 screenshot of only the simulation table. */
    fun snapshot(): Bitmap {
        val bitmap = Bitmap.createBitmap(1000, 500, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        renderWorld(canvas)
        return bitmap
    }

    private fun fill(color: Int) {
        brush.color = color
        brush.style = Paint.Style.FILL
        brush.strokeWidth = 1f
    }

    private fun renderWorld(canvas: Canvas) {
        canvas.drawColor(Color.rgb(13, 23, 32))
        fill(Color.rgb(91, 55, 33))
        canvas.drawRoundRect(RectF(14f, 14f, 986f, 486f), 34f, 34f, brush)
        fill(Color.rgb(166, 110, 63))
        canvas.drawRoundRect(RectF(24f, 24f, 976f, 476f), 26f, 26f, brush)
        fill(Color.rgb(16, 75, 57))
        canvas.drawRoundRect(RectF(43f, 43f, 957f, 457f), 17f, 17f, brush)
        fill(Color.rgb(19, 102, 73))
        canvas.drawRect(58f, 58f, 942f, 442f, brush)
        // Table markings and diamonds avoid the ball palette colours.
        fill(Color.rgb(26, 115, 87))
        for (i in 1..5) canvas.drawLine(58f + i * 147.333f, 59f,
            58f + i * 147.333f, 441f, brush)
        fill(Color.rgb(195, 153, 88))
        for (i in 0..7) {
            canvas.drawCircle(93f + i * 115f, 35f, 2.6f, brush)
            canvas.drawCircle(93f + i * 115f, 465f, 2.6f, brush)
        }
        for (p in TableSpec.POCKETS) {
            fill(Color.rgb(10, 17, 23))
            canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), 22f, brush)
            fill(Color.rgb(38, 35, 32))
            canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), 16f, brush)
        }
        val cue = engine.balls.firstOrNull { it.id == "cue" && !it.pocketed }
        if (cue != null && !engine.hasMotion()) {
            val a = cue.position - engine.aim * 24.0
            val b = cue.position - engine.aim * 155.0
            brush.color = Color.rgb(223, 180, 114)
            brush.style = Paint.Style.STROKE
            brush.strokeCap = Paint.Cap.ROUND
            brush.strokeWidth = 6f
            canvas.drawLine(a.x.toFloat(), a.y.toFloat(), b.x.toFloat(), b.y.toFloat(), brush)
            brush.color = Color.rgb(53, 89, 121)
            brush.strokeWidth = 7f
            val t = cue.position - engine.aim * 19.0
            canvas.drawLine(a.x.toFloat(), a.y.toFloat(), t.x.toFloat(), t.y.toFloat(), brush)
            brush.style = Paint.Style.FILL
        }
        for (ball in engine.balls) {
            if (ball.pocketed) continue
            val x = ball.position.x.toFloat()
            val y = ball.position.y.toFloat()
            fill(Color.argb(95, 3, 10, 10))
            canvas.drawCircle(x + 2f, y + 3f, 12.9f, brush)
            fill(ballColors[ball.id] ?: Color.WHITE)
            canvas.drawCircle(x, y, 12f, brush)
            // A small highlight; segmentation still sees an uninterrupted outer ring.
            fill(Color.argb(100, 255, 255, 255))
            canvas.drawCircle(x - 4.5f, y - 4.5f, 2.3f, brush)
            if (ball.id != "cue") {
                brush.isFakeBoldText = true
                brush.textAlign = Paint.Align.CENTER
                brush.textSize = 9f
                brush.color = if (ball.id == "yellow") Color.rgb(62, 54, 46) else Color.WHITE
                canvas.drawText(ballLabels[ball.id] ?: "", x, y + 3.2f, brush)
                brush.isFakeBoldText = false
            }
        }
    }

    private fun screenToWorld(x: Float, y: Float): Vec2 {
        val scale = min(width / TableSpec.WIDTH, height / TableSpec.HEIGHT)
        if (scale <= 0.0) return Vec2.ZERO
        val dx = (width - TableSpec.WIDTH * scale) * 0.5
        val dy = (height - TableSpec.HEIGHT * scale) * 0.5
        return Vec2((x - dx) / scale, (y - dy) / scale)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN ||
            event.actionMasked == MotionEvent.ACTION_MOVE) {
            if (!engine.hasMotion()) {
                engine.changeAim(screenToWorld(event.x, event.y))
                invalidate()
                onAimChange?.invoke()
            }
            return true
        }
        return event.actionMasked == MotionEvent.ACTION_UP || super.onTouchEvent(event)
    }

    fun reset(random: Boolean) {
        if (random) engine.randomize() else engine.reset()
        animating = false
        invalidate()
        onPhysicsChange?.invoke()
    }

    fun shoot() {
        if (!engine.shoot()) return
        animating = true
        lastFrameTime = SystemClock.uptimeMillis()
        postOnAnimation(frameCallback)
        onPhysicsChange?.invoke()
    }

    private val frameCallback = object : Runnable {
        override fun run() {
            if (!animating) return
            val now = SystemClock.uptimeMillis()
            val dt = ((now - lastFrameTime) / 1000.0).coerceIn(0.0, 0.04)
            lastFrameTime = now
            engine.step(dt)
            invalidate()
            onPhysicsChange?.invoke()
            if (engine.hasMotion()) postOnAnimation(this) else animating = false
        }
    }

    override fun onDetachedFromWindow() {
        animating = false
        super.onDetachedFromWindow()
    }
}
