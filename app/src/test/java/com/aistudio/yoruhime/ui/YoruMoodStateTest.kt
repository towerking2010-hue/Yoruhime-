package com.aistudio.yoruhime.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YoruMoodStateTest {

    @Test
    fun unknownEmotionLeavesMoodUnchanged() {
        val initial = YoruMoodState()
        assertEquals(initial, initial.reactTo("surprised"))
    }

    @Test
    fun repeatedReactionsKeepEveryDimensionWithinBounds() {
        var mood = YoruMoodState()
        repeat(30) {
            mood = mood.reactTo("angry").reactTo("sad").reactTo("happy").reactTo("blush")
        }

        listOf(mood.warmth, mood.energy, mood.stress, mood.affection, mood.confidence)
            .forEach { assertTrue(it in 0f..1f) }
    }

    @Test
    fun decayMovesMoodTowardCalmBaseline() {
        var mood = YoruMoodState().reactTo("angry").reactTo("sad")
        repeat(100) { mood = mood.decay() }

        assertEquals(0.35f, mood.warmth, 0.001f)
        assertEquals(0.55f, mood.energy, 0.001f)
        assertEquals(0.15f, mood.stress, 0.001f)
        assertEquals(0.30f, mood.affection, 0.001f)
        assertEquals(0.50f, mood.confidence, 0.001f)
    }

    @Test
    fun promptContextIncludesAllMoodDimensionsWithoutExposingRawStateObject() {
        val context = YoruMoodState(
            warmth = 0.75f,
            energy = 0.50f,
            stress = 0.25f,
            affection = 0.60f,
            confidence = 0.90f
        ).toPromptContext()

        assertTrue(context.contains("Warmth 75%"))
        assertTrue(context.contains("energy 50%"))
        assertTrue(context.contains("stress 25%"))
        assertTrue(context.contains("affection 60%"))
        assertTrue(context.contains("confidence 89%") || context.contains("confidence 90%"))
    }

    @Test
    fun expressionMapperOnlyEmitsExpressionsPresentInTheModel() {
        val supported = setOf("angry", "blush", "happy", "smile", "sad", "normal")
        var mood = YoruMoodState()
        repeat(50) {
            mood = mood.reactTo(listOf("happy", "smile", "blush", "sad", "angry").random())
            assertTrue(mood.toLive2DExpression() in supported)
        }
    }
}
