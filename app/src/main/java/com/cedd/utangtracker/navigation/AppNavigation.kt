package com.cedd.utangtracker.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.cedd.utangtracker.presentation.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Dashboard  : Screen("dashboard")
    data object DebtList   : Screen("debts")
    data object Persons    : Screen("persons")
    data object Settings   : Screen("settings")
    data object AddDebt    : Screen("add_debt?debtId={debtId}&personId={personId}") {
        fun withId(id: Long?) = if (id != null) "add_debt?debtId=$id" else "add_debt"
        fun forReloan(personId: Long) = "add_debt?personId=$personId"
    }
    data object DebtDetail : Screen("debt_detail/{debtId}") {
        fun withId(id: Long) = "debt_detail/$id"
    }
    data object AddPerson  : Screen("add_person?personId={personId}") {
        fun withId(id: Long?) = if (id != null) "add_person?personId=$id" else "add_person"
    }
    data object Contract      : Screen("contract/{debtId}") {
        fun withId(id: Long) = "contract/$id"
    }
    data object PersonDetail  : Screen("person_detail/{personId}") {
        fun withId(id: Long) = "person_detail/$id"
    }
}

private val bottomNavItems = listOf(
    Triple(Screen.Dashboard, Icons.Default.Dashboard, "Dashboard"),
    Triple(Screen.DebtList,  Icons.Default.List,      "Debts"),
    Triple(Screen.Persons,   Icons.Default.People,    "Persons"),
    Triple(Screen.Settings,  Icons.Default.Settings,  "Settings"),
)

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentDest = navBackStack?.destination

    val showBottomBar = bottomNavItems.any { (screen, _, _) ->
        currentDest?.hierarchy?.any { it.route == screen.route } == true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                FloatingNavBar(
                    items = bottomNavItems,
                    currentDest = currentDest,
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
}

// ── Floating Pill Navigation Bar ───────────────────────────────────────────────

@Composable
private fun FloatingNavBar(
    items: List<Triple<Screen, ImageVector, String>>,
    currentDest: NavDestination?,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 20.dp, top = 4.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 20.dp, shape = RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 0.dp
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
                        onClick = { onNavigate(screen.route) }
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
    onClick: () -> Unit
) {
    val bgColor   = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val iconTint  = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
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
