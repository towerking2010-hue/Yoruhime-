package com.aistudio.yoruhime.ui

import kotlin.math.abs

/**
 * Lightweight, local emotional state for YoruHime.
 *
 * The internal dimensions are richer than the six expressions currently shipped
 * with the Tsubaki Live2D model. The expression mapper deliberately emits only
 * supported model expressions; no model assets or parameter IDs are invented.
 */
data class YoruMoodState(
    val warmth: Float = 0.35f,
    val energy: Float = 0.55f,
    val stress: Float = 0.15f,
    val affection: Float = 0.30f,
    val confidence: Float = 0.50f
) {
    fun reactTo(tag: String): YoruMoodState {
        val next = when (tag.lowercase()) {
            "happy" -> copy(warmth = warmth + 0.18f, energy = energy + 0.14f,
                stress = stress - 0.10f, affection = affection + 0.08f,
                confidence = confidence + 0.08f)
            "smile" -> copy(warmth = warmth + 0.13f, energy = energy + 0.04f,
                stress = stress - 0.06f, affection = affection + 0.06f)
            "blush" -> copy(warmth = warmth + 0.08f, affection = affection + 0.16f,
                confidence = confidence - 0.04f)
            "sad" -> copy(warmth = warmth - 0.08f, energy = energy - 0.14f,
                stress = stress + 0.10f, confidence = confidence - 0.08f)
            "angry" -> copy(energy = energy + 0.10f, stress = stress + 0.20f,
                confidence = confidence + 0.05f, warmth = warmth - 0.10f)
            else -> this
        }
        return next.clamped()
    }

    /** Slowly returns the mood toward a calm baseline while retaining some bond warmth. */
    fun decay(): YoruMoodState = copy(
        warmth = approach(warmth, 0.35f, 0.025f),
        energy = approach(energy, 0.55f, 0.035f),
        stress = approach(stress, 0.15f, 0.04f),
        affection = approach(affection, 0.30f, 0.008f),
        confidence = approach(confidence, 0.50f, 0.02f)
    ).clamped()

    /**
     * Maps richer mood dimensions to expressions that actually exist in the
     * current Live2D expression set.
     */
    fun toLive2DExpression(): String = when {
        stress > 0.72f && energy > 0.58f -> "angry"
        warmth > 0.68f && affection > 0.58f -> "blush"
        warmth > 0.66f && energy > 0.58f -> "happy"
        energy < 0.28f || stress > 0.62f -> "sad"
        warmth > 0.52f -> "smile"
        else -> "normal"
    }

    /** A compact, non-sensitive state summary used to keep cloud replies emotionally consistent. */
    fun toPromptContext(): String =
        "Warmth ${(warmth * 100).toInt()}%, energy ${(energy * 100).toInt()}%, " +
            "stress ${(stress * 100).toInt()}%, affection ${(affection * 100).toInt()}%, " +
            "confidence ${(confidence * 100).toInt()}%. Treat this as fictional character-state guidance, " +
            "not evidence of consciousness; let it gently shape tone without overriding the user's request."

    private fun clamped() = copy(
        warmth = warmth.coerceIn(0f, 1f),
        energy = energy.coerceIn(0f, 1f),
        stress = stress.coerceIn(0f, 1f),
        affection = affection.coerceIn(0f, 1f),
        confidence = confidence.coerceIn(0f, 1f)
    )

    private fun approach(value: Float, target: Float, amount: Float): Float =
        if (abs(value - target) <= amount) target
        else value + if (value < target) amount else -amount
}
