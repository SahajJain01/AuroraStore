/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.util

import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster

object TvAppRanker {

    private val explicitTvTerms = listOf(
        "android tv",
        "google tv",
        "leanback",
        "tv remote",
        "television",
        "living room"
    )

    private val deviceTvTerms = listOf(
        "android tv",
        "google tv",
        "chromecast",
        "shield",
        "bravia",
        "fire tv",
        "mi box",
        "onn.",
        "television",
        "tv"
    )

    private val tvPackageSignals = listOf(
        ".tv",
        "tv.",
        "television",
        "leanback",
        "androidtv",
        "googletv",
        "chromecast"
    )

    private val tvCategoryTerms = listOf(
        "entertainment",
        "video",
        "music",
        "sports",
        "news",
        "movies",
        "media"
    )

    fun score(app: App): Int {
        val packageName = app.packageName.lowercase()
        val searchText = buildSearchText(app)
        var score = 0

        if (explicitTvTerms.any { it in searchText }) score += 120
        if (app.compatibility.any { device ->
                deviceTvTerms.any { term ->
                    term in device.name.lowercase() || term in device.requiredOS.lowercase()
                }
            }
        ) {
            score += 100
        }
        if (tvPackageSignals.any { it in packageName }) score += 70
        if (tvCategoryTerms.any { it in app.categoryName.lowercase() }) score += 30
        if (app.videoArtwork.url.isNotBlank()) score += 8
        if (app.coverArtwork.url.isNotBlank()) score += 4

        return score
    }

    fun isTvOptimized(app: App): Boolean = score(app) >= 70

    private fun buildSearchText(app: App): String = buildList {
        add(app.displayName)
        add(app.packageName)
        add(app.developerName)
        add(app.categoryName)
        add(app.shortDescription)
        add(app.description)
        addAll(app.tags.map { it.name })
        addAll(app.chips.map { it.title })
        addAll(
            app.displayBadges.flatMap { badge ->
                listOf(
                    badge.id,
                    badge.textMajor,
                    badge.textMinor,
                    badge.textDescription
                )
            }
        )
        addAll(
            app.infoBadges.flatMap { badge ->
                listOf(
                    badge.id,
                    badge.textMajor,
                    badge.textMinor,
                    badge.textDescription
                )
            }
        )
        addAll(app.appInfo.appInfoMap.keys)
        addAll(app.appInfo.appInfoMap.values)
        addAll(app.compatibility.flatMap { listOf(it.name, it.requiredOS) })
    }.joinToString(separator = " ").lowercase()
}

fun List<App>.tvOptimizedFirst(): List<App> = sortedWith(
    compareByDescending<App> { TvAppRanker.score(it) }
        .thenByDescending { it.rating.average }
        .thenByDescending { it.installs }
)

fun StreamCluster.tvOptimizedFirst(): StreamCluster = copy(
    clusterAppList = clusterAppList.tvOptimizedFirst()
)

fun StreamBundle.tvOptimizedFirst(): StreamBundle = copy(
    streamClusters = streamClusters.mapValues { (_, cluster) ->
        cluster.tvOptimizedFirst()
    }
)
