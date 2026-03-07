package com.cedd.utangtracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "debts",
    foreignKeys = [ForeignKey(
        entity = PersonEntity::class,
        parentColumns = ["id"],
        childColumns = ["personId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("personId")]
)
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val type: String,           // "OWED_TO_ME" | "I_OWE"
    val amount: Double,
    val paidAmount: Double = 0.0,
    val purpose: String,
    val dateCreated: Long = System.currentTimeMillis(),
    val dateDue: Long? = null,
    val interestRate: Double = 0.0,
    val autoApplyInterest: Boolean = false, // add monthly interest automatically on overdue
    val contractEnabled: Boolean = false,   // lender opted-in to digital contract (auto-on at ₱5,000+)
    val status: String = "ACTIVE", // "ACTIVE" | "SETTLED" | "OVERDUE"
    val notes: String = "",
    val disbursementReceiptPaths: String? = null,  // comma-separated absolute file paths
    val lastInterestAppliedAt: Long? = null        // timestamp of last auto-interest application
)
