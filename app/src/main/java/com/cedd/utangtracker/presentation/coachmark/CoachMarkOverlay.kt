package com.cedd.utangtracker.presentation.coachmark

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CoachMarkStep(
    val title: String,
    val message: String,
    val spotlightCenter: Offset? = null,
    val spotlightRadius: Float = 120f
)

@Composable
fun CoachMarkOverlay(
    steps: List<CoachMarkStep>,
    onFinish: () -> Unit
) {
    if (steps.isEmpty()) {
        LaunchedEffect(Unit) { onFinish() }
        return
    }

    var currentStep by remember { mutableIntStateOf(0) }

    if (currentStep >= steps.size) {
        LaunchedEffect(Unit) { onFinish() }
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(currentStep) {
                detectTapGestures {
                    if (currentStep < steps.size - 1) currentStep++ else onFinish()
                }
            }
    ) {
        val step = steps[currentStep]
        val screenHeightPx = constraints.maxHeight.toFloat()
        val screenWidthPx  = constraints.maxWidth.toFloat()
        val density        = LocalDensity.current

        // ── Dimmed overlay with spotlight cutout ──────────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.99f }
        ) {
            drawRect(Color.Black.copy(alpha = 0.72f))
            step.spotlightCenter?.let { center ->
                drawCircle(
                    color = Color.Transparent,
                    radius = step.spotlightRadius,
                    center = center,
                    blendMode = BlendMode.Clear
                )
            }
        }

        // ── Animated speech-bubble tooltip ────────────────────────────────────
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                (fadeIn() + slideInVertically { 20 }) togetherWith
                        (fadeOut() + slideOutVertically { -20 })
            },
            label = "coach_tooltip",
            modifier = Modifier.fillMaxSize()
        ) { idx ->
            val s = steps.getOrNull(idx) ?: return@AnimatedContent

            val hasSpotlight    = s.spotlightCenter != null
            // Spotlight in bottom 45% of screen → tooltip above with arrow pointing down
            val arrowPointsDown = hasSpotlight && s.spotlightCenter!!.y > screenHeightPx * 0.55f
            // How far the spotlight X deviates from the screen center (pixels)
            val arrowOffsetPx   = s.spotlightCenter?.x?.minus(screenWidthPx / 2f) ?: 0f
            // Bottom padding: push card up so the arrow tip sits just above the spotlighted icon
            val bottomPadding: Dp = when {
                !hasSpotlight   -> 0.dp
                arrowPointsDown -> with(density) {
                    (screenHeightPx - s.spotlightCenter!!.y + s.spotlightRadius + 28f).toDp()
                }
                else            -> with(density) {
                    (screenHeightPx - s.spotlightCenter!!.y + s.spotlightRadius + 28f).toDp()
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .align(
                            if (!hasSpotlight) Alignment.Center else Alignment.BottomCenter
                        )
                        .then(
                            if (!hasSpotlight) Modifier else Modifier.padding(bottom = bottomPadding)
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Arrow pointing up (tooltip is below a top-half element)
                    if (hasSpotlight && !arrowPointsDown) {
                        CalloutArrow(pointingDown = false, arrowOffsetPx = arrowOffsetPx)
                    }

                    CalloutCard(
                        step = s,
                        stepIndex = idx,
                        totalSteps = steps.size,
                        isLast = idx == steps.size - 1,
                        onNext = { if (currentStep < steps.size - 1) currentStep++ else onFinish() },
                        onSkip = onFinish
                    )

                    // Arrow pointing down (tooltip is above a bottom-half element)
                    if (arrowPointsDown) {
                        CalloutArrow(pointingDown = true, arrowOffsetPx = arrowOffsetPx)
                    }
                }
            }
        }
    }
}

// ── Callout arrow triangle ────────────────────────────────────────────────────

@Composable
private fun CalloutArrow(pointingDown: Boolean, arrowOffsetPx: Float = 0f) {
    val color = MaterialTheme.colorScheme.surface
    // Canvas fills card width so the tip can track the spotlight X position
    Canvas(modifier = Modifier.fillMaxWidth().height(14.dp)) {
        val halfBase = 16.dp.toPx()
        // Tip X = canvas center + offset, clamped so the triangle stays inside the canvas
        val tipX = (size.width / 2f + arrowOffsetPx).coerceIn(halfBase, size.width - halfBase)
        val path = Path().apply {
            if (pointingDown) {
                moveTo(tipX - halfBase, 0f)
                lineTo(tipX + halfBase, 0f)
                lineTo(tipX, size.height)
                close()
            } else {
                moveTo(tipX, 0f)
                lineTo(tipX + halfBase, size.height)
                lineTo(tipX - halfBase, size.height)
                close()
            }
        }
        drawPath(path, color)
    }
}

// ── Tooltip card ─────────────────────────────────────────────────────────────

@Composable
private fun CalloutCard(
    step: CoachMarkStep,
    stepIndex: Int,
    totalSteps: Int,
    isLast: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Progress dots
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(totalSteps) { i ->
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (i == stepIndex) 24.dp else 6.dp)
                            .background(
                                color = if (i == stepIndex) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
                }
            }

            Text(
                step.title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                step.message,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isLast) {
                    TextButton(onClick = onSkip) {
                        Text(
                            "Skip tour",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                Button(
                    onClick = onNext,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isLast) "Let's go!" else "Next  →")
                }
            }
        }
    }
}
