package com.cedd.utangtracker.presentation.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val bullets: List<String>,
    val gradientStart: Color,
    val gradientEnd: Color
)

private val pages = listOf(
    OnboardingPage(
        emoji = "\uD83D\uDC4B",
        title = "Welcome to\nUtang Tracker",
        subtitle = "The smart way to track debts — simple, clear, and trustworthy.",
        bullets = listOf(
            "Never forget who owes you",
            "Know exactly how much you owe",
            "Keep records your family can trust"
        ),
        gradientStart = Color(0xFF4361EE),
        gradientEnd   = Color(0xFF7B8FF7)
    ),
    OnboardingPage(
        emoji = "\uD83D\uDCC8",
        title = "Dashboard at\na Glance",
        subtitle = "Open the app and instantly see your full money picture.",
        bullets = listOf(
            "Total receivables vs. total payables",
            "Overdue debts highlighted in red",
            "Recent activity shown up front"
        ),
        gradientStart = Color(0xFF0EA5E9),
        gradientEnd   = Color(0xFF38BDF8)
    ),
    OnboardingPage(
        emoji = "\uD83D\uDC65",
        title = "Debts &\nPersons",
        subtitle = "Organize by person — see every transaction with each borrower or lender.",
        bullets = listOf(
            "\"Owed to Me\" tab — track what you lent",
            "\"I Owe\" tab — track what you borrowed",
            "Record partial payments anytime"
        ),
        gradientStart = Color(0xFF16A34A),
        gradientEnd   = Color(0xFF4ADE80)
    ),
    OnboardingPage(
        emoji = "\uD83D\uDCDD",
        title = "Digital Contracts",
        subtitle = "Generate a signed PDF loan agreement — valid as supporting evidence in disputes.",
        bullets = listOf(
            "Auto-generates for loans ₱5,000+",
            "Borrower signs digitally on their phone",
            "Valid supporting evidence if needed"
        ),
        gradientStart = Color(0xFFD97706),
        gradientEnd   = Color(0xFFFBBF24)
    ),
    OnboardingPage(
        emoji = "\uD83D\uDCC5",
        title = "Loan Reservations",
        subtitle = "When someone asks to borrow in advance, record the request before money moves.",
        bullets = listOf(
            "Set planned date and amount",
            "Approve or reject the request",
            "Convert to a real debt with one tap"
        ),
        gradientStart = Color(0xFF7C3AED),
        gradientEnd   = Color(0xFFA78BFA)
    )
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutines()
    val isLastPage = pagerState.currentPage == pages.size - 1

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { index ->
            OnboardingPage(page = pages[index])
        }

        // ── Skip button ───────────────────────────────────────────────────────
        if (!isLastPage) {
            TextButton(
                onClick = onFinish,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 16.dp)
            ) {
                Text("Skip", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }

        // ── Bottom controls ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Dot indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pages.size) { index ->
                    DotIndicator(selected = index == pagerState.currentPage)
                }
            }

            // Next / Get Started button
            Button(
                onClick = {
                    if (isLastPage) {
                        onFinish()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor   = pages[pagerState.currentPage].gradientStart
                )
            ) {
                Text(
                    if (isLastPage) "Get Started" else "Next",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun OnboardingPage(page: OnboardingPage) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(page.gradientStart, page.gradientEnd)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(top = 120.dp, bottom = 180.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Emoji in a frosted circle
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(120.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(page.emoji, fontSize = 52.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Title
            Text(
                page.title,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )

            // Subtitle
            Text(
                page.subtitle,
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(4.dp))

            // Bullets
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    page.bullets.forEach { bullet ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "\u2713",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                            Text(
                                bullet,
                                color = Color.White,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DotIndicator(selected: Boolean) {
    val width by animateDpAsState(
        targetValue = if (selected) 24.dp else 8.dp,
        animationSpec = tween(300),
        label = "dot_width"
    )
    val color by animateColorAsState(
        targetValue = if (selected) Color.White else Color.White.copy(alpha = 0.4f),
        animationSpec = tween(300),
        label = "dot_color"
    )
    Box(
        modifier = Modifier
            .height(8.dp)
            .width(width)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun rememberCoroutines() = rememberCoroutineScope()
