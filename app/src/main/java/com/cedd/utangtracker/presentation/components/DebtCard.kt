package com.cedd.utangtracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cedd.utangtracker.data.local.entity.DebtEntity
import com.cedd.utangtracker.domain.model.DebtType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFmt = SimpleDateFormat("MMM d", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtCard(
    debt: DebtEntity,
    personName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLockToggle: (() -> Unit)? = null
) {
    val target      = if (debt.totalAmount > 0) debt.totalAmount else debt.amount
    val remaining   = (target - debt.paidAmount).coerceAtLeast(0.0)
    val isOwedToMe  = debt.type == DebtType.OWED_TO_ME.value
    val amountColor = if (isOwedToMe) MaterialTheme.colorScheme.secondary
                      else MaterialTheme.colorScheme.tertiary
    val initial     = personName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val progress    = if (target > 0) (debt.paidAmount / target).toFloat().coerceIn(0f, 1f) else 0f
    val isSettled   = debt.status == "SETTLED"

    val cardGradient = Brush.horizontalGradient(
        colors = listOf(
            amountColor.copy(alpha = 0.18f),
            amountColor.copy(alpha = 0.04f),
            Color.Transparent
        )
    )

    Surface(
        onClick   = onClick,
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        color     = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        tonalElevation  = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardGradient)
        ) {
            // Left gradient accent bar
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .align(Alignment.CenterStart)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(amountColor, amountColor.copy(alpha = 0.2f))
                        )
                    )
            )
            // Padlock icon — Premium only, shown only in PersonDetail via onLockToggle
            if (onLockToggle != null) {
                IconButton(
                    onClick = onLockToggle,
                    modifier = Modifier.align(Alignment.TopEnd).size(36.dp)
                ) {
                    Icon(
                        imageVector = if (debt.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (debt.isLocked) "Unlock debt" else "Lock debt",
                        tint = if (debt.isLocked) amountColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Colored circular avatar
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    listOf(amountColor.copy(alpha = 0.30f), amountColor.copy(alpha = 0.12f))
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = initial,
                            color      = amountColor,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 20.sp
                        )
                    }

                    // Center: name + purpose + status
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            personName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 15.sp,
                            color      = MaterialTheme.colorScheme.onSurface,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            debt.purpose,
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusBadge(debt.status)
                            debt.dateDue?.let {
                                Text(
                                    "· Due ${dateFmt.format(Date(it))}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Right: remaining amount + direction label
                    Column(horizontalAlignment = Alignment.End) {
                        CurrencyText(
                            amount     = remaining,
                            color      = amountColor,
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (isOwedToMe) "owed to me" else "I owe",
                            fontSize = 11.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── Payment progress bar ──────────────────────────────────
                if (debt.paidAmount > 0 || isSettled) {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress       = { progress },
                        modifier       = Modifier.fillMaxWidth().height(5.dp),
                        color          = amountColor,
                        trackColor     = amountColor.copy(alpha = 0.15f),
                        strokeCap      = StrokeCap.Round,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "₱${"%,.0f".format(debt.paidAmount)} paid",
                            fontSize = 10.sp,
                            color    = amountColor.copy(alpha = 0.8f)
                        )
                        Text(
                            "of ₱${"%,.0f".format(target)}",
                            fontSize = 10.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
