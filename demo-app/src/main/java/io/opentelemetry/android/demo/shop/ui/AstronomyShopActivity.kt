/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo.shop.ui

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.opentelemetry.android.demo.OtelDemoApplication
import io.opentelemetry.android.demo.shop.clients.ProductCatalogClient
import io.opentelemetry.android.demo.shop.ui.cart.CartScreen
import io.opentelemetry.android.demo.shop.ui.cart.CartViewModel
import io.opentelemetry.android.demo.shop.ui.cart.CheckoutConfirmationScreen
import io.opentelemetry.android.demo.shop.ui.cart.CheckoutInfoViewModel
import io.opentelemetry.android.demo.shop.ui.cart.InfoScreen
import io.opentelemetry.android.demo.shop.ui.products.ProductDetails
import io.opentelemetry.android.demo.shop.ui.products.ProductList
import io.opentelemetry.android.demo.theme.DemoAppTheme
import io.opentelemetry.api.common.AttributeKey.doubleKey
import io.opentelemetry.api.common.AttributeKey.stringKey

class AstronomyShopActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AstronomyShopScreen()
        }
    }
}

@Composable
fun AstronomyShopScreen() {
    val productsClient = ProductCatalogClient(LocalContext.current)
    val products by remember { mutableStateOf(productsClient.get()) }
    val context = LocalContext.current
    val astronomyShopNavController = rememberAstronomyShopNavController()
    val cartViewModel: CartViewModel = viewModel()
    val checkoutInfoViewModel: CheckoutInfoViewModel = viewModel()

    DemoAppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(
                        items = listOf(BottomNavItem.Exit, BottomNavItem.List, BottomNavItem.Cart),
                        currentRoute = astronomyShopNavController.currentRoute,
                        onItemClicked = { route ->
                            astronomyShopNavController.navController.navigate(route) {
                                popUpTo(astronomyShopNavController.navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        },
                        onExitClicked = {
                            (context as? Activity)?.finish()
                        }
                    )
                }
            ) { innerPadding ->
                NavHost(
                    navController = astronomyShopNavController.navController,
                    startDestination = MainDestinations.HOME_ROUTE,
                    Modifier.padding(innerPadding)
                ) {
                    composable(BottomNavItem.List.route) {
                        ProductList(products = products) { productId ->
                            astronomyShopNavController.navigateToProductDetail(productId)
                        }
                    }
                    composable(BottomNavItem.Cart.route) {
                        CartScreen(cartViewModel = cartViewModel, onCheckoutClick = {astronomyShopNavController.navigateToCheckoutInfo()},  onProductClick = { productId ->
                            astronomyShopNavController.navigateToProductDetail(productId)
                        })
                    }
                    composable("${MainDestinations.PRODUCT_DETAIL_ROUTE}/{${MainDestinations.PRODUCT_ID_KEY}}") { backStackEntry ->
                        val productId = backStackEntry.arguments?.getString(MainDestinations.PRODUCT_ID_KEY)
                        val product = products.find { it.id == productId }
                        product?.let { ProductDetails(
                            product = it,
                            cartViewModel,
                            upPress = {astronomyShopNavController.upPress()},
                            onProductClick = { productId ->
                                astronomyShopNavController.navigateToProductDetail(productId)
                            }
                        )
                        }
                    }
                    composable(MainDestinations.CHECKOUT_INFO_ROUTE) {
                        InfoScreen(
                            onPlaceOrderClick = {instrumentedPlaceOrder(
                                astronomyShopNavController = astronomyShopNavController,
                                cartViewModel = cartViewModel,
                                checkoutInfoViewModel = checkoutInfoViewModel
                            )},
                            upPress = {astronomyShopNavController.upPress()},
                            checkoutInfoViewModel = checkoutInfoViewModel
                        )
                    }
                    composable(MainDestinations.CHECKOUT_CONFIRMATION_ROUTE){
                        CheckoutConfirmationScreen(
                            cartViewModel = cartViewModel,
                            checkoutInfoViewModel = checkoutInfoViewModel
                        )
                    }
                }
            }
        }
    }
}

private fun instrumentedPlaceOrder(
    astronomyShopNavController: InstrumentedAstronomyShopNavController,
    cartViewModel: CartViewModel,
    checkoutInfoViewModel: CheckoutInfoViewModel
){
    generateOrderPlacedEvent(cartViewModel, checkoutInfoViewModel)
    astronomyShopNavController.navigateToCheckoutConfirmation()
}

private fun generateOrderPlacedEvent(
    cartViewModel: CartViewModel,
    checkoutInfoViewModel: CheckoutInfoViewModel
) {
    val eventBuilder = OtelDemoApplication.eventBuilder("otel.demo.app", "order.placed")
    eventBuilder
        .setAttribute(doubleKey("order.total.value"), cartViewModel.getTotalPrice())
        .setAttribute(stringKey("buyer.state"), checkoutInfoViewModel.shippingInfo.state)
        .emit()
}

