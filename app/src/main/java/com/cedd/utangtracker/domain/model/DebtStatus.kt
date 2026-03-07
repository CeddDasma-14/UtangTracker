package com.cedd.utangtracker.domain.model

enum class DebtStatus(val value: String) {
    ACTIVE("ACTIVE"),
    OVERDUE("OVERDUE"),
    SETTLED("SETTLED");

    companion object {
        fun from(value: String) = entries.firstOrNull { it.value == value } ?: ACTIVE
    }
}
