package com.cedd.utangtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val photoPath: String? = null,
    val phone: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
