package com.jetpackduba.gitnuro

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import org.jetbrains.jewel.ui.component.Text

object MainViewModel {
    fun onNavigateTo(destination: String) {
        currentView = views.first { viewInfo -> viewInfo.title == destination }
    }

    var theme: IntUiThemes by mutableStateOf(IntUiThemes.Light)

    var swingCompat: Boolean by mutableStateOf(false)

    val projectColor
        get() =
            if (theme.isLightHeader()) {
                Color(0xFFF5D4C1)
            } else {
                Color(0xFF654B40)
            }

    val views = mainMenuItems

    var currentView by mutableStateOf(views.first())
}

private val mainMenuItems =
    mutableStateListOf(
        ViewInfo(
            title = "Welcome",
            keyboardShortcut = KeyBinding(macOs = setOf("⌥", "W"), windows = setOf("Alt", "W")),
            content = { Text("WelcomeView") },
        ),
        ViewInfo(
            title = "Components",
            keyboardShortcut = KeyBinding(macOs = setOf("⌥", "C"), windows = setOf("Alt", "C")),
            content = { Text("ComponentsView") },
        ),
        ViewInfo(
            title = "Markdown",
            keyboardShortcut =
                KeyBinding(macOs = setOf("⌥", "M"), windows = setOf("Alt", "M"), linux = setOf("Alt", "M")),
            content = { Text("MarkdownDemo") },
        ),
    )
