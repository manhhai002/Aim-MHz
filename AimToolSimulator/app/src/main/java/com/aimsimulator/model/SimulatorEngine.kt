package com.aimsimulator.model

import kotlin.math.sqrt
import kotlin.random.Random

/** Fixed-radius ball simulation with simple elastic collisions and rolling drag. */
class SimulatorEngine {
    val balls: MutableList<SimBall> = mutableListOf()
    var aim: Vec2 = Vec2(1.0, -0.14)
        private set

    init { reset() }

    fun reset() {
        balls.clear()
        val placements = listOf(
            "cue" to Vec2(220.0, 290.0),
            "red" to Vec2(480.0, 250.0),
            "yellow" to Vec2(635.0, 171.0),
            "blue" to Vec2(755.0, 305.0),
            "purple" to Vec2(833.0, 222.0),
            "orange" to Vec2(580.0, 353.0),
            "pink" to Vec2(687.0, 390.0),
            "dark" to Vec2(447.0, 386.0)
        )
        balls.addAll(placements.map { (id, pos) -> SimBall(id, pos) })
        aim = Vec2(1.0, -0.14).normalized()
    }

    fun randomize() {
        reset()
        val otherBalls = balls.drop(1)
        for (ball in otherBalls) {
            for (attempt in 0 until 600) {
                val p = Vec2(Random.nextDouble(300.0, 899.0), Random.nextDouble(95.0, 406.0))
                if (balls.none { it !== ball && !it.pocketed && (p - it.position).length() < 42.0 }) {
                    ball.position = p
                    break
                }
            }
        }
    }

    fun changeAim(toward: Vec2) {
        val cue = balls.firstOrNull { it.id == "cue" && !it.pocketed } ?: return
        val dir = (toward - cue.position).normalized()
        if (dir.length() > 1e-5) aim = dir
    }

    fun shoot(): Boolean {
        if (hasMotion()) return false
        val cue = balls.firstOrNull { it.id == "cue" && !it.pocketed } ?: return false
        cue.velocity = aim * 640.0
        return true
    }

    fun hasMotion(): Boolean = balls.any { !it.pocketed && it.velocity.length() > 1.0 }

    fun step(dtInput: Double) {
        // Smaller fixed substeps help prevent tunneling at mobile display frame rates.
        val dt = dtInput.coerceIn(0.0, 0.04)
        val substeps = 4
        repeat(substeps) { integrate(dt / substeps) }
        if (!hasMotion()) {
            for (b in balls) b.velocity = Vec2.ZERO
            val cue = balls.first { it.id == "cue" }
            if (cue.pocketed) {
                cue.pocketed = false
                cue.position = Vec2(210.0, 280.0)
            }
        }
    }

    private fun integrate(dt: Double) {
        val rollingDeceleration = 190.0
        val r = TableSpec.RADIUS
        for (b in balls) {
            if (b.pocketed) continue
            b.position += b.velocity * dt
            val speed = b.velocity.length()
            if (speed > 0.0) {
                val newSpeed = (speed - rollingDeceleration * dt).coerceAtLeast(0.0)
                b.velocity *= newSpeed / speed
            }
            if (b.velocity.length() < 1.0) b.velocity = Vec2.ZERO
            if (TableSpec.POCKETS.any { (it - b.position).length() < TableSpec.POCKET_RADIUS }) {
                b.pocketed = true
                b.velocity = Vec2.ZERO
                continue
            }
            var (x, y) = b.position
            var (vx, vy) = b.velocity
            if (x < TableSpec.LEFT + r) { x = TableSpec.LEFT + r; vx = kotlin.math.abs(vx) * 0.9 }
            if (x > TableSpec.RIGHT - r) { x = TableSpec.RIGHT - r; vx = -kotlin.math.abs(vx) * 0.9 }
            if (y < TableSpec.TOP + r) { y = TableSpec.TOP + r; vy = kotlin.math.abs(vy) * 0.9 }
            if (y > TableSpec.BOTTOM - r) { y = TableSpec.BOTTOM - r; vy = -kotlin.math.abs(vy) * 0.9 }
            b.position = Vec2(x, y)
            b.velocity = Vec2(vx, vy)
        }
        for (i in 0 until balls.lastIndex) for (j in i + 1 until balls.size) {
            val a = balls[i]; val b = balls[j]
            if (a.pocketed || b.pocketed) continue
            val separation = b.position - a.position
            val distSq = separation.dot(separation)
            val minDist = 2 * r
            if (distSq >= minDist * minDist) continue
            val dist = sqrt(distSq)
            val normal = if (dist < 1e-8) Vec2(1.0, 0.0) else separation / dist
            val overlap = minDist - dist
            a.position -= normal * (overlap / 2)
            b.position += normal * (overlap / 2)
            val approaching = (b.velocity - a.velocity).dot(normal)
            if (approaching < 0.0) {
                val impulse = -(1.0 + 0.94) * approaching / 2.0
                a.velocity -= normal * impulse
                b.velocity += normal * impulse
            }
        }
    }
}
