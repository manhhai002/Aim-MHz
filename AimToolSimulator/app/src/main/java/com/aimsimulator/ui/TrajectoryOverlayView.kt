package com.aimsimulator.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.view.View
import com.aimsimulator.model.BallObservation
import com.aimsimulator.model.ShotPredictor
import com.aimsimulator.model.TableSpec
import kotlin.math.min

/** In-app transparent overlay, same coordinate system as TableView. */
class TrajectoryOverlayView(context: Context) : View(context) {
    var prediction: ShotPredictor.Prediction? = null
        set(value) { field = value; invalidate() }
    var detected: List<BallObservation> = emptyList()
        set(value) { field = value; invalidate() }
    var enabled: Boolean = true
        set(value) { field = value; invalidate() }

    private val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    init { isClickable = false; isFocusable = false }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!enabled) return
        val scale = min(width / TableSpec.WIDTH, height / TableSpec.HEIGHT).toFloat()
        if (scale <= 0f) return
        val dx = (width - TableSpec.WIDTH * scale).toFloat() / 2f
        val dy = (height - TableSpec.HEIGHT * scale).toFloat() / 2f
        canvas.save()
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)
        pen.pathEffect = null
        pen.style = Paint.Style.STROKE
        pen.strokeWidth = 1.8f
        pen.color = Color.argb(160, 218, 252, 207)
        for (ball in detected) {
            canvas.drawCircle(ball.position.x.toFloat(), ball.position.y.toFloat(),
                (ball.radius + 3).toFloat(), pen)
        }
        val path = prediction
        if (path != null) {
            for (seg in path.segments) {
                pen.pathEffect = when (seg.kind) {
                    ShotPredictor.Kind.CUE -> DashPathEffect(floatArrayOf(15f, 8f), 0f)
                    ShotPredictor.Kind.OBJECT -> null
                    ShotPredictor.Kind.CUE_AFTER -> DashPathEffect(floatArrayOf(6f, 7f), 0f)
                }
                pen.color = when (seg.kind) {
                    ShotPredictor.Kind.CUE -> Color.rgb(250, 250, 244)
                    ShotPredictor.Kind.OBJECT -> Color.rgb(255, 206, 79)
                    ShotPredictor.Kind.CUE_AFTER -> Color.rgb(59, 224, 248)
                }
                pen.strokeWidth = if (seg.kind == ShotPredictor.Kind.OBJECT) 3.7f else 2.9f
                canvas.drawLine(seg.start.x.toFloat(), seg.start.y.toFloat(),
                    seg.end.x.toFloat(), seg.end.y.toFloat(), pen)
            }
            path.collisionAt?.let {
                pen.pathEffect = null
                pen.style = Paint.Style.FILL
                pen.color = Color.rgb(255, 212, 90)
                canvas.drawCircle(it.x.toFloat(), it.y.toFloat(), 5f, pen)
            }
        }
        canvas.restore()
    }
}
