package com.cedd.utangtracker.data.local.dao

import androidx.room.*
import com.cedd.utangtracker.data.local.entity.ContractEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContractDao {
    @Query("SELECT * FROM contracts WHERE debtId = :debtId LIMIT 1")
    fun getContractForDebt(debtId: Long): Flow<ContractEntity?>

    @Query("SELECT COUNT(*) FROM contracts")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contract: ContractEntity): Long

    @Update
    suspend fun update(contract: ContractEntity)

    @Delete
    suspend fun delete(contract: ContractEntity)
}
