package com.cedd.utangtracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ledger_entries",
    foreignKeys = [ForeignKey(
        entity = DebtEntity::class,
        parentColumns = ["id"],
        childColumns = ["debtId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index("debtId"),
        Index(value = ["debtId", "year", "month"], unique = true)
    ]
)
data class LedgerEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val year: Int,
    val month: Int,              // 1–12
    val openingBalance: Double,
    val interestAdded: Double,   // 0 if not an interest cycle month
    val carryOverAdded: Double,  // 0 if not applicable for this entry
    val paymentAmount: Double,   // 0 = missed payment
    val closingBalance: Double,
    val isMissedPayment: Boolean = false,
    val paymentDate: Long? = null,               // specific date payment was made (null = not recorded)
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
