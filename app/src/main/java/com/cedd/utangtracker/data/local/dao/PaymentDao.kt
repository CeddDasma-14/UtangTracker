package com.cedd.utangtracker.data.local.dao

import androidx.room.*
import com.cedd.utangtracker.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE debtId = :debtId ORDER BY datePaid DESC")
    fun getPaymentsForDebt(debtId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments ORDER BY datePaid DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: PaymentEntity): Long

    @Delete
    suspend fun delete(payment: PaymentEntity)
}
