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
    val lastInterestAppliedAt: Long? = null,       // timestamp of last auto-interest application
    val bankCharge: Double = 0.0,                  // one-time bank/processing fee
    val totalAmount: Double = 0.0,                 // principal + totalInterest + bankCharge (set at save time)
    val isLocked: Boolean = false,                 // locked debts cannot be deleted (Premium feature)
    // ── Loan Ledger ────────────────────────────────────────────────────────────
    val ledgerEnabled: Boolean = false,            // show/use the monthly ledger for this debt
    val ledgerCarryOver: Double = 0.0,             // previous balance carry-over from lender
    val ledgerCarryOverMonthly: Boolean = false,   // true = auto-add carry-over every month
    val ledgerCycleMonths: Int = 3,                // interest cycle length (3 = quarterly)
    val ledgerInitialBalance: Double = 0.0,        // custom opening balance (0 = use auto startingBalance)
    val ledgerCurrentBalance: Double = 0.0         // synced from last ledger entry; 0 = no entries yet
)
