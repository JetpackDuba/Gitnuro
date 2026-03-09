package com.jetpackduba.gitnuro.domain.repositories

import com.jetpackduba.gitnuro.domain.models.AppConfig
import com.jetpackduba.gitnuro.domain.models.AvatarProviderType
import com.jetpackduba.gitnuro.domain.models.DiffTextViewType
import com.jetpackduba.gitnuro.domain.models.ProxyType
import com.jetpackduba.gitnuro.domain.models.ui.LinesHeightType
import com.jetpackduba.gitnuro.domain.models.ui.Theme
import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    // UI
    val scaleUi: Flow<Float?>
    val theme: Flow<Theme?>
    val customTheme: Flow<String?>
    val linesHeightType: Flow<LinesHeightType?>
    val dateFormatUseDefault: Flow<Boolean?>
    val dateFormatCustomFormat: Flow<String?>
    val dateFormatIs24h: Flow<Boolean?>
    val dateFormatUseRelative: Flow<Boolean?>
    val avatarProvider: Flow<AvatarProviderType?>
    val swapStatusPanes: Flow<Boolean?>
    val showChangesAsTree: Flow<Boolean?>
    val diffDisplayFullFile: Flow<Boolean?>
    val diffTextViewType: Flow<DiffTextViewType?>

    // Git
    val pullWithRebase: Flow<Boolean?>
    val pushWithLease: Flow<Boolean?>
    val fastForwardMerge: Flow<Boolean?>
    val autoStashOnMerge: Flow<Boolean?>
    val cloneDefaultDirectory: Flow<String?>

    // Network
    val useProxy: Flow<Boolean?>
    val proxyUseAuth: Flow<Boolean?>
    val proxyType: Flow<ProxyType?>
    val proxyHostName: Flow<String?>
    val proxyPortNumber: Flow<Int?>
    val proxyHostUser: Flow<String?>
    val proxyHostPassword: Flow<String?>
    val verifySsl: Flow<Boolean?>
    val cacheCredentialsInMemory: Flow<Boolean?>

    // Tools
    val terminalPath: Flow<String?>

    suspend fun setConfiguration(appConfig: AppConfig)
}