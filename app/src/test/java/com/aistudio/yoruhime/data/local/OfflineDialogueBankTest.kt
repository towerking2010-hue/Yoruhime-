package com.aistudio.yoruhime.data.local

import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineDialogueBankTest {

    @Test
    fun identityQuestionReturnsAyakaIdentity() {
        val reply = OfflineDialogueBank.generateReply("Who are you?", 1)
        assertTrue(reply.text.contains("Ayaka Tōjou"))
    }

    @Test
    fun thanksReceivesARelevantResponse() {
        val reply = OfflineDialogueBank.generateReply("Thank you!", 1)
        assertTrue(reply.text.contains("welcome", ignoreCase = true) ||
            reply.text.contains("thank", ignoreCase = true))
    }

    @Test
    fun unknownQuestionIsHonestAboutOfflineLimitations() {
        val reply = OfflineDialogueBank.generateReply("Why is the sky blue?", 1)
        assertTrue(reply.text.contains("offline", ignoreCase = true))
    }

    @Test
    fun imageQuestionDoesNotPretendToSeeImageOffline() {
        val reply = OfflineDialogueBank.generateReply("Can you inspect this photo?", 1)
        assertTrue(reply.text.contains("cannot inspect images offline", ignoreCase = true))
    }
}
