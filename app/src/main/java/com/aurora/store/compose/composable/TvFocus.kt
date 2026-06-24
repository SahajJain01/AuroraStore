/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.composable

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
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
    enabled: Boolean = true
): Modifier {
    if (!enabled || LocalUI.current != UI.TV) return this

    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) focusedScale else 1f,
        label = "tvFocusScale"
    )
    val elevation by animateFloatAsState(
        targetValue = if (focused) 18f else 0f,
        label = "tvFocusElevation"
    )

    return this
        .onFocusChanged { focused = it.isFocused || it.hasFocus }
        .zIndex(if (focused) 1f else 0f)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            shadowElevation = elevation.dp.toPx()
            this.shape = shape
            clip = false
        }
}
