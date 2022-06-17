package app.extensions

import androidx.compose.ui.window.WindowPlacement
import app.preferences.WindowsPlacementPreference


private val windowPlacementFloating = WindowsPlacementPreference(0)
private val windowPlacementMaximized = WindowsPlacementPreference(1)
private val windowPlacementFullscreen = WindowsPlacementPreference(2)

val defaultWindowPlacement = windowPlacementMaximized

val WindowPlacement.preferenceValue: WindowsPlacementPreference
    get() {
        return when (this) {
            WindowPlacement.Floating -> windowPlacementFloating
            WindowPlacement.Maximized -> windowPlacementMaximized
            WindowPlacement.Fullscreen -> windowPlacementFullscreen
        }
    }

val WindowsPlacementPreference.toWindowPlacement: WindowPlacement
    get() {
        return when (this) {
            windowPlacementFloating -> WindowPlacement.Floating
            windowPlacementFullscreen -> WindowPlacement.Fullscreen
            else -> WindowPlacement.Maximized
        }
    }


