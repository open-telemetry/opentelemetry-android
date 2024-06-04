/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SessionId(sessionId: StateFlow<String>) {
    Row {
        Card(modifier = Modifier.size(width = 295.dp, height = 75.dp)) {
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 25.dp),
                Arrangement.Center,
            ) {
                CenterText(text = "session.id", fontSize = 14.sp)
                //TODO: Fix me -- this selection doesn't work
                SelectionContainer {
                    CenterText(text = sessionId.collectAsState().value, fontSize = 14.sp, selectable = true)
                }
            }
        }
    }
}
