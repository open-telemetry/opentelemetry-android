package io.opentelemetry.android.demo.shop.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

@Composable
fun CometAnimation(modifier: Modifier = Modifier) {
    val cometParams = listOf(
        CometParams(startX = 0f, startY = 1f, targetX = 1.2f, targetY = -0.2f),
        CometParams(startX = 1f, startY = 1f, targetX = -0.2f, targetY = -0.2f),
        CometParams(startX = 0.3f, startY = 1f, targetX = 2f, targetY = 0.2f)
    )

    for (params in cometParams) {
        SingleCometAnimation(
            startX = params.startX,
            startY = params.startY,
            targetX = params.targetX,
            targetY = params.targetY,
            modifier = modifier
        )
    }
}

@Composable
fun SingleCometAnimation(
    startX: Float,
    startY: Float,
    targetX: Float,
    targetY: Float,
    modifier: Modifier =  Modifier
) {
    val scope = rememberCoroutineScope()

    val animatedX = remember { Animatable(startX) }
    val animatedY = remember { Animatable(startY) }

    LaunchedEffect(Unit) {
        scope.launch {
            launch {
                animatedX.animateTo(
                    targetValue = targetX,
                    animationSpec = tween(durationMillis = 4000, easing = LinearEasing)
                )
            }
            launch {
                animatedY.animateTo(
                    targetValue = targetY,
                    animationSpec = tween(durationMillis = 4000, easing = LinearEasing)
                )
            }
        }
    }

    CometShape(
        modifier = modifier,
        headX = animatedX.value,
        headY = animatedY.value
    )
}

@Composable
fun CometShape(
    modifier: Modifier = Modifier,
    headX: Float = 0.8f,
    headY: Float = 0.3f
) {
    Canvas(modifier = modifier) {
        val headRadius = size.minDimension * 0.05f
        val animatedHeadX = size.width * headX
        val animatedHeadY = size.height * headY

        drawCircle(
            color = Color(0x80FFFF00),
            radius = headRadius * 2,
            center = Offset(animatedHeadX, animatedHeadY)
        )
        drawCircle(
            color = Color(0xAAFFA500),
            radius = headRadius * 1.5f,
            center = Offset(animatedHeadX, animatedHeadY)
        )
        drawCircle(
            color = Color(0xFFFFD700),
            radius = headRadius,
            center = Offset(animatedHeadX, animatedHeadY)
        )
        drawCircle(
            color = Color.White,
            radius = headRadius * 0.6f,
            center = Offset(animatedHeadX, animatedHeadY)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCometAnimation() {
    CometAnimation(
        Modifier
            .fillMaxSize()
            .zIndex(1f)
    )
}

data class CometParams(
    val startX: Float,
    val startY: Float,
    val targetX: Float,
    val targetY: Float
)
