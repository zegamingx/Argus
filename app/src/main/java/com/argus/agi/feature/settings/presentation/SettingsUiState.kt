package com.argus.agi.feature.settings.presentation

/**
 * Immutable presentation projections for the Settings feature.
 *
 * These types deliberately omit Android framework objects, persistence entities,
 * connector DTOs and mutable model records.
 */
data class SettingsUiState(
    val installedApps: List<InstalledAppUiItem> = emptyList(),
    val searchResults: List<SearchResultUiItem> = emptyList(),
    val worldEntities: List<WorldEntityUiItem> = emptyList()
)

data class InstalledAppUiItem(
    val appName: String,
    val packageName: String,
    val isExcluded: Boolean
)

data class SearchResultUiItem(
    val title: String,
    val snippet: String
)

data class WorldEntityUiItem(
    val key: String,
    val value: String,
    val source: String
)
