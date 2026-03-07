package com.cedd.utangtracker.presentation.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import com.cedd.utangtracker.R

private const val TAGLINE = "Smart Loan & Debt Tracker"

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var phase        by remember { mutableIntStateOf(0) }
    var visibleChars by remember { mutableIntStateOf(0) }
    var showCursor   by remember { mutableStateOf(false) }
    var typingDone   by remember { mutableStateOf(false) }

    // ── Phase 1: logo fades + scales in ──────────────────────────────────────
    val logoAlpha by animateFloatAsState(
        targetValue    = if (phase >= 1) 1f else 0f,
        animationSpec  = tween(700, easing = FastOutSlowInEasing),
        label          = "logo_alpha"
    )
    val logoScale by animateFloatAsState(
        targetValue   = if (phase >= 1) 1f else 0.4f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
        label         = "logo_scale"
    )

    // ── Continuous arc rotation + cursor blink (shared infinite transition) ──
    val infinite = rememberInfiniteTransition(label = "infinite")
    val arcAngle by infinite.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label         = "arc_angle"
    )
    val arcAlpha by animateFloatAsState(
        targetValue   = if (phase >= 1) 1f else 0f,
        animationSpec = tween(600),
        label         = "arc_alpha"
    )
    val cursorAlpha by infinite.animateFloat(
        initialValue  = 1f,
        targetValue   = 0f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label         = "cursor_blink"
    )

    // ── Phase 2: "LoanTrack" slides up ────────────────────────────────────────
    val titleAlpha by animateFloatAsState(
        targetValue   = if (phase >= 2) 1f else 0f,
        animationSpec = tween(500),
        label         = "title_alpha"
    )
    val titleY by animateFloatAsState(
        targetValue   = if (phase >= 2) 0f else 48f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label         = "title_y"
    )

    // ── Phase 3 container: slides up to reveal typewriter area ───────────────
    val tagContainerAlpha by animateFloatAsState(
        targetValue   = if (phase >= 3) 1f else 0f,
        animationSpec = tween(300),
        label         = "tag_container_alpha"
    )
    val tagY by animateFloatAsState(
        targetValue   = if (phase >= 3) 0f else 24f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label         = "tag_y"
    )

    // ── Shimmer brush (active after typing finishes) ──────────────────────────
    val shimmerX by infinite.animateFloat(
        initialValue  = -300f,
        targetValue   = 700f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label         = "shimmer_x"
    )
    val taglineBrush: Brush = if (typingDone) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.55f),
                Color.White,
                Color(0xFF90CAF9),
                Color.White,
                Color.White.copy(alpha = 0.55f),
            ),
            start = Offset(shimmerX, 0f),
            end   = Offset(shimmerX + 300f, 0f)
        )
    } else {
        SolidColor(Color.White.copy(alpha = 0.65f))
    }

    // ── Main animation sequence ───────────────────────────────────────────────
    LaunchedEffect(Unit) {
        delay(150)
        phase = 1                          // logo + arcs appear
        delay(750)
        phase = 2                          // "LoanTrack" slides up
        delay(350)
        phase = 3                          // tagline container slides up
        delay(100)
        showCursor = true
        for (i in 1..TAGLINE.length) {     // typewriter
            visibleChars = i
            delay(38)
        }
        delay(350)                         // hold cursor briefly
        showCursor = false
        typingDone = true                  // shimmer starts
        delay(1350)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A2A6C), Color(0xFF0A0A18)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Logo + rotating arc ring ──────────────────────────────────────
            Box(
                modifier = Modifier.size(168.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(arcAlpha)
                        .rotate(arcAngle)
                ) {
                    val stroke = 6.dp.toPx()
                    val inset  = stroke / 2f
                    val tl     = Offset(inset, inset)
                    val sz     = Size(size.width - stroke, size.height - stroke)

                    drawArc(
                        color      = Color(0xFF4361EE),
                        startAngle = 40f,
                        sweepAngle = 210f,
                        useCenter  = false,
                        topLeft    = tl,
                        size       = sz,
                        style      = Stroke(stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color      = Color(0xFF16A34A),
                        startAngle = 278f,
                        sweepAngle = 110f,
                        useCenter  = false,
                        topLeft    = tl,
                        size       = sz,
                        style      = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }

                Image(
                    painter            = painterResource(R.drawable.logo_loantrack),
                    contentDescription = "LoanTrack",
                    modifier           = Modifier
                        .size(114.dp)
                        .alpha(logoAlpha)
                        .scale(logoScale)
                )
            }

            Spacer(Modifier.height(36.dp))

            // ── "LoanTrack" title ─────────────────────────────────────────────
            Text(
                text          = "LoanTrack",
                fontSize      = 40.sp,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                color         = Color.White,
                modifier      = Modifier
                    .alpha(titleAlpha)
                    .offset(y = titleY.dp)
            )

            Spacer(Modifier.height(10.dp))

            // ── Tagline: typewriter + shimmer ─────────────────────────────────
            Row(
                modifier            = Modifier
                    .alpha(tagContainerAlpha)
                    .offset(y = tagY.dp),
                verticalAlignment   = Alignment.CenterVertically
            ) {
                Text(
                    text     = TAGLINE.take(visibleChars),
                    style    = TextStyle(brush = taglineBrush),
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
                if (showCursor) {
                    Text(
                        text     = "|",
                        color    = Color.White.copy(alpha = cursorAlpha),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }
        }
    }
}
