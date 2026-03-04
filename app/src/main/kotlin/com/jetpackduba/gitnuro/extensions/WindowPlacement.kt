package com.jetpackduba.gitnuro.extensions

import androidx.compose.ui.window.WindowPlacement
import com.jetpackduba.gitnuro.domain.models.ui.AppWindowPlacement


val defaultWindowPlacement = AppWindowPlacement.MAXIMIZED

val WindowPlacement.preferenceValue: AppWindowPlacement
    get() {
        return when (this) {
            WindowPlacement.Floating -> AppWindowPlacement.FLOATING
            WindowPlacement.Maximized -> AppWindowPlacement.MAXIMIZED
            WindowPlacement.Fullscreen -> AppWindowPlacement.FULLSCREEN
        }
    }

val AppWindowPlacement.toWindowPlacement: WindowPlacement
    get() {
        return when (this) {
            AppWindowPlacement.FLOATING -> WindowPlacement.Floating
            AppWindowPlacement.MAXIMIZED -> WindowPlacement.Maximized
            AppWindowPlacement.FULLSCREEN -> WindowPlacement.Fullscreen
        }
    }


