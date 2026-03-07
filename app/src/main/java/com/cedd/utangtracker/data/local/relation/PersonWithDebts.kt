package com.cedd.utangtracker.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.cedd.utangtracker.data.local.entity.DebtEntity
import com.cedd.utangtracker.data.local.entity.PersonEntity

data class PersonWithDebts(
    @Embedded val person: PersonEntity,
    @Relation(parentColumn = "id", entityColumn = "personId")
    val debts: List<DebtEntity>
)
