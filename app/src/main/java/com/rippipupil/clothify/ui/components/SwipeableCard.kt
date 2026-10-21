package com.rippipupil.clothify.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch

/** Arrastrar a la derecha = me gusta; a la izquierda = menos de esto (umbral: 30 % del ancho). */
@Composable
fun SwipeableCard(
    key: Any,
    onSwiped: (liked: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier) {
        val width = with(LocalDensity.current) { maxWidth.toPx() }
        val offset = remember(key) { Animatable(0f) }
        val scope = rememberCoroutineScope()
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = offset.value
                    rotationZ = offset.value / 20f
                }
                .pointerInput(key) {
                    detectDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val liked = offset.value > 0
                                if (kotlin.math.abs(offset.value) > width * THRESHOLD) {
                                    offset.animateTo(if (liked) width * 1.5f else -width * 1.5f)
                                    onSwiped(liked)
                                } else {
                                    offset.animateTo(0f)
                                }
                            }
                        },
                        onDragCancel = { scope.launch { offset.animateTo(0f) } },
                        onDrag = { change, drag ->
                            change.consume()
                            scope.launch { offset.snapTo(offset.value + drag.x) }
                        },
                    )
                },
        ) { content() }
    }
}

private const val THRESHOLD = 0.3f
