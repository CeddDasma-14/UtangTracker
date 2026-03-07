package com.cedd.utangtracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contracts",
    foreignKeys = [ForeignKey(
        entity = DebtEntity::class,
        parentColumns = ["id"],
        childColumns = ["debtId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("debtId", unique = true)]
)
data class ContractEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val contractNumber: String,       // UTC-2026-00001
    val lenderName: String,
    val borrowerName: String,
    val amount: Double,
    val purpose: String,
    val dateCreated: Long = System.currentTimeMillis(),
    val dateDue: Long? = null,
    val interestRate: Double = 0.0,
    val lenderSignaturePath: String? = null,
    val borrowerSignaturePath: String? = null,
    val witnessName: String? = null,
    val witnessSignaturePath: String? = null,
    val pdfPath: String? = null,
    val isSigned: Int = 0,            // 0 = not yet, 1 = locked
    val createdAt: Long = System.currentTimeMillis(),
    // ── Remote signing via secure link ────────────────────────────────────────
    val secureLinkToken: String? = null,              // UUID — used in the borrower URL
    val secureLinkExpiresAt: Long? = null,            // millis; token valid for 72 h
    val remoteBorrowerStatus: String = "none",        // "none" | "pending" | "completed"
    val remoteBorrowerFullName: String? = null,
    val remoteBorrowerAddress: String? = null,
    val remoteBorrowerIdType: String? = null,
    val remoteBorrowerIdNumber: String? = null,
    val remoteBorrowerIdImagePath: String? = null,    // local filesDir path after download
    val remoteBorrowerSignaturePath: String? = null,  // local filesDir path after download
    val collateral: String? = null,                   // e.g. "Samsung Galaxy S21, ATM card"
    val language: String = "en",                      // "en" | "tl" | "bis"
    val borrowerSignedAt: Long? = null,               // epoch ms when borrower remotely signed
    val remoteBorrowerIdVerificationStatus: String = "none",
    // "none" | "verifying" | "verified" | "mismatch" | "unreadable"
    val remoteBorrowerIdVerificationNote: String? = null,
    val disbursementReceiptPaths: String? = null  // comma-separated absolute file paths
)
