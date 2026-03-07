package com.cedd.utangtracker.domain.model

enum class DebtType(val value: String) {
    OWED_TO_ME("OWED_TO_ME"),
    I_OWE("I_OWE");

    companion object {
        fun from(value: String) = entries.first { it.value == value }
    }
}
