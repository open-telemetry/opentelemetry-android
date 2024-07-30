/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo.ui.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.opentelemetry.android.demo.clients.ProductCatalogClient
import io.opentelemetry.android.demo.theme.DemoAppTheme
import io.opentelemetry.android.demo.ui.shop.products.ProductDetails
import io.opentelemetry.android.demo.ui.shop.products.ProductList

class ShopFragment : Fragment() {
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?,
//    ): View {
//
//        val productsClient = ProductCatalogClient(requireContext())
//        val products = productsClient.get()
//        val productState = mutableStateOf(products)
//
//        return ComposeView(requireContext()).apply {
//            setContent {
//
//                val navController = rememberNavController()
//
//                DemoAppTheme {
//                    // A surface container using the 'background' color from the theme
//                    Surface(
//                        modifier = Modifier.fillMaxSize(),
//                        color = MaterialTheme.colorScheme.background,
//                    ) {
//                        NavHost(navController = navController, startDestination = "prod-list") {
//                            composable("prod-list") {
//                                ProductList(
//                                    products = productState,
//                                    navController = navController
//                                )
//                            }
//                            composable(
//                                "prod-details/{productId}",
//                                arguments = listOf(navArgument("productId") {
//                                    type = NavType.StringType
//                                })
//                            ) { backStackEntry ->
//                                val productId = backStackEntry.arguments?.getString("productId")
//                                val product = products.find { it.id == productId }
//                                product?.let {
//                                    ProductDetails(product = it)
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
}
