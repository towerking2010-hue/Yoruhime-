package com.aistudio.yoruhime.ui.avatar

import org.junit.Assert.assertEquals
import org.junit.Test

class AyakaLive2DPerformanceAdapterTest {

    private val director = AyakaAnimationDirector()

    @Test
    fun rikoComplimentMapsToRealBlushAndLookAwayControls() {
        val performance = director.decide(
            AvatarContext(
                persona = AyakaPersona.RIKO_TSUNDERE,
                cue = AvatarCue.COMPLIMENT
            )
        )

        val mapped = AyakaLive2DPerformanceAdapter.map(performance)

        assertEquals("blush", mapped.expression)
        assertEquals(0.62f, mapped.gazeX)
        assertEquals(7.0f, mapped.headTiltDegrees)
    }

    @Test
    fun distressMapsToConcernedFaceAndReassuringLean() {
        val performance = director.decide(
            AvatarContext(
                persona = AyakaPersona.RIKO_TSUNDERE,
                emotion = AyakaEmotion.SAD,
                cue = AvatarCue.USER_DISTRESSED
            )
        )

        val mapped = AyakaLive2DPerformanceAdapter.map(performance)

        assertEquals("sad", mapped.expression)
        assertEquals(0f, mapped.gazeX)
        assertEquals(2.0f, mapped.bodyLeanDegrees)
    }
}
