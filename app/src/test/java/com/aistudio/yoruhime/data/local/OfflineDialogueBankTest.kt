package com.aistudio.yoruhime.data.local

import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineDialogueBankTest {
    @Test
    fun exhaustedMessageReceivesCareInsteadOfScolding() {
        val response = OfflineDialogueBank.generateReply(
            "Ayaka, I'm so exhausted and sleepy...",
            bondLevel = 2
        )
        assertTrue(response.emotion == "sad" || response.emotion == "blush")
        assertTrue(response.text.contains("rest", ignoreCase = true) ||
            response.text.contains("break", ignoreCase = true) ||
            response.text.contains("sleep", ignoreCase = true))
    }

    @Test
    fun textOnlyRoomRequestDoesNotPretendToSeeRoom() {
        val response = OfflineDialogueBank.generateReply("look at my room", bondLevel = 2)
        assertTrue(response.text.contains("photo", ignoreCase = true))
        assertTrue(response.text.contains("can't see", ignoreCase = true))
    }

    @Test
    fun shortCasualGreetingGetsConversationalReply() {
        val response = OfflineDialogueBank.generateReply("yo", bondLevel = 2)
        assertTrue(response.text.contains("listening", ignoreCase = true) ||
            response.text.contains("what do you need", ignoreCase = true) ||
            response.text.contains("what's happening", ignoreCase = true))
    }
}
