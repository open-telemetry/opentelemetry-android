package io.opentelemetry.android.demo.shop.ui

import android.os.Bundle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.*
import androidx.navigation.NavDestination
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.instrumentation.compose.navigation.rememberObservedNavController


sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    data object Exit : BottomNavItem("home", Icons.AutoMirrored.Filled.ExitToApp, "Exit")
    data object List : BottomNavItem("prod-list", Icons.AutoMirrored.Filled.List, "List")
    data object Cart : BottomNavItem("cart", Icons.Filled.ShoppingCart, "Cart")
}

object MainDestinations {
    const val HOME_ROUTE = "prod-list"
    const val PRODUCT_DETAIL_ROUTE = "product"
    const val PRODUCT_ID_KEY = "productId"
    const val PRODUCT_DETAIL_ROUTE_PATTERN = "$PRODUCT_DETAIL_ROUTE/{$PRODUCT_ID_KEY}"
    const val CHECKOUT_INFO_ROUTE = "checkout-info"
    const val CHECKOUT_CONFIRMATION_ROUTE = "checkout-confirmation"
}

@Composable
fun rememberAstronomyShopNavController(
    navController: NavHostController = rememberAstronomyShopHostController(),
): AstronomyShopNavController = remember(navController) {
    AstronomyShopNavController(navController)
}

@Composable
private fun rememberAstronomyShopHostController(): NavHostController {
    val rum = OtelDemoApplication.rum
    return if (rum != null) {
        rememberObservedNavController(rum = rum, screenName = ::astronomyShopScreenName)
    } else {
        rememberNavController()
    }
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

    fun navigateToCheckoutInfo(){
        navController.navigate(MainDestinations.CHECKOUT_INFO_ROUTE)
    }

    fun navigateToCheckoutConfirmation(){
        navController.navigate(MainDestinations.CHECKOUT_CONFIRMATION_ROUTE)
    }
}

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

// Maps the shop's route patterns to display names. Product details reads the product id out of
// `arguments` so the screen name identifies which product was viewed
private fun astronomyShopScreenName(destination: NavDestination, arguments: Bundle?): String =
    when (destination.route) {
        MainDestinations.PRODUCT_DETAIL_ROUTE_PATTERN ->
            arguments?.getString(MainDestinations.PRODUCT_ID_KEY)
                ?.let { "Product Details: $it" }
                ?: "Product Details"
        MainDestinations.HOME_ROUTE -> "Product List"
        BottomNavItem.Cart.route -> "Cart"
        MainDestinations.CHECKOUT_INFO_ROUTE -> "Checkout Info"
        MainDestinations.CHECKOUT_CONFIRMATION_ROUTE -> "Checkout Confirmation"
        else -> destination.route ?: "unknown"
    }
