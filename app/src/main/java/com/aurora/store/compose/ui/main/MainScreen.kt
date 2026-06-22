/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.extensions.requiresObbDir
import com.aurora.store.MainViewModel
import com.aurora.store.R
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.composable.tvFocusRing
import com.aurora.store.compose.composition.LocalNetworkStatus
import com.aurora.store.compose.composition.LocalUI
import com.aurora.store.compose.composition.UI
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.compose.ui.apps.AppsGamesScreen
import com.aurora.store.compose.ui.commons.MoreSheet
import com.aurora.store.compose.ui.commons.NetworkScreen
import com.aurora.store.compose.ui.sheets.AppUpdateSheet
import com.aurora.store.compose.ui.updates.UpdatesScreen
import com.aurora.store.data.model.NetworkStatus
import com.aurora.store.data.model.PermissionType
import com.aurora.store.data.providers.PermissionProvider.Companion.isGranted
import com.aurora.store.data.room.update.Update
import com.aurora.store.viewmodel.all.UpdatesViewModel
import kotlinx.coroutines.launch

private enum class MainTab(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int
) {
    APPS(R.string.title_apps, R.drawable.ic_apps),
    GAMES(R.string.title_games, R.drawable.ic_games),
    UPDATES(R.string.title_updates, R.drawable.ic_updates)
}

@Composable
fun MainScreen(
    initialTab: Int = 0,
    mainViewModel: MainViewModel = hiltViewModel(),
    updatesViewModel: UpdatesViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val context = LocalContext.current
    val networkStatus = LocalNetworkStatus.current
    val updates by mainViewModel.updateHelper.updates.collectAsStateWithLifecycle(
        initialValue = null
    )
    val updateCount = updates?.size ?: 0

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = initialTab.coerceIn(
            0,
            MainTab.entries.size - 1
        )
    ) {
        MainTab.entries.size
    }

    var showMoreSheet by remember { mutableStateOf(false) }
    var appUpdateTarget by remember { mutableStateOf<Update?>(null) }

    fun handleNavigation(destination: Destination) {
        when (destination) {
            is Destination.AppUpdate -> appUpdateTarget = destination.update
            else -> onNavigateTo(destination)
        }
    }

    if (networkStatus == NetworkStatus.UNAVAILABLE) {
        NetworkScreen()
        return
    }

    if (showMoreSheet) {
        MoreSheet(
            onDismiss = { showMoreSheet = false },
            onNavigateTo = { destination ->
                showMoreSheet = false
                onNavigateTo(destination)
            }
        )
    }

    appUpdateTarget?.let { app ->
        AppUpdateSheet(
            update = app,
            onDismiss = { appUpdateTarget = null },
            onNavigateTo = { destination ->
                appUpdateTarget = null
                onNavigateTo(destination)
            }
        )
    }

    if (LocalUI.current == UI.TV) {
        TvMainScaffold(
            pagerState = pagerState,
            updateCount = updateCount,
            updatesViewModel = updatesViewModel,
            onNavigateTo = onNavigateTo,
            onMainTabClick = { index ->
                coroutineScope.launch { pagerState.animateScrollToPage(index) }
            },
            onAppUpdateTarget = { appUpdateTarget = it }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(MainTab.entries[pagerState.currentPage].labelRes),
                actions = {
                    IconButton(onClick = { onNavigateTo(Destination.Downloads) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_download_manager),
                            contentDescription = stringResource(R.string.title_download_manager)
                        )
                    }
                    IconButton(onClick = { showMoreSheet = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings_account),
                            contentDescription = stringResource(R.string.title_more)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateTo(Destination.Search) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_round_search),
                    contentDescription = stringResource(R.string.action_search)
                )
            }
        },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                        icon = {
                            MainTabIcon(tab = tab, updateCount = updateCount)
                        },
                        label = { Text(stringResource(tab.labelRes)) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .fillMaxSize()
        ) {
            MainPager(
                pagerState = pagerState,
                updatesViewModel = updatesViewModel,
                onNavigateTo = onNavigateTo,
                onHandleNavigation = ::handleNavigation
            )
        }
    }
}

@Composable
private fun TvMainScaffold(
    pagerState: PagerState,
    updateCount: Int,
    updatesViewModel: UpdatesViewModel,
    onNavigateTo: (Destination) -> Unit,
    onMainTabClick: (Int) -> Unit,
    onAppUpdateTarget: (Update) -> Unit
) {
    val firstTabFocusRequester = remember { FocusRequester() }

    fun handleNavigation(destination: Destination) {
        when (destination) {
            is Destination.AppUpdate -> onAppUpdateTarget(destination.update)
            else -> onNavigateTo(destination)
        }
    }

    LaunchedEffect(Unit) {
        firstTabFocusRequester.requestFocus()
    }

    androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail(
            modifier = Modifier
                .fillMaxHeight()
                .width(128.dp)
        ) {
            Column(modifier = Modifier.fillMaxHeight()) {
                MainTab.entries.forEachIndexed { index, tab ->
                    NavigationRailItem(
                        selected = pagerState.currentPage == index,
                        onClick = { onMainTabClick(index) },
                        icon = { MainTabIcon(tab = tab, updateCount = updateCount) },
                        label = { Text(stringResource(tab.labelRes)) },
                        alwaysShowLabel = true,
                        modifier = Modifier
                            .then(
                                if (index == 0) {
                                    Modifier.focusRequester(firstTabFocusRequester)
                                } else {
                                    Modifier
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .tvFocusRing(RoundedCornerShape(28.dp))
                    )
                }

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))

                TvRailAction(
                    labelRes = R.string.action_search,
                    iconRes = R.drawable.ic_round_search,
                    onClick = { onNavigateTo(Destination.Search) }
                )
                TvRailAction(
                    labelRes = R.string.title_download_manager,
                    iconRes = R.drawable.ic_download_manager,
                    onClick = { onNavigateTo(Destination.Downloads) }
                )
                TvRailAction(
                    labelRes = R.string.title_settings,
                    iconRes = R.drawable.ic_settings_account,
                    onClick = { onNavigateTo(Destination.Settings) }
                )
            }
        }

        Scaffold(
            modifier = Modifier.weight(1f),
            topBar = {
                TopAppBar(title = stringResource(MainTab.entries[pagerState.currentPage].labelRes))
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
                    .fillMaxSize()
            ) {
                MainPager(
                    pagerState = pagerState,
                    updatesViewModel = updatesViewModel,
                    onNavigateTo = onNavigateTo,
                    onHandleNavigation = ::handleNavigation
                )
            }
        }
    }
}

@Composable
private fun TvRailAction(
    @StringRes
    labelRes: Int,
    @DrawableRes
    iconRes: Int,
    onClick: () -> Unit
) {
    NavigationRailItem(
        selected = false,
        onClick = onClick,
        icon = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null
            )
        },
        label = { Text(stringResource(labelRes)) },
        alwaysShowLabel = true,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .tvFocusRing(RoundedCornerShape(28.dp))
    )
}

@Composable
private fun MainTabIcon(tab: MainTab, updateCount: Int) {
    if (tab == MainTab.UPDATES && updateCount > 0) {
        BadgedBox(badge = { Badge { Text("$updateCount") } }) {
            Icon(
                painter = painterResource(tab.iconRes),
                contentDescription = null
            )
        }
    } else {
        Icon(
            painter = painterResource(tab.iconRes),
            contentDescription = null
        )
    }
}

@Composable
private fun MainPager(
    pagerState: PagerState,
    updatesViewModel: UpdatesViewModel,
    onNavigateTo: (Destination) -> Unit,
    onHandleNavigation: (Destination) -> Unit
) {
    val context = LocalContext.current

    HorizontalPager(
        state = pagerState,
        userScrollEnabled = false,
        beyondViewportPageCount = MainTab.entries.size - 1,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (MainTab.entries[page]) {
            MainTab.APPS -> AppsGamesScreen(
                pageType = 0,
                onNavigateTo = onNavigateTo
            )

            MainTab.GAMES -> AppsGamesScreen(
                pageType = 1,
                onNavigateTo = onHandleNavigation
            )

            MainTab.UPDATES -> UpdatesScreen(
                viewModel = updatesViewModel,
                onNavigateTo = onHandleNavigation,
                onRequestUpdate = { update ->
                    if (update.fileList.requiresObbDir() &&
                        !isGranted(context, PermissionType.STORAGE_MANAGER)
                    ) {
                        onNavigateTo(
                            Destination.PermissionRationale(
                                setOf(PermissionType.STORAGE_MANAGER)
                            )
                        )
                    } else {
                        updatesViewModel.download(update)
                    }
                },
                onRequestUpdateAll = { selectedUpdates ->
                    val needsObb = selectedUpdates.any {
                        it.fileList.requiresObbDir()
                    }
                    if (needsObb && !isGranted(context, PermissionType.STORAGE_MANAGER)) {
                        onNavigateTo(
                            Destination.PermissionRationale(
                                setOf(PermissionType.STORAGE_MANAGER)
                            )
                        )
                    } else {
                        updatesViewModel.downloadAll(selectedUpdates)
                    }
                },
                onCancelUpdate = { packageName ->
                    updatesViewModel.cancelDownload(packageName)
                },
                onCancelAll = { updatesViewModel.cancelAll() }
            )
        }
    }
}
