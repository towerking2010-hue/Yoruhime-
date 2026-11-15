package com.aistudio.yoruhime.ui.avatar

/**
 * Maps renderer-neutral performance cues onto controls that the bundled Tsubaki
 * Live2D rig actually supports. Unsupported arm/hand gestures are deliberately
 * not faked: the current model only ships with an idle motion.
 */
data class AyakaLive2DPerformance(
    val expression: String,
    val gazeX: Float,
    val gazeY: Float,
    val headTiltDegrees: Float,
    val bodyLeanDegrees: Float
)

object AyakaLive2DPerformanceAdapter {

    fun map(performance: AvatarPerformance): AyakaLive2DPerformance {
        val face = performance.commandFor(AvatarLayer.FACE)?.clip.orEmpty()
        val gaze = performance.commandFor(AvatarLayer.GAZE)?.clip.orEmpty()
        val head = performance.commandFor(AvatarLayer.HEAD)?.clip.orEmpty()
        val body = performance.commandFor(AvatarLayer.UPPER_BODY)?.clip.orEmpty()

        return AyakaLive2DPerformance(
            expression = expressionFor(face),
            gazeX = when (gaze) {
                "glance_away" -> 0.62f
                "curious_gaze" -> -0.22f
                "idle_room_glance" -> -0.38f
                else -> 0f
            },
            gazeY = when (gaze) {
                "glance_away" -> -0.12f
                "curious_gaze" -> 0.16f
                "idle_room_glance" -> 0.10f
                else -> 0f
            },
            headTiltDegrees = when (head) {
                "friendly_nod" -> -2.0f
                "curious_head_tilt" -> -5.0f
                "gentle_head_tilt" -> 4.0f
                "bashful_look_away" -> 7.0f
                "subtle_idle_tilt" -> 2.0f
                else -> 0f
            },
            bodyLeanDegrees = when (body) {
                "lean_in_reassuring" -> 2.0f
                "arms_crossed_confident" -> -1.5f
                else -> 0f
            }
        )
    }

    private fun expressionFor(clip: String): String = when (clip) {
        "embarrassed_blush" -> "blush"
        "warm_smile", "gentle_smile" -> "smile"
        "concerned_soft" -> "sad"
        "proud_smile", "happy_smile" -> "happy"
        "confident_smirk" -> "smile"
        "determined" -> "angry"
        else -> "normal"
    }
}
