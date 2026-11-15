package com.aistudio.yoruhime.ui.avatar

/**
 * Turns conversational and emotional context into a small, composable
 * performance. It deliberately does not call a renderer or an AI service:
 * output is deterministic, bounded and safe to test.
 *
 * A future VRM renderer can blend these layers independently. The existing
 * Live2D renderer can initially map only the FACE and supported motion clips.
 */
class AyakaAnimationDirector {

    fun decide(context: AvatarContext): AvatarPerformance {
        val commands = linkedMapOf<AvatarLayer, AvatarLayerCommand>()

        commands[AvatarLayer.BASE] = command(
            AvatarLayer.BASE,
            "idle_breathing",
            priority = 0
        )

        val face = faceFor(context)
        commands[AvatarLayer.FACE] = command(
            AvatarLayer.FACE,
            face,
            priority = if (context.cue == AvatarCue.USER_DISTRESSED) 100 else 50,
            durationMs = if (face == "neutral") 0L else 1_400L
        )

        val gaze = gazeFor(context)
        if (gaze != null) {
            commands[AvatarLayer.GAZE] = command(
                AvatarLayer.GAZE,
                gaze,
                priority = 40,
                durationMs = 1_000L
            )
        }

        val head = headFor(context)
        if (head != null) {
            commands[AvatarLayer.HEAD] = command(
                AvatarLayer.HEAD,
                head,
                priority = 35,
                durationMs = 1_100L
            )
        }

        val body = bodyFor(context)
        if (body != null) {
            commands[AvatarLayer.UPPER_BODY] = command(
                AvatarLayer.UPPER_BODY,
                body,
                priority = if (context.cue == AvatarCue.USER_DISTRESSED) 90 else 30,
                durationMs = 1_500L
            )
        }

        val gesture = gestureFor(context)
        if (gesture != null) {
            commands[AvatarLayer.GESTURE] = command(
                AvatarLayer.GESTURE,
                gesture,
                priority = 45,
                durationMs = 1_200L
            )
        }

        return AvatarPerformance(
            commands = commands.values.toList(),
            speechDelivery = deliveryFor(context)
        )
    }

    private fun faceFor(context: AvatarContext): String = when {
        context.cue == AvatarCue.USER_DISTRESSED -> "concerned_soft"
        context.cue == AvatarCue.USER_ACHIEVEMENT -> "proud_smile"
        context.cue == AvatarCue.COMPLIMENT &&
            context.persona == AyakaPersona.RIKO_TSUNDERE -> "embarrassed_blush"
        context.cue == AvatarCue.COMPLIMENT &&
            context.persona == AyakaPersona.MOMMY_TSUNDERE -> "warm_smile"
        context.emotion == AyakaEmotion.EMBARRASSED -> "embarrassed_blush"
        context.emotion == AyakaEmotion.CONCERNED || context.emotion == AyakaEmotion.SAD ->
            "concerned_soft"
        context.emotion == AyakaEmotion.SURPRISED -> "surprised"
        context.emotion == AyakaEmotion.CURIOUS -> "curious"
        context.emotion == AyakaEmotion.FRUSTRATED -> "determined"
        context.emotion == AyakaEmotion.CONFIDENT -> "confident_smirk"
        context.emotion == AyakaEmotion.AFFECTIONATE -> "warm_smile"
        context.emotion == AyakaEmotion.HAPPY -> "happy_smile"
        context.persona == AyakaPersona.RIKO_TSUNDERE &&
            context.cue == AvatarCue.PLAYFUL_CHALLENGE -> "confident_smirk"
        context.persona == AyakaPersona.MOMMY_TSUNDERE -> "gentle_smile"
        context.persona == AyakaPersona.QUIET -> "relaxed_neutral"
        else -> "neutral"
    }

    private fun gazeFor(context: AvatarContext): String? = when {
        context.cue == AvatarCue.COMPLIMENT &&
            context.persona == AyakaPersona.RIKO_TSUNDERE -> "glance_away"
        context.isListening -> "attentive_gaze"
        context.cue == AvatarCue.USER_DISTRESSED -> "soft_direct_gaze"
        context.cue == AvatarCue.QUESTION -> "curious_gaze"
        context.idleDurationMs >= IDLE_RESTLESS_AFTER_MS -> "idle_room_glance"
        else -> null
    }

    private fun headFor(context: AvatarContext): String? = when {
        context.cue == AvatarCue.QUESTION -> "curious_head_tilt"
        context.cue == AvatarCue.GREETING -> "friendly_nod"
        context.cue == AvatarCue.USER_DISTRESSED -> "gentle_head_tilt"
        context.cue == AvatarCue.COMPLIMENT &&
            context.persona == AyakaPersona.RIKO_TSUNDERE -> "bashful_look_away"
        context.idleDurationMs >= IDLE_RESTLESS_AFTER_MS -> "subtle_idle_tilt"
        else -> null
    }

    private fun bodyFor(context: AvatarContext): String? = when {
        context.cue == AvatarCue.USER_DISTRESSED -> "lean_in_reassuring"
        context.cue == AvatarCue.PLAYFUL_CHALLENGE &&
            context.persona == AyakaPersona.RIKO_TSUNDERE -> "arms_crossed_confident"
        context.cue == AvatarCue.COMPLIMENT &&
            context.persona == AyakaPersona.MOMMY_TSUNDERE -> "relaxed_open_posture"
        else -> null
    }

    private fun gestureFor(context: AvatarContext): String? {
        if (context.persona == AyakaPersona.QUIET &&
            context.cue != AvatarCue.USER_DISTRESSED) return null

        return when (context.cue) {
            AvatarCue.GREETING -> "small_wave"
            AvatarCue.USER_ACHIEVEMENT -> "gentle_celebration"
            AvatarCue.PLAYFUL_CHALLENGE -> if (
                context.persona == AyakaPersona.RIKO_TSUNDERE
            ) "playful_point" else null
            else -> null
        }
    }

    private fun deliveryFor(context: AvatarContext): AyakaSpeechDelivery = when {
        context.persona == AyakaPersona.QUIET -> AyakaSpeechDelivery.QUIET
        context.cue == AvatarCue.USER_DISTRESSED ||
            context.emotion == AyakaEmotion.SAD ||
            context.emotion == AyakaEmotion.CONCERNED -> AyakaSpeechDelivery.REASSURING
        context.emotion == AyakaEmotion.FRUSTRATED -> AyakaSpeechDelivery.FIRM
        context.cue == AvatarCue.PLAYFUL_CHALLENGE ||
            context.persona == AyakaPersona.RIKO_TSUNDERE -> AyakaSpeechDelivery.PLAYFUL
        context.persona == AyakaPersona.MOMMY_TSUNDERE ||
            context.emotion == AyakaEmotion.AFFECTIONATE -> AyakaSpeechDelivery.WARM
        else -> AyakaSpeechDelivery.NATURAL
    }

    private fun command(
        layer: AvatarLayer,
        clip: String,
        priority: Int,
        durationMs: Long = 0L
    ) = AvatarLayerCommand(
        layer = layer,
        clip = clip,
        priority = priority,
        durationMs = durationMs
    )

    companion object {
        const val IDLE_RESTLESS_AFTER_MS = 45_000L
    }
}
