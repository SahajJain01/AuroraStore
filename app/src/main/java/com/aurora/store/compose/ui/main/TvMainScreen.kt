/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.aurora.store.compose.composable.tvFocusRing
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

private enum class TvHomeDestination(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @DrawableRes val iconRes: Int
) {
    APPS(R.string.title_apps, R.string.tab_for_you, R.drawable.ic_apps),
    GAMES(R.string.title_games, R.string.tab_for_you, R.drawable.ic_games),
    UPDATES(R.string.title_updates, R.string.check_updates, R.drawable.ic_updates)
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

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        TvSideBar(
            selected = selected,
            updateCount = updateCount,
            onSelect = { selected = it },
            onSearch = { onNavigateTo(Destination.Search) },
            onDownloads = { onNavigateTo(Destination.Downloads) },
            onSettings = { onNavigateTo(Destination.Settings) }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 28.dp, top = 28.dp, end = 36.dp, bottom = 24.dp)
        ) {
            TvHeader(destination = selected)

            Spacer(Modifier.height(20.dp))

            when (selected) {
                TvHomeDestination.APPS -> TvStoreFrontPage(
                    pageType = 0,
                    onNavigateTo = onNavigateTo
                )

                TvHomeDestination.GAMES -> TvStoreFrontPage(
                    pageType = 1,
                    onNavigateTo = onNavigateTo
                )

                TvHomeDestination.UPDATES -> TvUpdatesPage(
                    viewModel = updatesViewModel,
                    onNavigateTo = onNavigateTo,
                    onAppUpdateTarget = onAppUpdateTarget
                )
            }
        }
    }
}

@Composable
private fun TvSideBar(
    selected: TvHomeDestination,
    updateCount: Int,
    onSelect: (TvHomeDestination) -> Unit,
    onSearch: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit
) {
    val initialFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        initialFocus.requestFocus()
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxHeight()
            .width(232.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            )

            TvActionItem(
                labelRes = R.string.action_search,
                iconRes = R.drawable.ic_round_search,
                onClick = onSearch
            )

            Spacer(Modifier.height(8.dp))

            TvHomeDestination.entries.forEachIndexed { index, destination ->
                TvNavItem(
                    destination = destination,
                    selected = selected == destination,
                    updateCount = updateCount,
                    onClick = { onSelect(destination) },
                    modifier = if (index == 0) {
                        Modifier.focusRequester(initialFocus)
                    } else {
                        Modifier
                    }
                )
            }

            Spacer(Modifier.weight(1f))

            TvActionItem(
                labelRes = R.string.title_download_manager,
                iconRes = R.drawable.ic_download_manager,
                onClick = onDownloads
            )
            TvActionItem(
                labelRes = R.string.title_settings,
                iconRes = R.drawable.ic_settings_account,
                onClick = onSettings
            )
        }
    }
}

@Composable
private fun TvNavItem(
    destination: TvHomeDestination,
    selected: Boolean,
    updateCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    val background = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val foreground = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .tvFocusRing(shape, focusedScale = 1.02f)
            .clip(shape)
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (destination == TvHomeDestination.UPDATES && updateCount > 0) {
            BadgedBox(badge = { Badge { Text("$updateCount") } }) {
                Icon(
                    painter = painterResource(destination.iconRes),
                    contentDescription = null,
                    tint = foreground
                )
            }
        } else {
            Icon(
                painter = painterResource(destination.iconRes),
                contentDescription = null,
                tint = foreground
            )
        }
        Text(
            text = stringResource(destination.titleRes),
            style = MaterialTheme.typography.titleMedium,
            color = foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TvActionItem(
    @StringRes
    labelRes: Int,
    @DrawableRes
    iconRes: Int,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .tvFocusRing(shape, focusedScale = 1.02f)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(painter = painterResource(iconRes), contentDescription = null)
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TvHeader(destination: TvHomeDestination) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(destination.titleRes),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
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
}

@Composable
private fun TvStoreFrontPage(
    pageType: Int,
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

    TvStoreFrontBody(
        streamBundle = streamBundle,
        topChartCluster = topChartCluster,
        categories = categories,
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
private fun TvStoreFrontBody(
    streamBundle: StreamBundle?,
    topChartCluster: StreamCluster?,
    categories: List<Category>?,
    streamLoading: Boolean,
    chartLoading: Boolean,
    categoriesLoading: Boolean,
    onAppClick: (App) -> Unit,
    onHeaderClick: (StreamCluster) -> Unit,
    onCategoryClick: (Category) -> Unit,
    onLoadMoreStream: () -> Unit,
    onClusterEnd: (StreamCluster) -> Unit
) {
    val clusters = streamBundle?.streamClusters?.values
        ?.filter { it.clusterTitle.isNotBlank() && it.clusterAppList.isNotEmpty() }
        .orEmpty()
    val heroApp = clusters.firstOrNull()?.clusterAppList?.tvOptimizedFirst()?.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        when {
            heroApp != null -> item(key = "hero") {
                TvHeroApp(app = heroApp, onClick = { onAppClick(heroApp) })
            }

            streamLoading -> item(key = "hero_loading") {
                TvLoadingBand()
            }
        }

        clusters.take(5).forEach { cluster ->
            item(key = "cluster_${cluster.id}") {
                TvAppSection(
                    title = cluster.clusterTitle,
                    apps = cluster.clusterAppList.tvOptimizedFirst().take(16),
                    onHeaderClick = if (cluster.clusterBrowseUrl.isNotBlank()) {
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
            item(key = "load_more") {
                TvWideAction(
                    title = stringResource(R.string.title_more),
                    iconRes = R.drawable.ic_arrow_down,
                    onClick = onLoadMoreStream
                )
            }
        }

        if (!topChartCluster?.clusterAppList.isNullOrEmpty() || chartLoading) {
            item(key = "top_free") {
                TvAppSection(
                    title = stringResource(R.string.tab_top_free),
                    apps = topChartCluster?.clusterAppList?.tvOptimizedFirst()?.take(20)
                        .orEmpty(),
                    loading = chartLoading,
                    onAppClick = onAppClick
                )
            }
        }

        if (!categories.isNullOrEmpty() || categoriesLoading) {
            item(key = "categories") {
                TvCategorySection(
                    categories = categories.orEmpty().take(18),
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
private fun TvHeroApp(app: App, onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .tvFocusRing(shape, focusedScale = 1.01f)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            AsyncImage(
                modifier = Modifier
                    .requiredSize(156.dp)
                    .clip(RoundedCornerShape(32.dp)),
                model = ImageRequest.Builder(LocalContext.current)
                    .data(app.iconArtwork.url)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = app.displayName,
                    style = MaterialTheme.typography.headlineMedium,
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

@Composable
private fun TvAppSection(
    title: String,
    apps: List<App>,
    loading: Boolean = false,
    onHeaderClick: (() -> Unit)? = null,
    onAppClick: (App) -> Unit,
    onEndReached: (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TvSectionHeader(title = title, onClick = onHeaderClick)
        if (loading && apps.isEmpty()) {
            TvLoadingBand()
            return
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
        ) {
            items(count = apps.size, key = { apps[it].packageName }) { index ->
                if (index == apps.lastIndex) onEndReached?.invoke()
                TvAppCard(app = apps[index], onClick = { onAppClick(apps[index]) })
            }
        }
    }
}

@Composable
private fun TvAppCard(app: App, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .width(164.dp)
            .height(238.dp)
            .tvFocusRing(shape, focusedScale = 1.07f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp)),
            model = ImageRequest.Builder(LocalContext.current)
                .data(app.iconArtwork.url)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
        Text(
            text = app.displayName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
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
private fun TvCategorySection(
    categories: List<Category>,
    loading: Boolean,
    onCategoryClick: (Category) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TvSectionHeader(title = stringResource(R.string.tab_categories))
        if (loading && categories.isEmpty()) {
            TvLoadingBand()
            return
        }
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
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
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .width(230.dp)
            .height(82.dp)
            .tvFocusRing(shape, focusedScale = 1.04f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_apps),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = category.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TvUpdatesPage(
    viewModel: UpdatesViewModel,
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val activeUpdates = updateMap.entries.toList()
            if (activeUpdates.isNotEmpty()) {
                item(key = "update_header") {
                    TvUpdatesHeader(
                        count = activeUpdates.size,
                        fetching = fetchingUpdates,
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
                        onClick = { onAppUpdateTarget(update) },
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
                        onClick = { onAppUpdateTarget(update) },
                        onUnignore = { viewModel.unignore(update.packageName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TvUpdatesHeader(
    count: Int,
    fetching: Boolean,
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
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(onClick = onRefresh, enabled = !fetching) {
            Text(stringResource(R.string.check_updates))
        }
        Button(onClick = onUpdateAll) {
            Text(stringResource(R.string.action_update_all))
        }
    }
}

@Composable
private fun TvUpdateRow(
    update: Update,
    download: Download?,
    onClick: () -> Unit,
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
    val shape = RoundedCornerShape(22.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .tvFocusRing(shape, focusedScale = 1.01f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(modifier = Modifier.requiredSize(68.dp)) {
            AnimatedAppIcon(
                modifier = Modifier.requiredSize(68.dp),
                iconUrl = update.iconURL,
                inProgress = inProgress,
                progress = progress
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = update.displayName,
                style = MaterialTheme.typography.titleMedium,
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
        when {
            onUnignore != null -> OutlinedButton(onClick = onUnignore) {
                Text(stringResource(R.string.action_unignore))
            }

            installing -> OutlinedButton(onClick = {}, enabled = false) {
                Text(stringResource(R.string.action_installing))
            }

            inProgress && onCancel != null -> OutlinedButton(onClick = onCancel) {
                Text(stringResource(R.string.action_cancel))
            }

            onUpdate != null -> Button(onClick = onUpdate) {
                Text(stringResource(R.string.action_update))
            }
        }
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
                        .tvFocusRing(RoundedCornerShape(18.dp))
                        .clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
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
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .tvFocusRing(shape, focusedScale = 1.01f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(painter = painterResource(iconRes), contentDescription = null)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TvLoadingBand() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
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
