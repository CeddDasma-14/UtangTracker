package com.cedd.utangtracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    foreignKeys = [ForeignKey(
        entity = DebtEntity::class,
        parentColumns = ["id"],
        childColumns = ["debtId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("debtId")]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val amount: Double,
    val datePaid: Long = System.currentTimeMillis(),
    val notes: String = ""
)
