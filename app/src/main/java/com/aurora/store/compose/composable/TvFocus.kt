/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.aurora.store.compose.composition.LocalUI
import com.aurora.store.compose.composition.UI

@Composable
fun Modifier.tvFocusRing(
    shape: Shape = RoundedCornerShape(12.dp),
    focusedScale: Float = 1f,
    enabled: Boolean = true,
    normalContainerColor: Color = Color.Transparent,
    focusedContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest
): Modifier {
    if (!enabled || LocalUI.current != UI.TV) return this

    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) focusedScale else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "tvFocusScale"
    )
    val containerColor by animateColorAsState(
        targetValue = if (focused) focusedContainerColor else normalContainerColor,
        animationSpec = tween(durationMillis = 90),
        label = "tvFocusColor"
    )

    return this
        .onFocusChanged { focused = it.isFocused || it.hasFocus }
        .zIndex(if (focused) 1f else 0f)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.shape = shape
            clip = false
        }
        .clip(shape)
        .background(containerColor)
}
