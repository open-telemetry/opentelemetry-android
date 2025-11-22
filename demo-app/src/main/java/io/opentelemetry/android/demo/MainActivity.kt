/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.opentelemetry.android.demo.about.AboutActivity
import io.opentelemetry.android.demo.theme.DemoAppTheme
import io.opentelemetry.android.demo.shop.ui.AstronomyShopActivity

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<DemoViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DemoAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Row(
                            Modifier.padding(all = 20.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CenterText(
                                fontSize = 40.sp,
                                text =
                                    buildAnnotatedString {
                                        withStyle(style = SpanStyle(color = Color(0xFFF5A800))) {
                                            append("Open")
                                        }
                                        withStyle(style = SpanStyle(color = Color(0xFF425CC7))) {
                                            append("Telemetry")
                                        }
                                        withStyle(style = SpanStyle(color = Color.Black)) {
                                            append(" Android Demo")
                                        }
                                        toAnnotatedString()
                                    },
                            )
                        }
                        // Displays the current RUM session ID. To observe session transitions:
                        // 1. Modify OtelDemoApplication.kt to reduce session timeout values
                        //    (see comments in the configuration block)
                        // 2. Put the app in background for the configured timeout period
                        // 3. Return to the app and observe the session ID change
                        SessionId(viewModel.sessionIdState)
                        MainOtelButton(
                            painterResource(id = R.drawable.otel_icon),
                        )
                        val context = LocalContext.current
                        LauncherButton(text = "Go shopping", onClick = {
                            context.startActivity(Intent(this@MainActivity, AstronomyShopActivity::class.java))
                        })
                        LauncherButton(text = "Learn more", onClick = {
                            context.startActivity(Intent(this@MainActivity, AboutActivity::class.java))
                        })

                    }
                }
                Log.d(TAG, "Main Activity started ")
            }
        }
        viewModel.sessionIdState.value = OtelDemoApplication.rum?.getRumSessionId() ?: error("Session ID is null")

        // Request the correct phone state permission based on API level
        // This permission is needed for gathering certain network information like
        // carrier name and network subtype (LTE, 4G) on certain API levels.
        val phoneStatePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_BASIC_PHONE_STATE
        } else {
            Manifest.permission.READ_PHONE_STATE
        }

        if (ContextCompat.checkSelfPermission(this, phoneStatePermission)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(phoneStatePermission),
                100
            )
        }
    }
}
