package com.cedd.utangtracker.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.cedd.utangtracker.data.local.entity.DebtEntity
import com.cedd.utangtracker.data.local.entity.PaymentEntity

data class DebtWithPayments(
    @Embedded val debt: DebtEntity,
    @Relation(parentColumn = "id", entityColumn = "debtId")
    val payments: List<PaymentEntity>
)
