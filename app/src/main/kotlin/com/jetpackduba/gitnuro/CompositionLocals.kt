package com.jetpackduba.gitnuro

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.focus.FocusRequester
import com.jetpackduba.gitnuro.avatarproviders.AvatarProvider
import com.jetpackduba.gitnuro.avatarproviders.NoneAvatarProvider
import com.jetpackduba.gitnuro.domain.SettingsDefaults
import com.jetpackduba.gitnuro.viewmodels.RepositoryTabViewModel

val LocalTab =
    compositionLocalOf<RepositoryTabViewModel> { throw IllegalStateException("Tab information requested but not provided") }
val LocalTabFocusRequester = compositionLocalOf { FocusRequester() }
val LocalAvatarProvider = compositionLocalOf<AvatarProvider> { NoneAvatarProvider() }
val LocalDateTimeFormat = compositionLocalOf { SettingsDefaults.defaultDateTimeFormat }
