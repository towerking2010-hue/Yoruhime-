package com.aistudio.yoruhime.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_bond")
data class UserBondEntity(
    @PrimaryKey
    val id: Int = 1,
    val bondExp: Int = 20,
    val bondLevel: Int = 1,
    val streakDays: Int = 1,
    val totalStudyMinutes: Int = 0,
    val nickname: String = "Junior",
    val customBedtimePhrase: String = "It's 11:00 PM curfew! Put your phone down right now and take a rest, you hopeless slacker!",
    val bedtimeHour: Int = 23,
    val bedtimeMinute: Int = 0,
    val lastActiveTimestamp: Long = System.currentTimeMillis()
) {
    fun getTierTitle(): String {
        return when (bondLevel) {
            1 -> "Hopeless Slacker"
            2 -> "Stubborn Junior"
            3 -> "Promising Student"
            4 -> "Trusted Confidant"
            else -> "Irreplaceable Soulmate"
        }
    }

    fun getNextLevelExp(): Int = bondLevel * 100
}
