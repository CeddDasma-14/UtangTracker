package com.cedd.utangtracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reservations",
    foreignKeys = [ForeignKey(
        entity = PersonEntity::class,
        parentColumns = ["id"],
        childColumns = ["personId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("personId")]
)
data class ReservationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val type: String,               // "OWED_TO_ME" | "I_OWE"
    val amount: Double,
    val purpose: String,
    val plannedDate: Long,          // when the borrower wants to receive the loan
    val notes: String = "",
    val status: String = "PENDING", // "PENDING" | "APPROVED" | "REJECTED" | "CONVERTED"
    val createdAt: Long = System.currentTimeMillis()
)
