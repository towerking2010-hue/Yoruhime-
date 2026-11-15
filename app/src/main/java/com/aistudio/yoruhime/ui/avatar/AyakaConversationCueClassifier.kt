package com.aistudio.yoruhime.ui.avatar

/**
 * Converts the user's latest message into a small, deterministic performance cue.
 * This never infers a diagnosis; it only selects presentation tone for Ayaka.
 */
object AyakaConversationCueClassifier {

    fun classify(userText: String): AvatarCue {
        val text = userText.trim().lowercase()
        if (text.isBlank()) return AvatarCue.NONE

        return when {
            containsAny(text, "overwhelmed", "burnt out", "burned out", "exhausted",
                "panic", "panicking", "anxious", "anxiety", "depressed", "heartbroken",
                "having a rough day", "feeling down", "i feel sad", "i am sad", "i'm sad",
                "i feel awful", "i feel terrible") -> AvatarCue.USER_DISTRESSED

            containsAny(text, "you're pretty", "you are pretty", "beautiful", "gorgeous",
                "cute", "i love you", "love you", "miss you", "you look lovely",
                "you look good", "pretty today") -> AvatarCue.COMPLIMENT

            containsAny(text, "i finished", "i completed", "i passed", "i got accepted",
                "i won", "i achieved", "finally did it", "i did it", "we did it") ->
                AvatarCue.USER_ACHIEVEMENT

            text.matches(Regex("^(yo|sup|hey|hiya|hello|hi|good morning|good afternoon|good evening)[!. ]*$")) ->
                AvatarCue.GREETING

            containsAny(text, "challenge you", "prove it", "bet you can't", "bet you cannot",
                "baka", "don't tease me", "stop teasing", "you're wrong") ->
                AvatarCue.PLAYFUL_CHALLENGE

            text.contains("?") || text.startsWith("what ") || text.startsWith("why ") ||
                text.startsWith("how ") || text.startsWith("when ") || text.startsWith("where ") ||
                text.startsWith("who ") || text.startsWith("can you ") || text.startsWith("could you ") ->
                AvatarCue.QUESTION

            else -> AvatarCue.NONE
        }
    }

    fun personaFor(cue: AvatarCue): AyakaPersona =
        if (cue == AvatarCue.COMPLIMENT || cue == AvatarCue.PLAYFUL_CHALLENGE) {
            AyakaPersona.RIKO_TSUNDERE
        } else {
            AyakaPersona.MOMMY_TSUNDERE
        }

    fun emotionFor(expression: String): AyakaEmotion = when (expression.lowercase()) {
        "happy" -> AyakaEmotion.HAPPY
        "blush" -> AyakaEmotion.EMBARRASSED
        "sad" -> AyakaEmotion.SAD
        "angry" -> AyakaEmotion.FRUSTRATED
        "smile" -> AyakaEmotion.AFFECTIONATE
        else -> AyakaEmotion.NEUTRAL
    }

    private fun containsAny(text: String, vararg phrases: String): Boolean =
        phrases.any(text::contains)
}
