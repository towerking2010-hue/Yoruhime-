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

    private val TIRED_CARE = listOf(
        InCharacterResponse("sad", "Oh, sweetheart... if you're that exhausted, stop pushing yourself for a moment. Drink some water, put the phone down, and let yourself rest. I'll help you plan tomorrow."),
        InCharacterResponse("blush", "You're running on empty again? Honestly... take a proper break, Junior. I can be strict about deadlines, but I'm not asking you to run yourself into the ground."),
        InCharacterResponse("sad", "That sounds like a genuinely rough day. You don't have to prove anything to me tonight. Get comfortable, breathe, and sleep if you can.")
    )

    private val CASUAL_REPLIES = listOf(
        InCharacterResponse("smile", "Yo? That's your grand opening? Hmph. Fine, I'm listening. What do you need?"),
        InCharacterResponse("blush", "Oh, you're here. I was just checking the schedule, not waiting for you. Now tell me what's going on."),
        InCharacterResponse("normal", "I'm here. Give me the short version—what's happening?")
    )

    fun generateReply(userPrompt: String, bondLevel: Int): InCharacterResponse {
        val lower = userPrompt.trim().lowercase()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // Emotional needs take priority over teasing or generic keyword replies.
        if (listOf("exhausted", "sleepy", "worn out", "overwhelmed", "burnt out", "burned out").any { keyword -> lower.contains(keyword) } ||
            (lower.contains("tired") && listOf("so ", "really", "very").any { keyword -> lower.contains(keyword) })) {
            return TIRED_CARE.random()
        }

        if ((hour >= 22 || hour < 6) &&
            listOf("sleep", "tired", "night", "awake", "bed").any { keyword -> lower.contains(keyword) }) {
            return LATE_NIGHT.random()
        }

        return when {
            lower.matches(Regex("^(yo|sup|hey|hiya|hello|hi|good morning|good evening)[!. ]*$")) -> CASUAL_REPLIES.random()
            listOf("hello", "hi ", "morning", "hey", "good evening", "good morning").any { keyword -> lower.contains(keyword) } -> GREETINGS.random()
            listOf("love", "cute", "pretty", "beautiful", "marry", "like you", "miss you").any { keyword -> lower.contains(keyword) } -> COMPLIMENTS.random()
            listOf("study", "work", "focus", "homework", "exam", "assignment").any { keyword -> lower.contains(keyword) } -> STUDY_WORK.random()
            listOf("eat", "food", "hungry", "water", "snack", "coffee", "lunch", "dinner").any { keyword -> lower.contains(keyword) } -> FOOD_HEALTH.random()
            listOf("poke", "touch", "pat", "hug").any { keyword -> lower.contains(keyword) } -> TEASING_TOUCH.random()
            listOf("camera", "look at", "show you", "see this", "inspect").any { keyword -> lower.contains(keyword) } ->
                InCharacterResponse("normal", "I can't see your room or surroundings from text alone. Send me a photo with the camera or attachment button, and switch to Cloud mode if you want me to examine it. I won't pretend I've seen something I haven't.")
            lower.length <= 18 -> CASUAL_REPLIES.random()
            else -> {
                if (bondLevel >= 4 && (0..2).random() == 0) {
                    InCharacterResponse("blush", "...Even when you ramble about strange things, I find myself listening. Don't let it go to your head, though! Keep going.")
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

}
