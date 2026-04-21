/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private val DemoAccentBlue = Color(0xFF425CC7)

/**
 * Full-screen OkHttp instrumentation demo (issue #419). Sample HTTP calls produce traced client spans.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OkHttpDemoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { OkHttpClient() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Instrumentation testing") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "OkHttp sample calls (GET, POST, delayed GET); each should produce a traced client span in your collector.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OkHttpDemoActionButton(
                onClick = {
                    scope.launch {
                        val msg =
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    val request =
                                        Request.Builder().url("https://httpbin.org/status/200").get().build()
                                    val code = client.newCall(request).execute().use { it.code }
                                    "OkHttp: HTTP $code"
                                }
                            }.getOrElse { e -> "OkHttp 200 demo: ${e.message}" }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                text = "GET known 200",
            )
            OkHttpDemoActionButton(
                onClick = {
                    scope.launch {
                        val msg =
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    val request =
                                        Request.Builder().url("https://httpbin.org/status/404").get().build()
                                    val code = client.newCall(request).execute().use { it.code }
                                    "OkHttp: HTTP $code"
                                }
                            }.getOrElse { e -> "OkHttp 404 demo: ${e.message}" }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                text = "GET known 404",
            )
            OkHttpDemoActionButton(
                onClick = {
                    scope.launch {
                        val msg =
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    val request =
                                        Request.Builder().url("http://127.0.0.1:1/").get().build()
                                    client.newCall(request).execute().use { it.code }
                                    "OkHttp: unexpected success"
                                }
                            }.getOrElse { e -> "OkHttp failure demo: ${e.javaClass.simpleName}" }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                text = "GET connection failure",
            )
            OkHttpDemoActionButton(
                onClick = {
                    scope.launch {
                        val msg =
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    val json = """{"event":"otel-android-demo","source":"okhttp"}"""
                                    val body =
                                        json.toRequestBody("application/json; charset=utf-8".toMediaType())
                                    val request =
                                        Request.Builder().url("https://httpbin.org/post").post(body).build()
                                    val code = client.newCall(request).execute().use { it.code }
                                    "OkHttp POST: HTTP $code"
                                }
                            }.getOrElse { e -> "OkHttp POST demo: ${e.message}" }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                text = "POST JSON payload",
            )
            OkHttpDemoActionButton(
                onClick = {
                    scope.launch {
                        val msg =
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    val request =
                                        Request.Builder().url("https://httpbin.org/delay/3").get().build()
                                    val code = client.newCall(request).execute().use { it.code }
                                    "OkHttp slow GET: HTTP $code (~3s)"
                                }
                            }.getOrElse { e -> "OkHttp slow demo: ${e.message}" }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                text = "GET slow response (3s)",
            )
        }
    }
}

@Composable
private fun OkHttpDemoActionButton(
    text: String,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        border = BorderStroke(1.dp, DemoAccentBlue),
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = DemoAccentBlue,
            ),
    ) {
        Text(text)
    }
}
