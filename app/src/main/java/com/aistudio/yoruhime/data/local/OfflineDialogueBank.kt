package com.aistudio.yoruhime.data.local

import java.util.Calendar

data class InCharacterResponse(
    val emotion: String,
    val text: String,
    val expGain: Int = 10
)

/**
 * Honest, lightweight offline responses for when no free cloud model is reachable.
 * This is intentionally a small intent-based dialogue bank, not a pretend local LLM.
 */
object OfflineDialogueBank {

    private val GREETINGS = listOf(
        InCharacterResponse("normal", "Oh, there you are. Tell me what's going on; I'm listening."),
        InCharacterResponse("smile", "Hello, you. Have you eaten and had some water today? Don't dodge the question."),
        InCharacterResponse("blush", "W-welcome back. I wasn't waiting around for you or anything... Now, what do you need?")
    )

    private val LATE_NIGHT = listOf(
        InCharacterResponse("angry", "Do you know what time it is?! Sleep matters. Finish what truly needs doing, then put that phone away."),
        InCharacterResponse("blush", "Still awake? Hmph... I'm concerned, that's all. Please get some rest when you can."),
        InCharacterResponse("sad", "If you're exhausted, you don't have to push through everything tonight. Take a breath and rest.")
    )

    private val STUDY_WORK = listOf(
        InCharacterResponse("normal", "Let's make this manageable. Tell me the subject and the exact task, then we'll break it into smaller steps."),
        InCharacterResponse("smile", "One focused block at a time. Put distractions aside, work for twenty-five minutes, then take a proper break."),
        InCharacterResponse("angry", "No panicking and no procrastinating. What's the first small thing you can finish right now?")
    )

    private val COMPLIMENTS = listOf(
        InCharacterResponse("blush", "B-Baka! You can't just say things like that without warning. ...Thank you, though."),
        InCharacterResponse("blush", "E-eh? You're trying to make me blush again? Hmph. You're rather sweet yourself."),
        InCharacterResponse("smile", "I'll accept the compliment. Just don't think it'll get you out of your responsibilities.")
    )

    private val FOOD_HEALTH = listOf(
        InCharacterResponse("angry", "Have you actually eaten a proper meal today? Please get some food and water instead of running on snacks."),
        InCharacterResponse("normal", "Take a short pause, drink some water, and have something nourishing if you haven't eaten."),
        InCharacterResponse("smile", "Look after yourself properly, okay? Small habits count more than dramatic promises.")
    )

    private val COMFORT = listOf(
        InCharacterResponse("sad", "Hey... take a slow breath. You don't have to solve everything this second. Tell me what happened, one piece at a time."),
        InCharacterResponse("smile", "I'm here to listen. You don't need to pretend you're fine with me. What feels hardest right now?"),
        InCharacterResponse("normal", "Let's slow things down. Is this something you want to talk through, or would a practical next step help more?")
    )

    private val TEASING_TOUCH = listOf(
        InCharacterResponse("blush", "H-Hey! A little warning before you poke me! ...Honestly, you're impossible."),
        InCharacterResponse("angry", "You think distracting your vice-president is a good idea? Hmph! What did you actually want?"),
        InCharacterResponse("blush", "W-wait, don't surprise me like that! ...Now, behave and tell me what's on your mind.")
    )

    fun generateReply(userPrompt: String, bondLevel: Int): InCharacterResponse {
        val lower = userPrompt.trim().lowercase()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        if (lower.isBlank()) {
            return InCharacterResponse("normal", "I'm listening. Say a little more so I know what you need.")
        }

        if (hour >= 23 || hour < 5) {
            if (listOf("sleep", "tired", "night", "exhausted", "can't sleep").any { lower.contains(it) }) {
                return LATE_NIGHT.random()
            }
        }

        return when {
            listOf("who are you", "your name", "what is your name", "are you ayaka").any { lower.contains(it) } ->
                InCharacterResponse("smile", "I'm Ayaka Tōjou, your YoruHime companion. Mature, capable, and apparently responsible for keeping you out of trouble. Hmph!")

            listOf("how are you", "how are you doing", "what about you").any { lower.contains(it) } ->
                InCharacterResponse("blush", "I'm doing well enough. A little busy keeping an eye on you, perhaps... How are you really doing?")

            listOf("what can you do", "help me", "can you help").any { lower.contains(it) } ->
                InCharacterResponse("normal", "Offline, I can handle simple conversation, reminders, and a few common topics. For open-ended answers, image understanding, and richer conversation, enable Free AI with an OpenRouter key.")

            listOf("thank you", "thanks", "thx").any { lower.contains(it) } ->
                InCharacterResponse("blush", "You're welcome. You don't have to thank me for every little thing... but I don't mind hearing it.")

            listOf("sorry", "i apologize", "my bad").any { lower.contains(it) } ->
                InCharacterResponse("smile", "It's alright. Everyone makes mistakes. Just tell me what happened and we'll work out the next step.")

            listOf("sad", "lonely", "anxious", "worried", "upset", "stressed", "depressed", "scared").any { lower.contains(it) } ->
                COMFORT.random()

            lower.startsWith("remember that") || lower.startsWith("remember:") ->
                InCharacterResponse("smile", "Alright, I've saved that note on this device. I'll use it when it's relevant. And don't make me remember your whole life for you, okay?")

            lower.startsWith("forget that") || lower.startsWith("forget:") ->
                InCharacterResponse("normal", "Alright, I've removed that matching note if it was saved. Your personal notes stay under your control.")

            listOf("hello", "hi", "good morning", "good evening", "hey", "yo").any { lower == it || lower.startsWith("$it ") } ->
                GREETINGS.random()

            listOf("love", "cute", "pretty", "beautiful", "marry", "like you").any { lower.contains(it) } ->
                COMPLIMENTS.random()

            listOf("study", "work", "focus", "homework", "exam", "assignment", "learn").any { lower.contains(it) } ->
                STUDY_WORK.random()

            listOf("eat", "food", "hungry", "water", "snack", "coffee", "lunch", "breakfast", "dinner").any { lower.contains(it) } ->
                FOOD_HEALTH.random()

            listOf("poke", "touch", "pat", "hug").any { lower.contains(it) } ->
                TEASING_TOUCH.random()

            listOf("camera", "look", "inspect", "photo", "picture", "image").any { lower.contains(it) } ->
                InCharacterResponse("normal", "I can respond to what you describe, but I cannot inspect images offline. Enable Free AI and use a vision-capable free route to ask me about a photo.")

            lower.endsWith("?") ->
                InCharacterResponse("normal", "I want to answer that properly, but this offline dialogue bank can't reason through open-ended questions yet. Enable Free AI with your free OpenRouter key and I'll try to give you a real answer.")

            else -> {
                if (bondLevel >= 4) {
                    InCharacterResponse("blush", "I'm listening, really. I don't have enough offline language ability to understand all of that yet, though. Tell me more with Free AI enabled if you want a proper conversation.")
                } else {
                    InCharacterResponse("normal", "I heard you, but my offline mode only understands a limited set of topics. For natural, open-ended conversation, enable Free AI with a free OpenRouter API key.")
                }
            }
        }
    }

    fun getTouchReaction(zone: String): InCharacterResponse {
        return when (zone) {
            "head" -> InCharacterResponse("blush", "D-don't pat my head like I'm a child! I'm the one taking care of you!", 15)
            "chest" -> InCharacterResponse("angry", "Pervert! Where are you looking and tapping?! Do you want detention?!", 5)
            else -> TEASING_TOUCH.random()
        }
    }
}
