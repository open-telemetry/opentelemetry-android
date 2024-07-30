package io.opentelemetry.android.demo.ui.shop

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Exit : BottomNavItem("home", Icons.AutoMirrored.Filled.ExitToApp, "Exit")
    object List : BottomNavItem("prod-list", Icons.AutoMirrored.Filled.List, "List")
    object Cart : BottomNavItem("cart", Icons.Filled.ShoppingCart, "Cart")
}

object MainDestinations {
    const val HOME_ROUTE = "prod-list"
    const val PRODUCT_DETAIL_ROUTE = "product"
    const val PRODUCT_ID_KEY = "productId"
}

@Composable
fun rememberAstronomyShopNavController(navController: NavHostController = rememberNavController())
: AstronomyShopNavController = remember(navController)
{
    AstronomyShopNavController(navController)
}

@Stable
class AstronomyShopNavController(
    val navController: NavHostController,
) {
    val currentRoute: String?
        get() = navController.currentDestination?.route

    fun upPress() {
        navController.navigateUp()
    }

    fun navigateToProductDetail(productId: String) {
        navController.navigate("${MainDestinations.PRODUCT_DETAIL_ROUTE}/$productId")
    }

//    fun navigateToProductDetail(productId: String, from: NavBackStackEntry) {
//        // In order to discard duplicated navigation events, we check the Lifecycle
//        if (from.lifecycleIsResumed()) {
//            navController.navigate("${MainDestinations.PRODUCT_DETAIL_ROUTE}/$productId")
//        }
//    }
}

//some stuff copied from android samples
//
///**
// * If the lifecycle is not resumed it means this NavBackStackEntry already processed a nav event.
// *
// * This is used to de-duplicate navigation events.
// */
//private fun NavBackStackEntry.lifecycleIsResumed() =
//    this.lifecycle.currentState == Lifecycle.State.RESUMED
//
//private val NavGraph.startDestination: NavDestination?
//    get() = findNode(startDestinationId)
//
///**
// * Copied from similar function in NavigationUI.kt
// *
// * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:navigation/navigation-ui/src/main/java/androidx/navigation/ui/NavigationUI.kt
// */
//private tailrec fun findStartDestination(graph: NavDestination): NavDestination {
//    return if (graph is NavGraph) findStartDestination(graph.startDestination!!) else graph
//}

@Composable
fun BottomNavigationBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClicked: (String) -> Unit,
    onExitClicked: () -> Unit
) {
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (item.route == BottomNavItem.Exit.route) {
                        onExitClicked()
                    } else {
                        onItemClicked(item.route)
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}

