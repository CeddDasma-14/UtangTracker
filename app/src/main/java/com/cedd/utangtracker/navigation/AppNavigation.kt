package com.cedd.utangtracker.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cedd.utangtracker.presentation.contract.ContractScreen
import com.cedd.utangtracker.presentation.dashboard.DashboardScreen
import com.cedd.utangtracker.presentation.debt.AddEditDebtScreen
import com.cedd.utangtracker.presentation.debt.DebtDetailScreen
import com.cedd.utangtracker.presentation.debt.DebtListScreen
import com.cedd.utangtracker.presentation.person.AddEditPersonScreen
import com.cedd.utangtracker.presentation.person.PersonDetailScreen
import com.cedd.utangtracker.presentation.person.PersonListScreen
import com.cedd.utangtracker.presentation.reservation.AddEditReservationScreen
import com.cedd.utangtracker.presentation.reservation.ReservationListScreen
import com.cedd.utangtracker.presentation.coachmark.CoachMarkOverlay
import com.cedd.utangtracker.presentation.coachmark.CoachMarkStep
import com.cedd.utangtracker.presentation.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Dashboard         : Screen("dashboard")
    data object DebtList          : Screen("debts")
    data object Persons           : Screen("persons")
    data object Settings          : Screen("settings")
    data object Reservations      : Screen("reservations")
    data object AddDebt           : Screen("add_debt?debtId={debtId}&personId={personId}") {
        fun withId(id: Long?) = if (id != null) "add_debt?debtId=$id" else "add_debt"
        fun forReloan(personId: Long) = "add_debt?personId=$personId"
    }
    data object DebtDetail        : Screen("debt_detail/{debtId}") {
        fun withId(id: Long) = "debt_detail/$id"
    }
    data object AddPerson         : Screen("add_person?personId={personId}") {
        fun withId(id: Long?) = if (id != null) "add_person?personId=$id" else "add_person"
    }
    data object Contract          : Screen("contract/{debtId}") {
        fun withId(id: Long) = "contract/$id"
    }
    data object PersonDetail      : Screen("person_detail/{personId}") {
        fun withId(id: Long) = "person_detail/$id"
    }
    data object AddReservation    : Screen("add_reservation?reservationId={reservationId}&personId={personId}") {
        fun withId(id: Long?) = if (id != null) "add_reservation?reservationId=$id" else "add_reservation"
        fun forPerson(personId: Long) = "add_reservation?personId=$personId"
    }
}

private val bottomNavItems = listOf(
    Triple(Screen.Dashboard,    Icons.Default.Dashboard,  "Dashboard"),
    Triple(Screen.DebtList,     Icons.Default.List,       "Debts"),
    Triple(Screen.Reservations, Icons.Default.DateRange,  "Reservations"),
    Triple(Screen.Persons,      Icons.Default.People,     "Persons"),
    Triple(Screen.Settings,     Icons.Default.Settings,   "Settings"),
)

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    showTour: Boolean = false,
    onTourComplete: () -> Unit = {}
) {
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentDest = navBackStack?.destination
    val navCenters = remember { mutableStateMapOf<String, Offset>() }

    val showBottomBar = bottomNavItems.any { (screen, _, _) ->
        currentDest?.hierarchy?.any { it.route == screen.route } == true
    }

    val tourSteps by remember {
        derivedStateOf {
            listOf(
                CoachMarkStep(
                    title = "Welcome to LoanTrack!",
                    message = "Let me give you a quick tour of the app so you can get started right away.",
                    spotlightCenter = null
                ),
                CoachMarkStep(
                    title = "Dashboard",
                    message = "Your home screen — see your net balance, debt overview chart, and recent records at a glance.",
                    spotlightCenter = navCenters[Screen.Dashboard.route],
                    spotlightRadius = 100f
                ),
                CoachMarkStep(
                    title = "Debts",
                    message = "View and manage all your debt records. Filter by status, search by person, and track payments.",
                    spotlightCenter = navCenters[Screen.DebtList.route],
                    spotlightRadius = 100f
                ),
                CoachMarkStep(
                    title = "Reservations",
                    message = "Record loan requests in advance. When the borrower is ready, convert a reservation into an actual debt.",
                    spotlightCenter = navCenters[Screen.Reservations.route],
                    spotlightRadius = 100f
                ),
                CoachMarkStep(
                    title = "Persons",
                    message = "Manage your contacts — see each person's total balance and full debt history in one place.",
                    spotlightCenter = navCenters[Screen.Persons.route],
                    spotlightRadius = 100f
                ),
                CoachMarkStep(
                    title = "Settings",
                    message = "Customize the app — dark mode, biometric lock, lender name, and your Anthropic API key for AI features.",
                    spotlightCenter = navCenters[Screen.Settings.route],
                    spotlightRadius = 100f
                ),
                CoachMarkStep(
                    title = "You're all set!",
                    message = "Tap \"Add Debt\" on the dashboard to record your first debt. You're all set!",
                    spotlightCenter = null
                )
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                FloatingNavBar(
                    items = bottomNavItems,
                    currentDest = currentDest,
                    onCenterCaptured = { route, offset -> navCenters[route] = offset },
                    onNavigate = { route ->
                        if (route == Screen.Dashboard.route) {
                            navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                        } else {
                            navController.navigate(route) {
                                popUpTo(Screen.Dashboard.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onAddDebt         = { navController.navigate(Screen.AddDebt.withId(null)) },
                    onNavigatePersons = { navController.navigate(Screen.Persons.route) },
                    onNavigateDebts   = { navController.navigate(Screen.DebtList.route) },
                    onDebtClick       = { id -> navController.navigate(Screen.DebtDetail.withId(id)) }
                )
            }
            composable(Screen.DebtList.route) {
                DebtListScreen(
                    onDebtClick = { id -> navController.navigate(Screen.DebtDetail.withId(id)) },
                    onAddDebt   = { navController.navigate(Screen.AddDebt.withId(null)) }
                )
            }
            composable(Screen.Persons.route) {
                PersonListScreen(
                    onAddPerson   = { navController.navigate(Screen.AddPerson.withId(null)) },
                    onEditPerson  = { id -> navController.navigate(Screen.AddPerson.withId(id)) },
                    onViewPerson  = { id -> navController.navigate(Screen.PersonDetail.withId(id)) }
                )
            }
            composable(Screen.Settings.route) { SettingsScreen() }

            composable(Screen.Reservations.route) {
                ReservationListScreen(
                    onAddReservation = { navController.navigate(Screen.AddReservation.withId(null)) },
                    onEditReservation = { id -> navController.navigate(Screen.AddReservation.withId(id)) },
                    onDebtCreated = { debtId -> navController.navigate(Screen.DebtDetail.withId(debtId)) }
                )
            }

            composable(
                route = Screen.AddReservation.route,
                arguments = listOf(
                    navArgument("reservationId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("personId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) {
                AddEditReservationScreen(onDone = { navController.popBackStack() })
            }

            composable(
                route = Screen.AddDebt.route,
                arguments = listOf(
                    navArgument("debtId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("personId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) {
                AddEditDebtScreen(
                    onDone = { navController.popBackStack() },
                    onAddPerson = { navController.navigate(Screen.AddPerson.withId(null)) }
                )
            }

            composable(
                route = Screen.DebtDetail.route,
                arguments = listOf(navArgument("debtId") { type = NavType.LongType })
            ) {
                DebtDetailScreen(
                    onBack     = { navController.popBackStack() },
                    onEdit     = { id -> navController.navigate(Screen.AddDebt.withId(id)) },
                    onContract = { id -> navController.navigate(Screen.Contract.withId(id)) },
                    onReloan   = { personId -> navController.navigate(Screen.AddDebt.forReloan(personId)) }
                )
            }

            composable(
                route = Screen.AddPerson.route,
                arguments = listOf(navArgument("personId") { type = NavType.LongType; defaultValue = -1L })
            ) {
                AddEditPersonScreen(onDone = { navController.popBackStack() })
            }

            composable(
                route = Screen.PersonDetail.route,
                arguments = listOf(navArgument("personId") { type = NavType.LongType })
            ) {
                PersonDetailScreen(
                    onBack       = { navController.popBackStack() },
                    onDebtClick  = { id -> navController.navigate(Screen.DebtDetail.withId(id)) },
                    onEditPerson = { id -> navController.navigate(Screen.AddPerson.withId(id)) }
                )
            }

            composable(
                route = Screen.Contract.route,
                arguments = listOf(navArgument("debtId") { type = NavType.LongType })
            ) {
                ContractScreen(onBack = { navController.popBackStack() })
            }
        }
    }

    if (showTour) {
        CoachMarkOverlay(steps = tourSteps, onFinish = onTourComplete)
    }
    } // end outer Box
}

// ── Floating Pill Navigation Bar ───────────────────────────────────────────────

@Composable
private fun FloatingNavBar(
    items: List<Triple<Screen, ImageVector, String>>,
    currentDest: NavDestination?,
    onCenterCaptured: (String, Offset) -> Unit = { _, _ -> },
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 20.dp, top = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 20.dp, shape = RoundedCornerShape(32.dp))
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { (screen, icon, label) ->
                    val selected = currentDest?.hierarchy?.any { it.route == screen.route } == true
                    NavPillItem(
                        icon = icon,
                        label = label,
                        selected = selected,
                        onClick = { onNavigate(screen.route) },
                        onCenterCaptured = { offset -> onCenterCaptured(screen.route, offset) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavPillItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onCenterCaptured: (Offset) -> Unit = {}
) {
    val selectedBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF4361EE), Color(0xFF7B2FBE))
    )
    val iconTint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .then(
                if (selected) Modifier.background(selectedBrush)
                else Modifier.background(Color.Transparent)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .onGloballyPositioned { coords ->
                val pos = coords.positionInWindow()
                val size = coords.size
                onCenterCaptured(Offset(pos.x + size.width / 2f, pos.y + size.height / 2f))
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
    }
}
