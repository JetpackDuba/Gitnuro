@file:Suppress("unused")

package com.jetpackduba.gitnuro.theme

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.jetpackduba.gitnuro.ui.dropdowns.DropDownOption

private val defaultAppTheme: ColorsScheme = darkBlueTheme
private var appTheme: ColorsScheme = defaultAppTheme

@Composable
fun AppTheme(
    selectedTheme: Theme = Theme.DARK,
    customTheme: ColorsScheme?,
    content: @Composable() () -> Unit
) {
    val theme = when (selectedTheme) {
        Theme.LIGHT -> lightTheme
        Theme.DARK -> darkBlueTheme
        Theme.DARK_GRAY -> darkGrayTheme
        Theme.CUSTOM -> customTheme ?: defaultAppTheme
    }

    appTheme = theme
    val composeColors = theme.toComposeColors()
    MaterialTheme(
        colors = composeColors,
        content = content,
        typography = typography(composeColors),
    )
}

val Colors.backgroundSelected: Color
    get() = appTheme.backgroundSelected

val Colors.onBackgroundSecondary: Color
    get() = appTheme.onBackgroundSecondary

val Colors.secondarySurface: Color
    get() = appTheme.secondarySurface

val Colors.tertiarySurface: Color
    get() = appTheme.tertiarySurface


val Colors.addFile: Color
    get() = appTheme.addFile

val Colors.deleteFile: Color
    get() = appTheme.deletedFile

val Colors.modifyFile: Color
    get() = appTheme.modifiedFile

val Colors.conflictFile: Color
    get() = appTheme.conflictingFile

val Colors.abortButton: Color
    get() = appTheme.error

val Colors.scrollbarNormal: Color
    get() = appTheme.normalScrollbar

val Colors.scrollbarHover: Color
    get() = appTheme.hoverScrollbar

val Colors.dialogOverlay: Color
    get() = appTheme.dialogOverlay

val Colors.diffLineAdded: Color
    get() = appTheme.diffLineAdded

val Colors.diffLineRemoved: Color
    get() = appTheme.diffLineRemoved

val Colors.isDark: Boolean
    get() = !this.isLight


enum class Theme(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    DARK_GRAY("Dark gray"),
    CUSTOM("Custom");
}

val themeLists = listOf(
    DropDownOption(Theme.LIGHT, Theme.LIGHT.displayName),
    DropDownOption(Theme.DARK, Theme.DARK.displayName),
    DropDownOption(Theme.DARK_GRAY, Theme.DARK_GRAY.displayName),
    DropDownOption(Theme.CUSTOM, Theme.CUSTOM.displayName),
)