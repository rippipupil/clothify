package com.rippipupil.clothify.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rippipupil.clothify.AppContainer
import com.rippipupil.clothify.ui.basket.BasketScreen
import com.rippipupil.clothify.ui.discover.DiscoverScreen
import com.rippipupil.clothify.ui.onboarding.OnboardingScreen
import com.rippipupil.clothify.ui.profile.ProfileScreen
import com.rippipupil.clothify.ui.search.SearchScreen

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    DISCOVER("discover", "Descubrir", Icons.Filled.Style),
    SEARCH("search", "Buscar", Icons.Filled.PhotoCamera),
    BASKET("basket", "Cesta", Icons.Filled.ShoppingCart),
    PROFILE("profile", "Perfil", Icons.Filled.Person),
}

@Composable
fun ClothifyRoot(container: AppContainer) {
    val settings by container.settings.settings.collectAsState(initial = null)
    val current = settings ?: return Box(Modifier.fillMaxSize())
    if (!current.onboardingDone) {
        OnboardingScreen(container)
        return
    }
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = route == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Tab.DISCOVER.route, modifier = Modifier.padding(padding)) {
            composable(Tab.DISCOVER.route) { DiscoverScreen(container) }
            composable(Tab.SEARCH.route) { SearchScreen(container) }
            composable(Tab.BASKET.route) { BasketScreen(container) }
            composable(Tab.PROFILE.route) { ProfileScreen(container) }
        }
    }
}
