package com.aimsimulator.model

import org.junit.Assert.*
import org.junit.Test

class ShotPredictorTest {
    private val predictor = ShotPredictor()

    @Test fun straightShotCollides() {
        val path = predictor.predict(listOf(
            BallObservation("cue", Vec2(200.0, 250.0), 12.0),
            BallObservation("red", Vec2(400.0, 250.0), 12.0)
        ), Vec2(1.0, 0.0))
        assertEquals("red", path.targetId)
        assertEquals(376.0, path.collisionAt!!.x, 0.001)
        assertTrue(path.segments.any { it.kind == ShotPredictor.Kind.OBJECT })
        assertFalse(path.segments.any { it.kind == ShotPredictor.Kind.CUE_AFTER })
    }

    @Test fun missesAndBanksTwice() {
        val path = predictor.predict(listOf(
            BallObservation("cue", Vec2(200.0, 250.0), 12.0)
        ), Vec2(1.0, 0.3), maxBanks = 2)
        assertNull(path.targetId)
        assertEquals(2, path.bankCount)
        assertEquals(3, path.segments.size)
    }

    @Test fun noCueMeansNoPrediction() {
        val path = predictor.predict(listOf(
            BallObservation("red", Vec2(450.0, 250.0), 12.0)
        ), Vec2(1.0, 0.0))
        assertTrue(path.segments.isEmpty())
    }

    @Test fun halfBallCreatesCueDeflection() {
        val path = predictor.predict(listOf(
            BallObservation("cue", Vec2(200.0, 250.0), 12.0),
            BallObservation("red", Vec2(350.0, 265.0), 12.0)
        ), Vec2(1.0, 0.0))
        assertEquals("red", path.targetId)
        assertTrue(path.segments.any { it.kind == ShotPredictor.Kind.CUE_AFTER })
    }
}
