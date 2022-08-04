@file:Suppress("unused")

package app.theme

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.ui.dropdowns.DropDownOption

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


enum class Theme(val displayName: String) : DropDownOption {
    LIGHT("Light"),
    DARK("Dark"),
    DARK_GRAY("Dark gray"),
    CUSTOM("Custom");

    override val optionName: String
        get() = displayName
}

val themeLists = listOf(
    Theme.LIGHT,
    Theme.DARK,
    Theme.DARK_GRAY,
    Theme.CUSTOM,
)