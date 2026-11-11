package com.aimsimulator.model

import kotlin.math.abs
import kotlin.math.sqrt

/** Lightweight analytical straight-line model. No English, spin or swerve. */
class ShotPredictor {
    enum class Kind { CUE, CUE_AFTER, OBJECT }
    data class Segment(val start: Vec2, val end: Vec2, val kind: Kind)
    data class Prediction(
        val segments: List<Segment>,
        val collisionAt: Vec2? = null,
        val targetId: String? = null,
        val bankCount: Int = 0
    )

    fun predict(
        observations: List<BallObservation>,
        aim: Vec2,
        maxBanks: Int = 2
    ): Prediction {
        val cue = observations.firstOrNull { it.id == "cue" } ?: return Prediction(emptyList())
        val direction = aim.normalized()
        if (direction.length() < 1e-8) return Prediction(emptyList())
        val objects = observations.filter { it.id != "cue" }
        val traces = mutableListOf<Segment>()
        var origin = cue.position
        var ray = direction
        var banks = 0

        repeat(maxBanks + 1) {
            val wall = nearestWall(origin, ray, cue.radius)
                ?: return Prediction(traces, bankCount = banks)
            var closest: BallObservation? = null
            var distance = wall.distance
            for (ball in objects) {
                val t = hitDistance(origin, ray, ball.position, cue.radius + ball.radius)
                if (t != null && t < distance) {
                    closest = ball
                    distance = t
                }
            }
            val impact = origin + ray * distance
            traces += Segment(origin, impact, Kind.CUE)
            if (closest != null) {
                val target = closest
                val normal = (target.position - impact).normalized()
                // Equal masses: target moves in the normal direction and cue retains tangential component.
                val normalVelocity = ray.dot(normal).coerceAtLeast(0.0)
                if (normalVelocity > 1e-5) {
                    val objectStart = target.position
                    val objectEnd = nearestWall(objectStart, normal, target.radius)
                        ?.let { objectStart + normal * it.distance }
                        ?: (objectStart + normal * 150.0)
                    traces += Segment(objectStart, objectEnd, Kind.OBJECT)
                }
                val tangent = ray - normal * normalVelocity
                if (tangent.length() > 0.04) {
                    val outgoing = tangent.normalized()
                    val cueEnd = nearestWall(impact, outgoing, cue.radius)
                        ?.let { impact + outgoing * it.distance }
                        ?: (impact + outgoing * 140.0)
                    traces += Segment(impact, cueEnd, Kind.CUE_AFTER)
                }
                return Prediction(traces, impact, target.id, banks)
            }
            if (banks == maxBanks) return Prediction(traces, bankCount = banks)
            // If both axes are hit at the same time, reflect both.
            ray = Vec2(if (wall.reflectX) -ray.x else ray.x,
                       if (wall.reflectY) -ray.y else ray.y)
            origin = impact + ray * 0.001
            banks++
        }
        return Prediction(traces, bankCount = banks)
    }

    private fun hitDistance(o: Vec2, d: Vec2, center: Vec2, radius: Double): Double? {
        val delta = o - center
        val b = delta.dot(d)
        val c = delta.dot(delta) - radius * radius
        if (c <= 0.0) return null // overlapping at origin; avoid self-collision loop
        val discriminant = b * b - c
        if (discriminant < 0.0) return null
        val distance = -b - sqrt(discriminant)
        return distance.takeIf { it > 0.001 }
    }

    private data class WallHit(val distance: Double, val reflectX: Boolean, val reflectY: Boolean)

    private fun nearestWall(o: Vec2, d: Vec2, radius: Double): WallHit? {
        val left = TableSpec.LEFT + radius
        val right = TableSpec.RIGHT - radius
        val top = TableSpec.TOP + radius
        val bottom = TableSpec.BOTTOM - radius
        val dx = when {
            d.x > 1e-9 -> (right - o.x) / d.x
            d.x < -1e-9 -> (left - o.x) / d.x
            else -> Double.POSITIVE_INFINITY
        }
        val dy = when {
            d.y > 1e-9 -> (bottom - o.y) / d.y
            d.y < -1e-9 -> (top - o.y) / d.y
            else -> Double.POSITIVE_INFINITY
        }
        val distance = minOf(dx, dy)
        if (!distance.isFinite() || distance <= 0.0) return null
        return WallHit(distance, abs(dx - distance) < 0.01, abs(dy - distance) < 0.01)
    }
}
