package com.example.mobiledev.ui.customer

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mobiledev.R

private data class CustomerTab(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
)

private val customerTabs = listOf(
    CustomerTab("discover", R.string.tab_discover, Icons.Default.Home),
    CustomerTab("map", R.string.tab_map, Icons.Default.Place),
    CustomerTab("orders", R.string.tab_orders, Icons.Default.ShoppingCart),
    CustomerTab("favorites", R.string.tab_favorites, Icons.Default.Favorite)
)

/** Startscherm van de klant: bottom navigation met 4 tabbladen. */
@Composable
fun CustomerHome(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                customerTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "discover",
            modifier = Modifier.padding(padding)
        ) {
            composable("discover") { DiscoverScreen(onLogout = onLogout) }
            composable("map") { ComingSoon() }        // week 5
            composable("orders") { ComingSoon() }     // week 6
            composable("favorites") { ComingSoon() }  // week 6
        }
    }
}

@Composable
private fun ComingSoon() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.coming_soon))
    }
}