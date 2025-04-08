package com.jetpackduba.gitnuro

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.focus.FocusRequester
import com.jetpackduba.gitnuro.avatarproviders.AvatarProvider
import com.jetpackduba.gitnuro.avatarproviders.NoneAvatarProvider

val LocalTabFocusRequester = compositionLocalOf { FocusRequester() }
val LocalAvatarProvider = compositionLocalOf<AvatarProvider> { NoneAvatarProvider() }
val LocalDateTimeFormat = compositionLocalOf { SettingsDefaults.defaultDateTimeFormat }
