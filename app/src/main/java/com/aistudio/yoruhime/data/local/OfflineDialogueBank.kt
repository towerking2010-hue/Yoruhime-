package com.aistudio.yoruhime.data.local

import java.util.Calendar

data class InCharacterResponse(
    val emotion: String,
    val text: String,
    val expGain: Int = 10
)

object OfflineDialogueBank {

    private val GREETINGS = listOf(
        InCharacterResponse("normal", "Ah, you're finally awake? Don't look at me with those half-closed eyes. Wash your face and start your tasks already!"),
        InCharacterResponse("smile", "Good morning. ...Don't smile like that, I only prepared your schedule because no one else would bother."),
        InCharacterResponse("blush", "W-welcome back. Not that I was waiting for you or staring at the door the whole time! Don't flatter yourself!")
    )

    private val LATE_NIGHT = listOf(
        InCharacterResponse("angry", "Do you know what time it is?! Look at the clock! Sleep is not optional for humans, you stubborn child!"),
        InCharacterResponse("blush", "Still awake...? If you get sick from staying up, I'm the one who has to nurse you back to health! S-so go to bed!"),
        InCharacterResponse("sad", "Your dark circles are terrible. Put that glowing screen away right now. I'll stay right here until you close your eyes.")
    )

    private val STUDY_WORK = listOf(
        InCharacterResponse("normal", "Focus! Scrolling through distractions won't finish that assignment or study goal. I'm watching your screen."),
        InCharacterResponse("smile", "You actually finished a study block without complaining? Hmph... I suppose you deserve a head pat. Just this once!"),
        InCharacterResponse("angry", "Hey! Did your eyes just wander off? Fifty percent effort yields zero results. Put your back into it!")
    )

    private val COMPLIMENTS = listOf(
        InCharacterResponse("blush", "B-Baka! Why are you suddenly saying sweet things like that?! I'm only looking after you out of civic duty, okay?!"),
        InCharacterResponse("blush", "E-eh?! 'You're pretty today, Ayaka'? Stop making that goofy face! My cheeks aren't red, it's just warm in here!"),
        InCharacterResponse("smile", "...Hmph. As if praising me will get you out of your chores. ...Though, thank you. You're not completely hopeless after all.")
    )

    private val FOOD_HEALTH = listOf(
        InCharacterResponse("angry", "Don't tell me your lunch was just a pack of potato chips and soda?! Drink some water and eat real vegetables!"),
        InCharacterResponse("normal", "Have you had water in the last two hours? Go pour a glass right now. I'll wait until you drink it."),
        InCharacterResponse("smile", "I packed a healthy snack in the fridge earlier. Make sure you finish every single bite without being picky.")
    )

    private val TEASING_TOUCH = listOf(
        InCharacterResponse("blush", "H-Hey! Who gave you permission to poke me so casually?! Keep your hands to yourself!"),
        InCharacterResponse("angry", "You think poking your vice-president is funny?! If you have that much spare energy, do twenty push-ups!"),
        InCharacterResponse("blush", "...W-wait, don't tap there so suddenly! It tickles... I mean, it's highly inappropriate!")
    )

    private val GENERAL_DEVOTION = listOf(
        InCharacterResponse("normal", "What is it now? Can't handle five minutes on your own without calling for me? ...Fine, what's on your mind?"),
        InCharacterResponse("smile", "I made sure your schedule has breathing room today. Even a slacker like you needs pacing, right?"),
        InCharacterResponse("blush", "I'm only staying by your side so you don't ruin your life. Remember that! It has nothing to do with... liking your company..."),
        InCharacterResponse("happy", "When you actually apply yourself, you can do anything. Now stop making me repeat myself and get to it!")
    )

    private val CAMERA_FRONT_TIRED = listOf(
        InCharacterResponse("sad", "[Camera Analysis] Look at those eyes. Your facial tension indicates severe fatigue. Close your eyes for at least 5 minutes!"),
        InCharacterResponse("blush", "[Camera Analysis] Staring straight into the lens at me? Your posture isn't bad today, but don't lean so close... it's distracting!")
    )

    private val CAMERA_BACK_INSPECT = listOf(
        InCharacterResponse("angry", "[Camera Inspection] What is that mess on your desk?! Tidy up those papers and cables immediately before starting!"),
        InCharacterResponse("normal", "[Camera Inspection] Hmm... desk looks workable. Keep your water bottle nearby and don't let clutter pile up.")
    )

    fun generateReply(userPrompt: String, bondLevel: Int): InCharacterResponse {
        val lower = userPrompt.trim().lowercase()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // Late night check
        if (hour >= 23 || hour < 5) {
            if (lower.contains("sleep") || lower.contains("tired") || lower.contains("night")) {
                return LATE_NIGHT.random()
            }
        }

        // Keywords detection
        return when {
            lower.contains("hello") || lower.contains("hi") || lower.contains("morning") || lower.contains("hey") -> {
                GREETINGS.random()
            }
            lower.contains("love") || lower.contains("cute") || lower.contains("pretty") || lower.contains("beautiful") || lower.contains("marry") || lower.contains("like you") -> {
                COMPLIMENTS.random()
            }
            lower.contains("study") || lower.contains("work") || lower.contains("focus") || lower.contains("homework") || lower.contains("exam") -> {
                STUDY_WORK.random()
            }
            lower.contains("eat") || lower.contains("food") || lower.contains("hungry") || lower.contains("water") || lower.contains("snack") || lower.contains("coffee") -> {
                FOOD_HEALTH.random()
            }
            lower.contains("poke") || lower.contains("touch") || lower.contains("pat") || lower.contains("hug") -> {
                TEASING_TOUCH.random()
            }
            lower.contains("camera") || lower.contains("look") || lower.contains("inspect") -> {
                CAMERA_BACK_INSPECT.random()
            }
            else -> {
                // Adjust warmth based on bondLevel
                if (bondLevel >= 4 && (0..2).random() == 0) {
                    InCharacterResponse(
                        "blush",
                        "...Even when you ramble about strange things, I find myself listening. Don't let it go to your head, though! Keep going."
                    )
                } else {
                    GENERAL_DEVOTION.random()
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

    fun getCameraVisionReaction(isFrontCamera: Boolean): InCharacterResponse {
        return if (isFrontCamera) {
            CAMERA_FRONT_TIRED.random()
        } else {
            CAMERA_BACK_INSPECT.random()
        }
    }
}
