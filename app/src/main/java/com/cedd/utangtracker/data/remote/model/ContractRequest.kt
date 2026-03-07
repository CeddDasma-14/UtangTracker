package com.cedd.utangtracker.data.remote.model

import java.util.Date

/** Mirrors the Firestore document at `contract_requests/{token}`. */
data class ContractRequest(
    val contractNumber: String = "",
    val lenderName: String = "",
    val borrowerName: String = "",
    val amount: Double = 0.0,
    val purpose: String = "",
    val dateDue: Long? = null,
    val interestRate: Double = 0.0,
    val collateral: String = "",
    // "pending" → borrower hasn't submitted yet
    // "completed" → borrower submitted all info
    // "expired"   → TTL elapsed (set by Firestore TTL policy or app logic)
    val status: String = "pending",
    val expiresAt: Long = 0L,
    val createdAt: Long = 0L,
    // ── Filled by borrower via the web form ───────────────────────────────────
    val borrowerFullName: String = "",
    val borrowerAddress: String = "",
    val borrowerPhone: String = "",
    val borrowerIdType: String = "",
    val borrowerIdNumber: String = "",
    val borrowerSignatureBase64: String = "", // PNG data-URI encoded directly in Firestore
    val borrowerIdPhotoBase64: String = "",   // JPEG data-URI of government ID photo
    val borrowerCollateral: String = "",      // Comma-separated collateral items from borrower
    val completedAt: Date? = null
)
