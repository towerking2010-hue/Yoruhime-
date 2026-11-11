package com.aistudio.yoruhime.ui

import android.content.Context

/**
 * Persists Ayaka's lightweight relationship/mood state locally between app launches.
 * This is presentation and conversation context, not a claim of machine consciousness.
 */
class YoruMoodStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("yoruhime_mood_state", Context.MODE_PRIVATE)

    fun load(): YoruMoodState = YoruMoodState(
        warmth = preferences.getFloat(KEY_WARMTH, 0.35f),
        energy = preferences.getFloat(KEY_ENERGY, 0.55f),
        stress = preferences.getFloat(KEY_STRESS, 0.15f),
        affection = preferences.getFloat(KEY_AFFECTION, 0.30f),
        confidence = preferences.getFloat(KEY_CONFIDENCE, 0.50f)
    )

    fun save(state: YoruMoodState) {
        preferences.edit()
            .putFloat(KEY_WARMTH, state.warmth)
            .putFloat(KEY_ENERGY, state.energy)
            .putFloat(KEY_STRESS, state.stress)
            .putFloat(KEY_AFFECTION, state.affection)
            .putFloat(KEY_CONFIDENCE, state.confidence)
            .apply()
    }

    private companion object {
        const val KEY_WARMTH = "warmth"
        const val KEY_ENERGY = "energy"
        const val KEY_STRESS = "stress"
        const val KEY_AFFECTION = "affection"
        const val KEY_CONFIDENCE = "confidence"
    }
}
