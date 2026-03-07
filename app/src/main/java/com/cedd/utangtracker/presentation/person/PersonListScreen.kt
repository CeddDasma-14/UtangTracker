package com.cedd.utangtracker.presentation.person

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cedd.utangtracker.data.local.entity.PersonEntity
import kotlin.math.abs

// Unique vivid color palette — one per person based on name hash
private val avatarPalette = listOf(
    Color(0xFF6C63FF), // purple
    Color(0xFF4ADE80), // green
    Color(0xFF4361EE), // blue
    Color(0xFFFFA726), // amber
    Color(0xFF26C6DA), // cyan
    Color(0xFFEC407A), // pink
    Color(0xFFFF7043), // deep orange
    Color(0xFF26A69A), // teal
    Color(0xFFAB47BC), // violet
    Color(0xFF66BB6A), // lime green
)

private fun avatarColorFor(name: String): Color =
    avatarPalette[abs(name.hashCode()) % avatarPalette.size]

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonListScreen(
    onAddPerson: () -> Unit,
    onEditPerson: (Long) -> Unit,
    onViewPerson: (Long) -> Unit = {},
    vm: PersonListViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Persons", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPerson,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, "Add Person")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // ── Search bar ───────────────────────────────────────────────────
            OutlinedTextField(
                value = state.query,
                onValueChange = { vm.setQuery(it) },
                placeholder = { Text("Search by name or phone…", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                trailingIcon = {
                    if (state.query.isNotBlank()) {
                        IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Default.Close, "Clear", Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            )

            if (state.persons.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👥", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (state.query.isBlank()) "Walang tao pa." else "Walang nahanap.",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (state.query.isBlank()) "Tap + para magdagdag ng tao." else "Try a different search.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.persons, key = { it.id }) { person ->
                        PersonRow(
                            person    = person,
                            onView    = { onViewPerson(person.id) },
                            onEdit    = { onEditPerson(person.id) },
                            onDelete  = { vm.deletePerson(person) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonRow(person: PersonEntity, onView: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    val initial     = person.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val avatarColor = avatarColorFor(person.name)

    // Left-to-right gradient tint using that person's unique color
    val cardGradient = Brush.horizontalGradient(
        colors = listOf(
            avatarColor.copy(alpha = 0.18f),
            avatarColor.copy(alpha = 0.04f),
            Color.Transparent
        )
    )

    Surface(
        onClick         = onView,
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(16.dp),
        color           = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        tonalElevation  = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardGradient)
        ) {
            Row(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Unique colored avatar with radial gradient background
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    listOf(
                                        avatarColor.copy(alpha = 0.35f),
                                        avatarColor.copy(alpha = 0.12f)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = initial,
                            color      = avatarColor,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 20.sp
                        )
                    }

                    Column {
                        Text(
                            person.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        person.phone?.let {
                            Text(
                                it,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showConfirm = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Delete ${person.name}?") },
            text  = { Text("All debts linked to this person will also be deleted.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showConfirm = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.tertiary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
