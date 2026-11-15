package com.aistudio.yoruhime.ui.avatar

import org.junit.Assert.assertEquals
import org.junit.Test

class AyakaConversationCueClassifierTest {

    @Test
    fun distressTakesPriorityOverPlayfulKeywords() {
        assertEquals(
            AvatarCue.USER_DISTRESSED,
            AyakaConversationCueClassifier.classify("I'm overwhelmed, stop teasing me")
        )
    }

    @Test
    fun tiredOrLonelyMessagesSelectSupportiveCues() {
        assertEquals(AvatarCue.USER_DISTRESSED, AyakaConversationCueClassifier.classify("I'm tired and lonely"))
    }

    @Test
    fun complimentSelectsRikoInspiredTsundereMode() {
        val cue = AyakaConversationCueClassifier.classify("Ayaka, you're beautiful")
        assertEquals(AvatarCue.COMPLIMENT, cue)
        assertEquals(AyakaPersona.RIKO_TSUNDERE, AyakaConversationCueClassifier.personaFor(cue))
    }

    @Test
    fun ordinaryConversationKeepsAyakaInHerCaringPersona() {
        val cue = AyakaConversationCueClassifier.classify("Can you help me plan tomorrow?")
        assertEquals(AvatarCue.QUESTION, cue)
        assertEquals(AyakaPersona.MOMMY_TSUNDERE, AyakaConversationCueClassifier.personaFor(cue))
    }
}
