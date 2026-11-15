package com.aistudio.yoruhime.ui.avatar

/**
 * Renderer-neutral vocabulary for Ayaka's expressive behaviour.
 *
 * The director chooses intent; a renderer (Live2D or VRM) maps these stable
 * semantic clips to model-specific parameters, expressions and animations.
 */
enum class AyakaPersona {
    MOMMY_TSUNDERE,
    RIKO_TSUNDERE,
    CARING,
    QUIET
}

enum class AyakaEmotion {
    NEUTRAL,
    HAPPY,
    AFFECTIONATE,
    EMBARRASSED,
    CURIOUS,
    CONCERNED,
    SAD,
    FRUSTRATED,
    CONFIDENT,
    SURPRISED
}

enum class AvatarCue {
    NONE,
    GREETING,
    COMPLIMENT,
    QUESTION,
    USER_DISTRESSED,
    USER_ACHIEVEMENT,
    PLAYFUL_CHALLENGE
}

enum class AvatarLayer {
    BASE,
    FACE,
    GAZE,
    HEAD,
    UPPER_BODY,
    GESTURE
}

enum class AyakaSpeechDelivery {
    NATURAL,
    WARM,
    PLAYFUL,
    REASSURING,
    FIRM,
    QUIET
}

/**
 * A semantic animation request. clip is a stable intent key, not a renderer
 * implementation detail. weight is clamped by the director to [0, 1].
 */
data class AvatarLayerCommand(
    val layer: AvatarLayer,
    val clip: String,
    val weight: Float = 1f,
    val priority: Int = 0,
    val durationMs: Long = 0L,
    val blendInMs: Long = 180L,
    val blendOutMs: Long = 220L,
    val interruptible: Boolean = true
)

data class AvatarContext(
    val persona: AyakaPersona = AyakaPersona.MOMMY_TSUNDERE,
    val emotion: AyakaEmotion = AyakaEmotion.NEUTRAL,
    val cue: AvatarCue = AvatarCue.NONE,
    val isSpeaking: Boolean = false,
    val isListening: Boolean = false,
    val idleDurationMs: Long = 0L
)

data class AvatarPerformance(
    val commands: List<AvatarLayerCommand>,
    val speechDelivery: AyakaSpeechDelivery
) {
    init {
        require(commands.map { it.layer }.distinct().size == commands.size) {
            "A performance may contain only one active command per animation layer"
        }
    }

    fun commandFor(layer: AvatarLayer): AvatarLayerCommand? =
        commands.firstOrNull { it.layer == layer }
}
