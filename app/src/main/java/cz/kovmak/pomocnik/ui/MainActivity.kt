package cz.kovmak.pomocnik.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cz.kovmak.pomocnik.ui.screens.HomeScreen
import cz.kovmak.pomocnik.ui.screens.HistoryScreen
import cz.kovmak.pomocnik.ui.screens.SettingsScreen
import cz.kovmak.pomocnik.ui.screens.ShiftScheduleScreen
import cz.kovmak.pomocnik.ui.theme.PomocnikTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.kovmak.pomocnik.viewmodel.WorkViewModel
import cz.kovmak.pomocnik.data.database.WorkEntry

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PomocnikTheme {
                PomocnikApp()
            }
        }
    }
}

@Composable
fun PomocnikApp() {
    var selectedTab by remember { mutableStateOf(0) }
    val workViewModel: WorkViewModel = viewModel()

    Scaffold(
        containerColor = Color(0xFF080B12),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF111827),
                contentColor = Color(0xFFE8E8E8),
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Mic, contentDescription = null) },
                    label = { Text("Новий") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFFB000),
                        selectedTextColor = Color(0xFFFFB000),
                        indicatorColor = Color(0x22FFB000),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.History, contentDescription = null) },
                    label = { Text("Історія") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFFB000),
                        selectedTextColor = Color(0xFFFFB000),
                        indicatorColor = Color(0x22FFB000),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                    label = { Text("Графік") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFFB000),
                        selectedTextColor = Color(0xFFFFB000),
                        indicatorColor = Color(0x22FFB000),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Налашт.") },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFFB000),
                        selectedTextColor = Color(0xFFFFB000),
                        indicatorColor = Color(0x22FFB000),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF080B12), Color(0xFF0F172A), Color(0xFF080B12))
                    )
                )
        ) {
            when (selectedTab) {
                0 -> HomeScreen(viewModel = workViewModel)
                1 -> HistoryScreen(onRepeatEntry = { entry ->
                    // Pre-fill WorkViewModel and switch to Home tab
                    workViewModel.updateDescriptionUa(entry.descriptionUa)
                    workViewModel.updateOrderId(entry.orderId)
                    workViewModel.updateWorkType(entry.workType)
                    workViewModel.updateMaterials(entry.materials)
                    workViewModel.updateStartTime(entry.startTime)
                    workViewModel.updateEndTime(entry.endTime)
                    selectedTab = 0
                })
                2 -> ShiftScheduleScreen()
                3 -> SettingsScreen()
            }
        }
    }
}
