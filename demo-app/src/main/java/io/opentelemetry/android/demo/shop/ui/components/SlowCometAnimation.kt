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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.SecureRandom

@Composable
fun SlowCometAnimation(modifier: Modifier = Modifier) {
    val cometParams = listOf(
        CometParams(startX = 0f, startY = 0f, targetX = 1.3f, targetY = 1.4f),
        CometParams(startX = 0.2f, startY = 0f, targetX = 1.4f, targetY = 0.9f),
        CometParams(startX = 0.2f, startY = -0.1f, targetX = 1.5f, targetY = 0.8f),
        CometParams(startX = 0f, startY = 0.6f, targetX = 1.2f, targetY = 1.7f),
        CometParams(startX = -0.2f, startY = 0.1f, targetX = 1.4f, targetY = 1.3f),
        CometParams(startX = 0.5f, startY = 0f, targetX = 1.7f, targetY = 1.3f),
        CometParams(startX = 0f, startY = 0f, targetX = 1.3f, targetY = 1.4f),
        CometParams(startX = 0.2f, startY = 0f, targetX = 1.4f, targetY = 1.3f),
        CometParams(startX = 0f, startY = 0f, targetX = 1.4f, targetY = 1.3f),
        CometParams(startX = 0.2f, startY = -0.1f, targetX = 1.5f, targetY = 0.7f),
        CometParams(startX = 0f, startY = 0.6f, targetX = 1.2f, targetY = 1.7f),
        CometParams(startX = -0.2f, startY = 0.1f, targetX = 1.3f, targetY = 1.4f),
    )

    val cometVisibility = remember { mutableStateListOf<Boolean>().apply { addAll(List(cometParams.size) { false }) } }

    LaunchedEffect(Unit) {
        cometParams.forEachIndexed { index, _ ->
            launch {
                delay(index * 500L)
                cometVisibility[index] = true
            }
        }
    }

    cometParams.forEachIndexed { index, params ->
        if (cometVisibility[index]) {
            SlowSingleCometAnimation(
                startX = params.startX,
                startY = params.startY,
                targetX = params.targetX,
                targetY = params.targetY,
                modifier = modifier
            )
        }
    }
}

@Composable
fun SlowSingleCometAnimation(
    startX: Float,
    startY: Float,
    targetX: Float,
    targetY: Float,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    val tailPositions = remember { mutableStateListOf<Offset>() }

    val animatedX = remember { Animatable(startX) }
    val animatedY = remember { Animatable(startY) }

    LaunchedEffect(Unit) {
        scope.launch {
            launch {
                delay(500)
                animatedX.animateTo(
                    targetValue = targetX,
                    animationSpec = tween(durationMillis = 3000, easing = LinearEasing)
                )
            }
            launch {
                delay(500)
                animatedY.animateTo(
                    targetValue = targetY,
                    animationSpec = tween(durationMillis = 3000, easing = LinearEasing)
                )
            }
        }
    }

    LaunchedEffect(animatedX.value, animatedY.value) {
        val currentPosition = Offset(animatedX.value, animatedY.value)
        tailPositions.add(currentPosition)

        if (tailPositions.size > 30) {
            tailPositions.removeAt(0)
        }
    }

    val random = SecureRandom()
    repeat(10_000) {
        random.nextFloat()
    }

    ExpensiveCometShape(
        modifier = modifier,
        headX = animatedX.value,
        headY = animatedY.value,
        tailPositions = tailPositions
    )
}

@Composable
fun ExpensiveCometShape(
    modifier: Modifier = Modifier,
    headX: Float = 0.8f,
    headY: Float = 0.3f,
    tailPositions: List<Offset> = emptyList()
) {
    Canvas(modifier = modifier) {
        val headRadius = size.minDimension * 0.05f
        val animatedHeadX = size.width * headX
        val animatedHeadY = size.height * headY


        for (i in tailPositions.indices) {
            val position = tailPositions[i]
            val posX = size.width * position.x
            val posY = size.height * position.y

            val progress = i / tailPositions.size.toFloat()
            val tailRadius = headRadius * (1 - progress)
            val tailAlpha = 0.3f * (1 - progress)

            repeat(100) {
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = tailAlpha),
                    radius = tailRadius,
                    center = Offset(posX, posY)
                )
            }
        }

        repeat(100) {
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
}


@Preview(showBackground = true)
@Composable
fun PreviewCometAnimation() {
    SlowCometAnimation(
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