package com.cedd.utangtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.cedd.utangtracker.data.local.entity.ComakerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ComakerDao {
    @Query("SELECT * FROM comakers WHERE contractId = :contractId ORDER BY id ASC")
    fun getByContractId(contractId: Long): Flow<List<ComakerEntity>>

    @Insert
    suspend fun insert(comaker: ComakerEntity): Long

    @Delete
    suspend fun delete(comaker: ComakerEntity)
}
