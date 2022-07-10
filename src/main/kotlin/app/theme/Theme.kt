@file:Suppress("unused")

package app.theme

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.DropDownOption

private val defaultAppTheme: ColorsScheme = darkBlueTheme
private var appTheme: ColorsScheme = defaultAppTheme

@Composable
fun AppTheme(
    selectedTheme: Themes = Themes.DARK,
    customTheme: ColorsScheme?,
    content: @Composable() () -> Unit
) {
    val theme = when (selectedTheme) {
        Themes.LIGHT -> lightTheme
        Themes.DARK -> darkBlueTheme
        Themes.DARK_GRAY -> darkGrayTheme
        Themes.CUSTOM -> customTheme ?: defaultAppTheme
    }

    appTheme = theme

    MaterialTheme(
        colors = theme.toComposeColors(),
        content = content,
        typography = typography(),
    )
}

@get:Composable
val Colors.backgroundSelected: Color
    get() = appTheme.backgroundSelected

@get:Composable
val Colors.primaryTextColor: Color
    get() = appTheme.primaryText

@get:Composable
val Colors.secondaryTextColor: Color
    get() = appTheme.secondaryText

@get:Composable
val Colors.borderColor: Color
    get() = appTheme.borderColor

@get:Composable
val Colors.headerBackground: Color
    get() = appTheme.headerBackground

@get:Composable
val Colors.graphHeaderBackground: Color
    get() = appTheme.graphHeaderBackground

@get:Composable
val Colors.addFile: Color
    get() = appTheme.addFile

@get:Composable
val Colors.deleteFile: Color
    get() = appTheme.deletedFile

@get:Composable
val Colors.modifyFile: Color
    get() = appTheme.modifiedFile

@get:Composable
val Colors.conflictFile: Color
    get() = appTheme.conflictingFile

@get:Composable
val Colors.headerText: Color
    get() = appTheme.onHeader

val Colors.stageButton: Color
    get() = appTheme.primary

val Colors.unstageButton: Color
    get() = appTheme.error

val Colors.abortButton: Color
    get() = appTheme.error

val Colors.scrollbarNormal: Color
    get() = appTheme.normalScrollbar

val Colors.scrollbarHover: Color
    get() = appTheme.hoverScrollbar

val Colors.secondarySurface: Color
    get() = appTheme.secondarySurface

val Colors.dialogOverlay: Color
    get() = appTheme.dialogOverlay


enum class Themes(val displayName: String) : DropDownOption {
    LIGHT("Light"),
    DARK("Dark"),
    DARK_GRAY("Dark gray"),
    CUSTOM("Custom");

    override val optionName: String
        get() = displayName
}

val themesList = listOf(
    Themes.LIGHT,
    Themes.DARK,
    Themes.DARK_GRAY,
    Themes.CUSTOM,
)