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
import cz.kovmak.pomocnik.ui.theme.PomocnikTheme

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

    Scaffold(
        containerColor = Color(0xFF0A0E21),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF141832),
                contentColor = Color(0xFFE8E8E8),
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Mic, contentDescription = null) },
                    label = { Text("Новий") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFF6B35),
                        selectedTextColor = Color(0xFFFF6B35),
                        indicatorColor = Color(0x1AFF6B35),
                        unselectedIconColor = Color(0xFF8892B0),
                        unselectedTextColor = Color(0xFF8892B0)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.History, contentDescription = null) },
                    label = { Text("Історія") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFF6B35),
                        selectedTextColor = Color(0xFFFF6B35),
                        indicatorColor = Color(0x1AFF6B35),
                        unselectedIconColor = Color(0xFF8892B0),
                        unselectedTextColor = Color(0xFF8892B0)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Налашт.") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFF6B35),
                        selectedTextColor = Color(0xFFFF6B35),
                        indicatorColor = Color(0x1AFF6B35),
                        unselectedIconColor = Color(0xFF8892B0),
                        unselectedTextColor = Color(0xFF8892B0)
                    )
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> HomeScreen()
                1 -> HistoryScreen()
                2 -> SettingsScreen()
            }
        }
    }
}
