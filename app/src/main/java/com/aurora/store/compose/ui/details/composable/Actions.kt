/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import com.aurora.extensions.isWindowCompact
import com.aurora.store.R
import com.aurora.store.compose.composition.LocalUI
import com.aurora.store.compose.composition.UI
import com.aurora.store.compose.preview.ThemePreviewProvider

/**
 * Composable to display primary and secondary actions available for the app, supposed to be used
 * as a part of the Column with proper vertical arrangement spacing in the AppDetailsScreen.
 * @param primaryActionDisplayName Name of the primary action
 * @param secondaryActionDisplayName Name of the secondary action
 * @param isPrimaryActionEnabled Whether the primary action is enabled
 * @param isSecondaryActionEnabled Whether the secondary action is enabled
 * @param onPrimaryAction Callback when the primary action is clicked
 * @param onSecondaryAction Callback when the secondary action is clicked
 * @param windowAdaptiveInfo Adaptive window information
 */
@Composable
fun Actions(
    primaryActionDisplayName: String,
    secondaryActionDisplayName: String,
    isPrimaryActionEnabled: Boolean = true,
    isSecondaryActionEnabled: Boolean = true,
    primaryActionModifier: Modifier = Modifier,
    secondaryActionModifier: Modifier = Modifier,
    onPrimaryAction: () -> Unit = {},
    onSecondaryAction: () -> Unit = {},
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfoV2()
) {
    val isTv = LocalUI.current == UI.TV
    val horizontalPadding = if (isTv) 0.dp else dimensionResource(R.dimen.spacing_medium)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PaddingValues(horizontal = horizontalPadding)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        val buttonWidthModifier = when {
            isTv || windowAdaptiveInfo.isWindowCompact -> Modifier.weight(1F)
            else -> Modifier.widthIn(min = dimensionResource(R.dimen.width_button))
        }
        val buttonModifier = buttonWidthModifier.then(
            if (isTv) Modifier.height(56.dp) else Modifier
        )

        FilledTonalButton(
            modifier = buttonModifier.then(secondaryActionModifier),
            onClick = onSecondaryAction,
            enabled = isSecondaryActionEnabled
        ) {
            Text(
                text = secondaryActionDisplayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Button(
            modifier = buttonModifier.then(primaryActionModifier),
            onClick = onPrimaryAction,
            enabled = isPrimaryActionEnabled
        ) {
            Text(
                text = primaryActionDisplayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun ActionsPreview() {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        Actions(
            primaryActionDisplayName = stringResource(R.string.action_install),
            secondaryActionDisplayName = stringResource(R.string.title_manual_download)
        )
    }
}
