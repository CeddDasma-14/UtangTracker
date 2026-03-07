package com.cedd.utangtracker.data.local.dao

import androidx.room.*
import com.cedd.utangtracker.data.local.entity.DebtEntity
import com.cedd.utangtracker.data.local.relation.DebtWithPayments
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY dateCreated DESC")
    fun getAllDebts(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE type = :type ORDER BY dateCreated DESC")
    fun getDebtsByType(type: String): Flow<List<DebtEntity>>

    @Transaction
    @Query("SELECT * FROM debts WHERE id = :id")
    fun getDebtWithPayments(id: Long): Flow<DebtWithPayments?>

    @Query("SELECT SUM(amount - paidAmount) FROM debts WHERE type = 'OWED_TO_ME' AND status != 'SETTLED'")
    fun getTotalOwedToMe(): Flow<Double?>

    @Query("SELECT SUM(amount - paidAmount) FROM debts WHERE type = 'I_OWE' AND status != 'SETTLED'")
    fun getTotalIOwe(): Flow<Double?>

    @Query("SELECT * FROM debts WHERE status = 'ACTIVE' AND dateDue IS NOT NULL AND dateDue < :now")
    suspend fun getActiveOverdueDebts(now: Long): List<DebtEntity>

    @Query("SELECT * FROM debts WHERE status = 'OVERDUE'")
    suspend fun getOverdueDebts(): List<DebtEntity>

    @Query("SELECT * FROM debts WHERE status = 'OVERDUE' AND autoApplyInterest = 1 AND interestRate > 0")
    suspend fun getAutoInterestOverdueDebts(): List<DebtEntity>

    @Query("SELECT * FROM debts WHERE status = 'ACTIVE' AND dateDue IS NOT NULL AND dateDue BETWEEN :from AND :to")
    suspend fun getDebtsWithUpcomingDue(from: Long, to: Long): List<DebtEntity>

    @Query("SELECT * FROM debts WHERE personId = :personId ORDER BY dateCreated DESC")
    fun getDebtsForPerson(personId: Long): Flow<List<DebtEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(debt: DebtEntity): Long

    @Update
    suspend fun update(debt: DebtEntity)

    @Delete
    suspend fun delete(debt: DebtEntity)
}
