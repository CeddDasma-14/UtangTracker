package com.cedd.utangtracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cedd.utangtracker.domain.model.DebtStatus

@Composable
fun StatusBadge(status: String) {
    val debtStatus = DebtStatus.from(status)

    val textColor = when (debtStatus) {
        DebtStatus.ACTIVE  -> MaterialTheme.colorScheme.primary
        DebtStatus.OVERDUE -> MaterialTheme.colorScheme.tertiary
        DebtStatus.SETTLED -> MaterialTheme.colorScheme.secondary
    }
    val label = when (debtStatus) {
        DebtStatus.ACTIVE  -> "Active"
        DebtStatus.OVERDUE -> "Overdue"
        DebtStatus.SETTLED -> "Settled"
    }

    Text(
        text       = label,
        color      = textColor,
        fontSize   = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier   = Modifier
            .background(textColor.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
