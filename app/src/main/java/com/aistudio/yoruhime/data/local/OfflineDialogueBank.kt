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

    private val GREETING_REPLIES = listOf(
        InCharacterResponse("smile", "Hey. I'm here, and I'm listening. What have you been up to?"),
        InCharacterResponse("blush", "Oh, you came back. Hmph... it's nice to see you. I'm listening—how's your day going?"),
        InCharacterResponse("normal", "Hi! I'm listening—what's on your mind?")
    )

    private val THANK_YOU_REPLIES = listOf(
        InCharacterResponse("blush", "You're welcome. I was happy to help... not that you need to make a big deal out of it."),
        InCharacterResponse("smile", "Anytime. Just make sure you give yourself some credit too.")
    )

    private val CHECK_IN_REPLIES = listOf(
        InCharacterResponse("smile", "Not much chaos on my end. I'm right here with you. What's happening in your world?"),
        InCharacterResponse("normal", "I'm here and ready to listen. Tell me what's going on."),
        InCharacterResponse("blush", "I was keeping myself busy, obviously. But you have my attention now. How are you?")
    )

    fun generateReply(userPrompt: String, bondLevel: Int): InCharacterResponse {
        val lower = userPrompt.trim().lowercase()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val exactGreeting = lower.matches(Regex("^(yo|sup|hey|hiya|hello|hi|good morning|good afternoon|good evening)[!. ]*$"))

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
            exactGreeting -> GREETING_REPLIES.random()
            listOf("what's happening", "what is happening", "what's up", "whats up", "how are you", "how's it going", "hows it going").any { lower.contains(it) } ->
                CHECK_IN_REPLIES.random()
            listOf("thank you", "thanks", "thx", "appreciate it").any { lower.contains(it) } -> THANK_YOU_REPLIES.random()
            listOf("love", "cute", "pretty", "beautiful", "marry", "like you", "miss you").any { keyword -> lower.contains(keyword) } -> COMPLIMENTS.random()
            listOf("study", "work", "focus", "homework", "exam", "assignment").any { keyword -> lower.contains(keyword) } -> STUDY_WORK.random()
            listOf("eat", "food", "hungry", "water", "snack", "coffee", "lunch", "dinner").any { keyword -> lower.contains(keyword) } -> FOOD_HEALTH.random()
            listOf("poke", "touch", "pat", "hug").any { keyword -> lower.contains(keyword) } -> TEASING_TOUCH.random()
            listOf("camera", "look at", "show you", "see this", "inspect").any { keyword -> lower.contains(keyword) } ->
                InCharacterResponse("normal", "I can't see your surroundings from text alone. Attach a photo or use the camera snapshot, then connect Cloud mode with your OpenRouter key so I can analyse the actual image. I won't pretend I've seen something I haven't.")
            lower.contains("?") || listOf("what ", "why ", "how ", "when ", "where ", "who ", "can you", "could you", "should i", "should I").any { lower.startsWith(it) } ->
                InCharacterResponse("normal", "I want to give you a proper answer, but I'm in offline mode right now. I can handle a few basic topics, but I can't reason through every question without a connected AI model. Tell me a little more, or connect a free Cloud model when you're ready.")
            lower.length < 4 -> InCharacterResponse("smile", "I'm listening. Go on—what did you want to tell me?")
            else -> {
                if (bondLevel >= 4) {
                    InCharacterResponse("smile", "I read that. I don't want to give you a random canned answer—tell me what part matters most, and I'll do my best with the offline tools I have.")
                } else {
                    InCharacterResponse("normal", "I hear you. I'm currently using limited offline replies, so I may not understand the full meaning yet. Could you explain a little more?")
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
