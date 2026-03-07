package com.cedd.utangtracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "comakers",
    foreignKeys = [ForeignKey(
        entity = ContractEntity::class,
        parentColumns = ["id"],
        childColumns = ["contractId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ComakerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(index = true) val contractId: Long,
    val fullName: String,
    val mobileNumber: String,
    val address: String
)
