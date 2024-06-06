/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

val gothamFont =
    FontFamily(
        Font(R.font.gotham_bold, FontWeight.Bold),
    )

@Composable
fun CenterText(
    text: AnnotatedString,
    fontSize: TextUnit = 12.sp,
) {
    Text(
        text,
        textAlign = TextAlign.Center,
        fontSize = fontSize,
        modifier = Modifier.fillMaxWidth(),
        fontFamily = gothamFont,
        style = TextStyle.Default.copy(textAlign = TextAlign.Center),
    )
}

@Composable
fun CenterText(
    text: String,
    fontSize: TextUnit = 12.sp,
    selectable: Boolean = false,
    color: Color = Color.Unspecified
) {
    val textComposable = Text(
        text,
        color = color,
        textAlign = TextAlign.Center,
        fontSize = fontSize,
        modifier = Modifier.fillMaxWidth(),
        fontFamily = gothamFont,
        style = TextStyle.Default.copy(textAlign = TextAlign.Center),
    )
    if (selectable) {
        return SelectionContainer {
            textComposable
        }
    }
    return textComposable
}
