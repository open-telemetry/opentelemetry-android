/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo.ui.shop

import android.app.Activity
import android.content.Intent
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.opentelemetry.android.demo.MainActivity
import io.opentelemetry.android.demo.clients.ProductCatalogClient
import io.opentelemetry.android.demo.theme.DemoAppTheme
import io.opentelemetry.android.demo.ui.shop.cart.CartScreen
import io.opentelemetry.android.demo.ui.shop.products.ProductDetails
import io.opentelemetry.android.demo.ui.shop.products.ProductList


class AstronomyShopActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AstronomyShopScreen()
        }
    }
}

@Composable
fun AstronomyShopScreen(){
    val productsClient = ProductCatalogClient(LocalContext.current)
    val products by remember { mutableStateOf(productsClient.get()) }

    DemoAppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val astronomyShopNavController = rememberAstronomyShopNavController()
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(navController = astronomyShopNavController.navController,
                        onExitClicked = {
//                          TODO
                        }
                    )

                }
            ) { innerPadding ->
                NavHost(
                    navController = astronomyShopNavController.navController,
                    startDestination = "prod-list",
                    Modifier.padding(innerPadding) // Ensure proper padding with Scaffold
                ) {
                    composable("prod-list") {
                        ProductList(
                            products = products
                        )
                    }
                    composable(BottomNavItem.List.route) {
                        ProductList(
                            products = products
                        )
                    }
                    composable(BottomNavItem.Cart.route) {
                        CartScreen()
                    }
                }
            }
        }
    }
}



