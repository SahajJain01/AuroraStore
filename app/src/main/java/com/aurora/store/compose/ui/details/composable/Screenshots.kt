/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.details.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Artwork
import com.aurora.store.R
import com.aurora.store.compose.composable.details.ScreenshotListItem
import com.aurora.store.compose.composition.LocalUI
import com.aurora.store.compose.composition.UI
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider

/**
 * Composable to display screenshots of the app, supposed to be used as a part
 * of the Column with proper vertical arrangement spacing in the AppDetailsScreen.
 * @param screenshots Screenshots to display
 * @param onNavigateToScreenshot Callback when a screenshot is clicked
 */
@Composable
fun Screenshots(screenshots: List<Artwork>, onNavigateToScreenshot: (index: Int) -> Unit = {}) {
    val screenshotHeight = if (LocalUI.current == UI.TV) {
        140.dp
    } else {
        dimensionResource(R.dimen.screenshot_height)
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_medium)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        items(items = screenshots, key = { artwork -> artwork.url }) { artwork ->
            ScreenshotListItem(
                modifier = Modifier
                    .height(screenshotHeight)
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_small)))
                    .clickable { onNavigateToScreenshot(screenshots.indexOf(artwork)) },
                url = "${artwork.url}=rw-w480-v1-e15"
            )
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun ScreenshotsPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        Screenshots(screenshots = app.screenshots)
    }
}
