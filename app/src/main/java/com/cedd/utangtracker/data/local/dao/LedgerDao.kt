package com.cedd.utangtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cedd.utangtracker.data.local.entity.LedgerEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledger_entries WHERE debtId = :debtId ORDER BY year ASC, month ASC")
    fun getEntriesForDebt(debtId: Long): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM ledger_entries WHERE debtId = :debtId ORDER BY year DESC, month DESC LIMIT 1")
    suspend fun getLatestEntry(debtId: Long): LedgerEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LedgerEntryEntity): Long

    @Update
    suspend fun update(entry: LedgerEntryEntity)

    @Delete
    suspend fun delete(entry: LedgerEntryEntity)

    @Query("DELETE FROM ledger_entries WHERE debtId = :debtId")
    suspend fun deleteAllForDebt(debtId: Long)
}
