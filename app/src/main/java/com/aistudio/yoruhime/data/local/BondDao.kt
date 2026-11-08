package com.aistudio.yoruhime.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BondDao {
    @Query("SELECT * FROM user_bond WHERE id = 1 LIMIT 1")
    fun getBondFlow(): Flow<UserBondEntity?>

    @Query("SELECT * FROM user_bond WHERE id = 1 LIMIT 1")
    suspend fun getBond(): UserBondEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBond(bond: UserBondEntity)
}
