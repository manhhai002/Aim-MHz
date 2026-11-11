package com.aimsimulator.model

import kotlin.math.hypot

/** All coordinates in a stable 1000 x 500 simulation coordinate system. */
data class Vec2(val x: Double, val y: Double) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(scale: Double) = Vec2(x * scale, y * scale)
    operator fun div(scale: Double) = Vec2(x / scale, y / scale)
    fun dot(other: Vec2): Double = x * other.x + y * other.y
    fun length(): Double = hypot(x, y)
    fun normalized(): Vec2 = if (length() < 1e-9) ZERO else this / length()

    companion object { val ZERO = Vec2(0.0, 0.0) }
}

object TableSpec {
    const val WIDTH = 1000.0
    const val HEIGHT = 500.0
    const val LEFT = 58.0
    const val RIGHT = 942.0
    const val TOP = 58.0
    const val BOTTOM = 442.0
    const val RADIUS = 12.0
    const val POCKET_RADIUS = 21.0
    val POCKETS = listOf(
        Vec2(LEFT, TOP), Vec2(500.0, TOP), Vec2(RIGHT, TOP),
        Vec2(LEFT, BOTTOM), Vec2(500.0, BOTTOM), Vec2(RIGHT, BOTTOM)
    )
}

data class BallObservation(val id: String, val position: Vec2, val radius: Double)

data class SimBall(
    val id: String,
    var position: Vec2,
    var velocity: Vec2 = Vec2.ZERO,
    var pocketed: Boolean = false
)
