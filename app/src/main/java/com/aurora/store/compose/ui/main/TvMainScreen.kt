/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aurora.extensions.requiresGMS
import com.aurora.extensions.requiresObbDir
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.Category
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.contracts.StreamContract
import com.aurora.gplayapi.helpers.contracts.TopChartsContract
import com.aurora.store.CategoryStash
import com.aurora.store.HomeStash
import com.aurora.store.R
import com.aurora.store.compose.composable.ContainedLoadingIndicator
import com.aurora.store.compose.composable.Placeholder
import com.aurora.store.compose.composable.app.AnimatedAppIcon
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.data.model.DownloadStatus
import com.aurora.store.data.model.PermissionType
import com.aurora.store.data.model.ViewState
import com.aurora.store.data.providers.PermissionProvider.Companion.isGranted
import com.aurora.store.data.room.download.Download
import com.aurora.store.data.room.update.Update
import com.aurora.store.util.CommonUtil
import com.aurora.store.util.tvOptimizedFirst
import com.aurora.store.viewmodel.all.UpdatesViewModel
import com.aurora.store.viewmodel.category.CategoryViewModel
import com.aurora.store.viewmodel.homestream.StreamViewModel
import com.aurora.store.viewmodel.topchart.TopChartViewModel
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private val TvOverscanHorizontal = 58.dp
private val TvOverscanVertical = 28.dp
private val TvRailCollapsedWidth = 88.dp
private val TvRailExpandedWidth = 268.dp
private val TvContentStartPadding = 32.dp
private val TvCardWidth = 196.dp
private val TvCardHeight = 282.dp
private val TvAppShelfFocusPadding = 10.dp
private val TvCategoryShelfFocusPadding = 8.dp

private enum class TvHomeDestination(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @DrawableRes val iconRes: Int
) {
    APPS(R.string.title_apps, R.string.tab_for_you, R.drawable.ic_apps),
    GAMES(R.string.title_games, R.string.tab_for_you, R.drawable.ic_games),
    UPDATES(R.string.title_updates, R.string.check_updates, R.drawable.ic_updates)
}

private data class TvDisplayCluster(
    val cluster: StreamCluster,
    val apps: List<App>
)

@Composable
private fun Modifier.tvFocusSurface(
    shape: Shape,
    normalColor: Color,
    focusedColor: Color,
    focusedScale: Float = 1f,
    onFocusedChange: ((Boolean) -> Unit)? = null
): Modifier {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) focusedScale else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "tvFocusScale"
    )
    val containerColor by animateColorAsState(
        targetValue = if (focused) focusedColor else normalColor,
        animationSpec = tween(durationMillis = 90),
        label = "tvFocusColor"
    )

    return this
        .onFocusChanged {
            val nextFocused = it.isFocused || it.hasFocus
            focused = nextFocused
            onFocusedChange?.invoke(nextFocused)
        }
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

@Composable
internal fun TvMainScreen(
    initialTab: Int,
    updateCount: Int,
    updatesViewModel: UpdatesViewModel,
    onNavigateTo: (Destination) -> Unit,
    onAppUpdateTarget: (Update) -> Unit
) {
    var selected by rememberSaveable {
        mutableStateOf(
            when (initialTab) {
                1 -> TvHomeDestination.GAMES
                2 -> TvHomeDestination.UPDATES
                else -> TvHomeDestination.APPS
            }
        )
    }
    val contentFocusRequester = remember { FocusRequester() }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TvNavigationDrawer(
            selected = selected,
            updateCount = updateCount,
            contentFocusRequester = contentFocusRequester,
            onSelect = { selected = it },
            onSearch = { onNavigateTo(Destination.Search) },
            onDownloads = { onNavigateTo(Destination.Downloads) },
            onSettings = { onNavigateTo(Destination.Settings) }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(
                    start = TvContentStartPadding,
                    top = TvOverscanVertical,
                    end = TvOverscanHorizontal,
                    bottom = TvOverscanVertical
                )
        ) {
            TvPageHeader(destination = selected, updateCount = updateCount)

            Spacer(Modifier.height(18.dp))

            when (selected) {
                TvHomeDestination.APPS -> TvStoreFrontPage(
                    pageType = 0,
                    contentFocusRequester = contentFocusRequester,
                    onNavigateTo = onNavigateTo
                )

                TvHomeDestination.GAMES -> TvStoreFrontPage(
                    pageType = 1,
                    contentFocusRequester = contentFocusRequester,
                    onNavigateTo = onNavigateTo
                )

                TvHomeDestination.UPDATES -> TvUpdatesPage(
                    viewModel = updatesViewModel,
                    contentFocusRequester = contentFocusRequester,
                    onNavigateTo = onNavigateTo,
                    onAppUpdateTarget = onAppUpdateTarget
                )
            }
        }
    }
}

@Composable
private fun TvNavigationDrawer(
    selected: TvHomeDestination,
    updateCount: Int,
    contentFocusRequester: FocusRequester,
    onSelect: (TvHomeDestination) -> Unit,
    onSearch: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit
) {
    val initialFocus = remember { FocusRequester() }
    var expanded by remember { mutableStateOf(true) }
    val railWidth by animateDpAsState(
        targetValue = if (expanded) TvRailExpandedWidth else TvRailCollapsedWidth,
        animationSpec = tween(durationMillis = 140),
        label = "tvRailWidth"
    )
    val railPadding by animateDpAsState(
        targetValue = if (expanded) 20.dp else 12.dp,
        animationSpec = tween(durationMillis = 140),
        label = "tvRailPadding"
    )

    LaunchedEffect(Unit) {
        awaitFrame()
        initialFocus.requestFocus()
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxHeight()
            .width(railWidth)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
                    runCatching { contentFocusRequester.requestFocus() }.getOrDefault(false)
                } else {
                    false
                }
            }
            .focusGroup()
            .onFocusChanged { expanded = it.hasFocus || it.isFocused }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = railPadding, top = TvOverscanVertical, end = railPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TvDrawerLogo(
                expanded = expanded
            )

            TvDrawerActionItem(
                labelRes = R.string.action_search,
                iconRes = R.drawable.ic_round_search,
                expanded = expanded,
                contentFocusRequester = contentFocusRequester,
                onClick = onSearch
            )

            Spacer(Modifier.height(4.dp))

            TvHomeDestination.entries.forEachIndexed { index, destination ->
                TvDrawerDestinationItem(
                    destination = destination,
                    selected = selected == destination,
                    updateCount = updateCount,
                    expanded = expanded,
                    contentFocusRequester = contentFocusRequester,
                    onClick = { onSelect(destination) },
                    modifier = if (index == 0) {
                        Modifier.focusRequester(initialFocus)
                    } else {
                        Modifier
                    }
                )
            }

            Spacer(Modifier.weight(1f))

            TvDrawerActionItem(
                labelRes = R.string.title_download_manager,
                iconRes = R.drawable.ic_download_manager,
                expanded = expanded,
                contentFocusRequester = contentFocusRequester,
                onClick = onDownloads
            )
            TvDrawerActionItem(
                labelRes = R.string.title_settings,
                iconRes = R.drawable.ic_settings_account,
                expanded = expanded,
                contentFocusRequester = contentFocusRequester,
                onClick = onSettings
            )
        }
    }
}

@Composable
private fun TvDrawerLogo(expanded: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = if (expanded) 16.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (expanded) Arrangement.spacedBy(14.dp) else Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(30.dp)
        )
        if (expanded) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TvDrawerDestinationItem(
    destination: TvHomeDestination,
    selected: Boolean,
    updateCount: Int,
    expanded: Boolean,
    contentFocusRequester: FocusRequester,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)
    val normalColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    val focusedColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .focusProperties { right = contentFocusRequester }
            .tvFocusSurface(
                shape = shape,
                normalColor = normalColor,
                focusedColor = focusedColor,
                focusedScale = 1.03f
            )
            .clickable(onClick = onClick)
            .padding(horizontal = if (expanded) 18.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (expanded) Arrangement.spacedBy(14.dp) else Arrangement.Center
    ) {
        if (destination == TvHomeDestination.UPDATES && updateCount > 0) {
            BadgedBox(badge = { Badge { Text("$updateCount") } }) {
                Icon(
                    painter = painterResource(destination.iconRes),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        } else {
            Icon(
                painter = painterResource(destination.iconRes),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(26.dp)
            )
        }
        if (expanded) {
            Text(
                text = stringResource(destination.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TvDrawerActionItem(
    @StringRes labelRes: Int,
    @DrawableRes iconRes: Int,
    expanded: Boolean,
    contentFocusRequester: FocusRequester,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .focusProperties { right = contentFocusRequester }
            .tvFocusSurface(
                shape = shape,
                normalColor = Color.Transparent,
                focusedColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                focusedScale = 1.03f
            )
            .clickable(onClick = onClick)
            .padding(horizontal = if (expanded) 18.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (expanded) Arrangement.spacedBy(14.dp) else Arrangement.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(26.dp)
        )
        if (expanded) {
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TvPageHeader(destination: TvHomeDestination, updateCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = stringResource(destination.titleRes),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(destination.subtitleRes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (updateCount > 0) {
            TvInfoPill(text = "$updateCount " + stringResource(R.string.title_updates))
        }
    }
}

@Composable
private fun TvInfoPill(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun TvStoreFrontPage(
    pageType: Int,
    contentFocusRequester: FocusRequester,
    streamViewModel: StreamViewModel = hiltViewModel(key = "tv_stream_$pageType"),
    topChartViewModel: TopChartViewModel = hiltViewModel(key = "tv_topChart_$pageType"),
    categoryViewModel: CategoryViewModel = hiltViewModel(key = "tv_category_$pageType"),
    onNavigateTo: (Destination) -> Unit
) {
    val streamCategory = if (pageType == 1) {
        StreamContract.Category.GAME
    } else {
        StreamContract.Category.APPLICATION
    }
    val chartType = if (pageType == 1) {
        TopChartsContract.Type.GAME
    } else {
        TopChartsContract.Type.APPLICATION
    }
    val categoryType = if (pageType == 1) {
        Category.Type.GAME
    } else {
        Category.Type.APPLICATION
    }

    val streamState by streamViewModel.liveData.observeAsState()
    val chartState by topChartViewModel.state.collectAsStateWithLifecycle()
    val categoryState by categoryViewModel.liveData.observeAsState()

    LaunchedEffect(streamCategory) {
        streamViewModel.getStreamBundle(streamCategory, StreamContract.Type.HOME)
    }
    LaunchedEffect(chartType) {
        topChartViewModel.getStreamCluster(chartType, TopChartsContract.Chart.TOP_SELLING_FREE)
    }
    LaunchedEffect(categoryType) {
        categoryViewModel.getCategoryList(categoryType)
    }

    @Suppress("UNCHECKED_CAST")
    val streamBundle = (streamState as? ViewState.Success<*>)?.data
        .let { it as? HomeStash }
        ?.get(streamCategory)
    val topChartCluster = (chartState as? ViewState.Success<*>)?.data as? StreamCluster

    @Suppress("UNCHECKED_CAST")
    val categories = (categoryState as? ViewState.Success<*>)?.data
        .let { it as? CategoryStash }
        ?.get(categoryType)

    TvBrowsePage(
        streamBundle = streamBundle,
        topChartCluster = topChartCluster,
        categories = categories,
        contentFocusRequester = contentFocusRequester,
        streamLoading = streamState == null || streamState is ViewState.Loading,
        chartLoading = chartState is ViewState.Loading,
        categoriesLoading = categoryState == null || categoryState is ViewState.Loading,
        onAppClick = { onNavigateTo(Destination.AppDetails(it.packageName)) },
        onHeaderClick = { onNavigateTo(Destination.StreamBrowse(it)) },
        onCategoryClick = { onNavigateTo(Destination.CategoryBrowse(it)) },
        onLoadMoreStream = {
            streamViewModel.observe(streamCategory, StreamContract.Type.HOME)
        },
        onClusterEnd = { cluster ->
            streamViewModel.observeCluster(streamCategory, cluster)
        }
    )
}

@Composable
private fun TvBrowsePage(
    streamBundle: StreamBundle?,
    topChartCluster: StreamCluster?,
    categories: List<Category>?,
    contentFocusRequester: FocusRequester,
    streamLoading: Boolean,
    chartLoading: Boolean,
    categoriesLoading: Boolean,
    onAppClick: (App) -> Unit,
    onHeaderClick: (StreamCluster) -> Unit,
    onCategoryClick: (Category) -> Unit,
    onLoadMoreStream: () -> Unit,
    onClusterEnd: (StreamCluster) -> Unit
) {
    val clusters = remember(streamBundle) {
        streamBundle?.streamClusters?.values
            ?.filter { it.clusterTitle.isNotBlank() && it.clusterAppList.isNotEmpty() }
            .orEmpty()
    }
    val displayClusters = remember(clusters) {
        clusters.take(5).map { cluster ->
            TvDisplayCluster(
                cluster = cluster,
                apps = cluster.clusterAppList.tvOptimizedFirst().take(14)
            )
        }
    }
    val heroApp = remember(clusters) {
        clusters.firstOrNull()?.clusterAppList?.tvOptimizedFirst()?.firstOrNull()
    }
    val topChartApps = remember(topChartCluster) {
        topChartCluster?.clusterAppList?.tvOptimizedFirst()?.take(14).orEmpty()
    }
    val categoryItems = remember(categories) {
        categories.orEmpty().take(12)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        when {
            heroApp != null -> item(key = "featured") {
                TvFeaturedApp(
                    app = heroApp,
                    modifier = Modifier.focusRequester(contentFocusRequester),
                    onClick = { onAppClick(heroApp) }
                )
            }

            streamLoading -> item(key = "featured_loading") {
                TvLoadingPanel(height = 238.dp)
            }
        }

        displayClusters.forEach { displayCluster ->
            val cluster = displayCluster.cluster
            item(key = "cluster_${cluster.id}") {
                TvAppSection(
                    title = cluster.clusterTitle,
                    apps = displayCluster.apps,
                    onShowAllClick = if (cluster.clusterBrowseUrl.isNotBlank()) {
                        { onHeaderClick(cluster) }
                    } else {
                        null
                    },
                    onAppClick = onAppClick,
                    onEndReached = if (cluster.hasNext()) {
                        { onClusterEnd(cluster) }
                    } else {
                        null
                    }
                )
            }
        }

        if (clusters.isNotEmpty() && streamBundle?.hasNext() == true) {
            item(key = "more_stream") {
                TvWideAction(
                    title = stringResource(R.string.title_more),
                    iconRes = R.drawable.ic_arrow_down,
                    onClick = onLoadMoreStream
                )
            }
        }

        if (topChartApps.isNotEmpty() || chartLoading) {
            item(key = "top_free") {
                TvAppSection(
                    title = stringResource(R.string.tab_top_free),
                    apps = topChartApps,
                    loading = chartLoading,
                    onAppClick = onAppClick
                )
            }
        }

        if (categoryItems.isNotEmpty() || categoriesLoading) {
            item(key = "categories") {
                TvCategorySection(
                    categories = categoryItems,
                    loading = categoriesLoading,
                    onCategoryClick = onCategoryClick
                )
            }
        }

        if (!streamLoading && clusters.isEmpty()) {
            item(key = "empty") {
                Placeholder(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    painter = painterResource(R.drawable.ic_apps),
                    message = stringResource(R.string.no_apps_available)
                )
            }
        }
    }
}

@Composable
private fun TvFeaturedApp(app: App, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(238.dp)
            .tvFocusSurface(
                shape = shape,
                normalColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                focusedScale = 1.015f
            )
            .clickable(onClick = onClick)
            .padding(26.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        TvAppArtwork(
            imageUrl = app.iconArtwork.url,
            modifier = Modifier.requiredSize(168.dp),
            shape = RoundedCornerShape(34.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                text = stringResource(R.string.tab_for_you),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = app.displayName,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.developerName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildTvAppMeta(app),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            painter = painterResource(R.drawable.ic_arrow_forward),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(38.dp)
        )
    }
}

@Composable
private fun TvAppSection(
    title: String,
    apps: List<App>,
    loading: Boolean = false,
    onShowAllClick: (() -> Unit)? = null,
    onAppClick: (App) -> Unit,
    onEndReached: (() -> Unit)? = null
) {
    val rowState = rememberLazyListState()
    var requestedSize by remember { mutableIntStateOf(-1) }

    LaunchedEffect(rowState, apps.size, onEndReached) {
        if (onEndReached == null || apps.isEmpty()) return@LaunchedEffect
        snapshotFlow {
            rowState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        }
            .distinctUntilChanged()
            .filter { lastVisibleIndex ->
                lastVisibleIndex >= apps.lastIndex - 1 && requestedSize != apps.size
            }
            .collect {
                requestedSize = apps.size
                onEndReached()
            }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TvSectionHeader(title = title)

        if (loading && apps.isEmpty()) {
            TvLoadingPanel(height = 196.dp)
            return
        }

        LazyRow(
            state = rowState,
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = TvAppShelfFocusPadding)
        ) {
            items(count = apps.size, key = { apps[it].packageName }) { index ->
                TvAppCard(app = apps[index], onClick = { onAppClick(apps[index]) })
            }
            if (onShowAllClick != null) {
                item(key = "show_more") {
                    TvShowMoreCard(onClick = onShowAllClick)
                }
            }
        }
    }
}

@Composable
private fun TvAppCard(app: App, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .width(TvCardWidth)
            .height(TvCardHeight)
            .tvFocusSurface(
                shape = shape,
                normalColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedColor = MaterialTheme.colorScheme.primaryContainer,
                focusedScale = 1f
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TvAppArtwork(
            imageUrl = app.iconArtwork.url,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(20.dp)
        )
        Text(
            text = app.displayName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = buildTvAppMeta(app),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TvShowMoreCard(onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .width(TvCardWidth)
            .height(TvCardHeight)
            .tvFocusSurface(
                shape = shape,
                normalColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                focusedScale = 1f
            )
            .clickable(onClick = onClick)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_forward),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(42.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.title_more),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TvAppArtwork(imageUrl: String, modifier: Modifier, shape: Shape) {
    AsyncImage(
        modifier = modifier.clip(shape),
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(false)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun TvCategorySection(
    categories: List<Category>,
    loading: Boolean,
    onCategoryClick: (Category) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TvSectionHeader(title = stringResource(R.string.tab_categories))
        if (loading && categories.isEmpty()) {
            TvLoadingPanel(height = 96.dp)
            return
        }
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(
                horizontal = 2.dp,
                vertical = TvCategoryShelfFocusPadding
            )
        ) {
            items(count = categories.size, key = { categories[it].title }) { index ->
                TvCategoryCard(
                    category = categories[index],
                    onClick = { onCategoryClick(categories[index]) }
                )
            }
        }
    }
}

@Composable
private fun TvCategoryCard(category: Category, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .width(240.dp)
            .height(84.dp)
            .tvFocusSurface(
                shape = shape,
                normalColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                focusedScale = 1f
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_apps),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = category.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TvUpdatesPage(
    viewModel: UpdatesViewModel,
    contentFocusRequester: FocusRequester,
    onNavigateTo: (Destination) -> Unit,
    onAppUpdateTarget: (Update) -> Unit
) {
    val context = LocalContext.current
    val updates by viewModel.updates.collectAsStateWithLifecycle()
    val ignoredUpdates by viewModel.ignoredUpdates.collectAsStateWithLifecycle()
    val downloads by viewModel.downloadsList.collectAsStateWithLifecycle()
    val fetchingUpdates by viewModel.fetchingUpdates.collectAsStateWithLifecycle()

    val updateMap = remember(updates, downloads) {
        updates?.associateWith { update ->
            downloads.find {
                it.packageName == update.packageName &&
                    it.versionCode == update.versionCode
            }
        }
    }

    fun requestUpdate(update: Update) {
        if (update.fileList.requiresObbDir() &&
            !isGranted(context, PermissionType.STORAGE_MANAGER)
        ) {
            onNavigateTo(
                Destination.PermissionRationale(setOf(PermissionType.STORAGE_MANAGER))
            )
        } else {
            viewModel.download(update)
        }
    }

    fun requestUpdateAll(targets: List<Update>) {
        val needsObb = targets.any { it.fileList.requiresObbDir() }
        if (needsObb && !isGranted(context, PermissionType.STORAGE_MANAGER)) {
            onNavigateTo(
                Destination.PermissionRationale(setOf(PermissionType.STORAGE_MANAGER))
            )
        } else {
            viewModel.downloadAll(targets)
        }
    }

    when {
        updateMap == null -> TvUpdatesLoading()

        updateMap.isEmpty() && ignoredUpdates.isEmpty() -> Placeholder(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(R.drawable.ic_updates),
            message = stringResource(R.string.details_no_updates),
            actionLabel = stringResource(R.string.check_updates),
            onAction = { viewModel.fetchUpdates() }
        )

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val activeUpdates = updateMap.entries.toList()
            if (activeUpdates.isNotEmpty()) {
                item(key = "update_actions") {
                    TvUpdateActions(
                        count = activeUpdates.size,
                        fetching = fetchingUpdates,
                        contentFocusRequester = contentFocusRequester,
                        onRefresh = { viewModel.fetchUpdates() },
                        onUpdateAll = { requestUpdateAll(activeUpdates.map { it.key }) }
                    )
                }
                items(
                    count = activeUpdates.size,
                    key = { activeUpdates[it].key.packageName }
                ) { index ->
                    val (update, download) = activeUpdates[index]
                    TvUpdateRow(
                        update = update,
                        download = download,
                        onOpen = { onAppUpdateTarget(update) },
                        onUpdate = { requestUpdate(update) },
                        onCancel = { viewModel.cancelDownload(update.packageName) }
                    )
                }
            }

            if (ignoredUpdates.isNotEmpty()) {
                item(key = "ignored_header") {
                    TvSectionHeader(title = stringResource(R.string.updates_ignored_header))
                }
                items(
                    count = ignoredUpdates.size,
                    key = { "ignored-${ignoredUpdates[it].packageName}" }
                ) { index ->
                    val update = ignoredUpdates[index]
                    TvUpdateRow(
                        update = update,
                        download = null,
                        onOpen = { onAppUpdateTarget(update) },
                        onUnignore = { viewModel.unignore(update.packageName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TvUpdateActions(
    count: Int,
    fetching: Boolean,
    contentFocusRequester: FocusRequester,
    onRefresh: () -> Unit,
    onUpdateAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "$count " + stringResource(
                if (count == 1) R.string.update_available else R.string.updates_available
            ),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        TvActionChip(
            modifier = Modifier.focusRequester(contentFocusRequester),
            title = stringResource(R.string.check_updates),
            enabled = !fetching,
            onClick = onRefresh
        )
        TvActionChip(
            title = stringResource(R.string.action_update_all),
            onClick = onUpdateAll
        )
    }
}

@Composable
private fun TvUpdateRow(
    update: Update,
    download: Download?,
    onOpen: () -> Unit,
    onUpdate: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onUnignore: (() -> Unit)? = null
) {
    val inProgress = download != null && !download.isFinished
    val installing = download?.status == DownloadStatus.INSTALLING
    val progress = if (download?.status == DownloadStatus.DOWNLOADING) {
        download.progress.toFloat()
    } else {
        0f
    }
    val shape = RoundedCornerShape(24.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .tvFocusSurface(
                    shape = shape,
                    normalColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    focusedScale = 1.015f
                )
                .clickable(onClick = onOpen)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            AnimatedAppIcon(
                modifier = Modifier.requiredSize(68.dp),
                iconUrl = update.iconURL,
                inProgress = inProgress,
                progress = progress
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = update.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = update.developerName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${update.versionName}  ${CommonUtil.addSiPrefix(update.size)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        when {
            onUnignore != null -> TvActionChip(
                title = stringResource(R.string.action_unignore),
                onClick = onUnignore
            )

            installing -> TvActionChip(
                title = stringResource(R.string.action_installing),
                enabled = false,
                onClick = {}
            )

            inProgress && onCancel != null -> TvActionChip(
                title = stringResource(R.string.action_cancel),
                onClick = onCancel
            )

            onUpdate != null -> TvActionChip(
                title = stringResource(R.string.action_update),
                onClick = onUpdate
            )
        }
    }
}

@Composable
private fun TvActionChip(
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val normalColor = if (enabled) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .widthIn(min = 124.dp)
            .height(50.dp)
            .tvFocusSurface(
                shape = shape,
                normalColor = normalColor,
                focusedColor = MaterialTheme.colorScheme.primaryContainer,
                focusedScale = 1.04f
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TvSectionHeader(title: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .height(44.dp)
                        .tvFocusSurface(
                            shape = RoundedCornerShape(20.dp),
                            normalColor = Color.Transparent,
                            focusedColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedScale = 1.01f
                        )
                        .clickable(onClick = onClick)
                        .padding(horizontal = 8.dp)
                } else {
                    Modifier.padding(horizontal = 2.dp, vertical = 4.dp)
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (onClick != null) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TvWideAction(title: String, @DrawableRes iconRes: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .tvFocusSurface(
                shape = shape,
                normalColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                focusedScale = 1.015f
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TvLoadingPanel(height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center
    ) {
        ContainedLoadingIndicator()
    }
}

@Composable
private fun TvUpdatesLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun buildTvAppMeta(app: App): String = buildList {
    if (app.labeledRating.isNotBlank()) add("${app.labeledRating}*")
    add(stringResource(if (app.isFree) R.string.details_free else R.string.details_paid))
    if (app.size > 0) add(CommonUtil.addSiPrefix(app.size))
    if (app.requiresGMS()) add(stringResource(R.string.details_gsf_dependent))
}.joinToString(separator = "  ")
