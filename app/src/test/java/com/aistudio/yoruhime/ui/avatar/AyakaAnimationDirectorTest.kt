package com.aistudio.yoruhime.ui.avatar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AyakaAnimationDirectorTest {

    private val director = AyakaAnimationDirector()

    @Test
    fun rikoStyleComplimentCombinesBlushAndLookAway() {
        val result = director.decide(
            AvatarContext(
                persona = AyakaPersona.RIKO_TSUNDERE,
                cue = AvatarCue.COMPLIMENT
            )
        )

        assertEquals("embarrassed_blush", result.commandFor(AvatarLayer.FACE)?.clip)
        assertEquals("glance_away", result.commandFor(AvatarLayer.GAZE)?.clip)
        assertEquals("bashful_look_away", result.commandFor(AvatarLayer.HEAD)?.clip)
        assertEquals(AyakaSpeechDelivery.PLAYFUL, result.speechDelivery)
    }

    @Test
    fun mommyTsundereComplimentUsesWarmOpenBodyLanguage() {
        val result = director.decide(
            AvatarContext(
                persona = AyakaPersona.MOMMY_TSUNDERE,
                cue = AvatarCue.COMPLIMENT
            )
        )

        assertEquals("warm_smile", result.commandFor(AvatarLayer.FACE)?.clip)
        assertEquals("relaxed_open_posture", result.commandFor(AvatarLayer.UPPER_BODY)?.clip)
        assertEquals(AyakaSpeechDelivery.WARM, result.speechDelivery)
    }

    @Test
    fun distressOverridesPlayfulPersonaAndUsesReassuringPerformance() {
        val result = director.decide(
            AvatarContext(
                persona = AyakaPersona.RIKO_TSUNDERE,
                emotion = AyakaEmotion.SAD,
                cue = AvatarCue.USER_DISTRESSED
            )
        )

        assertEquals("concerned_soft", result.commandFor(AvatarLayer.FACE)?.clip)
        assertEquals("lean_in_reassuring", result.commandFor(AvatarLayer.UPPER_BODY)?.clip)
        assertEquals("soft_direct_gaze", result.commandFor(AvatarLayer.GAZE)?.clip)
        assertEquals(AyakaSpeechDelivery.REASSURING, result.speechDelivery)
    }

    @Test
    fun quietModeSuppressesUnneededGesturesButKeepsCare() {
        val idle = director.decide(
            AvatarContext(persona = AyakaPersona.QUIET, cue = AvatarCue.GREETING)
        )
        val caring = director.decide(
            AvatarContext(persona = AyakaPersona.QUIET, cue = AvatarCue.USER_DISTRESSED)
        )

        assertNull(idle.commandFor(AvatarLayer.GESTURE))
        assertNotNull(caring.commandFor(AvatarLayer.UPPER_BODY))
        assertEquals(AyakaSpeechDelivery.QUIET, caring.speechDelivery)
    }

    @Test
    fun longIdleUsesSubtleBehaviourRatherThanConstantBigGestures() {
        val result = director.decide(
            AvatarContext(
                persona = AyakaPersona.MOMMY_TSUNDERE,
                idleDurationMs = AyakaAnimationDirector.IDLE_RESTLESS_AFTER_MS
            )
        )

        assertEquals("idle_room_glance", result.commandFor(AvatarLayer.GAZE)?.clip)
        assertEquals("subtle_idle_tilt", result.commandFor(AvatarLayer.HEAD)?.clip)
        assertNull(result.commandFor(AvatarLayer.GESTURE))
        assertTrue(result.commands.size <= AvatarLayer.values().size)
    }
}
