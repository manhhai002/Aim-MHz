package com.aimsimulator.vision

import android.graphics.Bitmap
import com.aimsimulator.model.BallObservation
import com.aimsimulator.model.TableSpec
import com.aimsimulator.model.Vec2
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * Detects balls FROM PIXELS in the simulated table bitmap.
 * The colour palette is intentionally known (colour segmentation is robust in this controlled MVP).
 * The predictor receives only these observations and the user's aiming direction.
 */
class BallDetector {
    private data class BallColour(val id: String, val red: Int, val green: Int, val blue: Int)
    private val palette = listOf(
        BallColour("cue", 245, 247, 250),
        BallColour("red", 240, 91, 85),
        BallColour("yellow", 248, 199, 73),
        BallColour("blue", 74, 153, 243),
        BallColour("purple", 155, 114, 232),
        BallColour("orange", 245, 162, 75),
        BallColour("pink", 218, 111, 169),
        BallColour("dark", 92, 102, 121)
    )

    fun detect(frame: Bitmap): List<BallObservation> {
        val rgba = Mat()
        val rgb = Mat()
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(3.0, 3.0))
        try {
            Utils.bitmapToMat(frame, rgba)
            Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
            val balls = mutableListOf<BallObservation>()
            for (c in palette) {
                val mask = Mat()
                val contours = mutableListOf<MatOfPoint>()
                val hierarchy = Mat()
                try {
                    fun low(v: Int) = (v - 34).coerceAtLeast(0).toDouble()
                    fun high(v: Int) = (v + 34).coerceAtMost(255).toDouble()
                    Core.inRange(rgb,
                        Scalar(low(c.red), low(c.green), low(c.blue)),
                        Scalar(high(c.red), high(c.green), high(c.blue)), mask)
                    Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel)
                    Imgproc.findContours(mask, contours, hierarchy,
                        Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
                    var best: BallObservation? = null
                    var bestArea = 0.0
                    for (contour in contours) {
                        val area = Imgproc.contourArea(contour)
                        if (area < 165.0 || area > 950.0) continue
                        val points = MatOfPoint2f(*contour.toArray())
                        try {
                            val center = Point()
                            val radius = FloatArray(1)
                            Imgproc.minEnclosingCircle(points, center, radius)
                            val r = radius[0].toDouble()
                            if (r !in 8.5..14.8) continue
                            if (center.x !in (TableSpec.LEFT + 9.0)..(TableSpec.RIGHT - 9.0)) continue
                            if (center.y !in (TableSpec.TOP + 9.0)..(TableSpec.BOTTOM - 9.0)) continue
                            if (area > bestArea) {
                                bestArea = area
                                best = BallObservation(c.id, Vec2(center.x, center.y),
                                    TableSpec.RADIUS)
                            }
                        } finally {
                            points.release()
                        }
                    }
                    if (best != null) balls += best
                } finally {
                    contours.forEach { it.release() }
                    hierarchy.release()
                    mask.release()
                }
            }
            return balls
        } finally {
            kernel.release()
            rgb.release()
            rgba.release()
        }
    }
}
