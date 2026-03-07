package com.cedd.utangtracker.presentation.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

private val phpFormat = NumberFormat.getNumberInstance(Locale("fil", "PH")).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

fun formatPeso(amount: Double) = "₱${phpFormat.format(amount)}"

@Composable
fun CurrencyText(
    amount: Double,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Text(
        text = formatPeso(amount),
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        modifier = modifier
    )
}
