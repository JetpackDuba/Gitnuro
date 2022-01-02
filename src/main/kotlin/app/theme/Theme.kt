package app.theme

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
    primary = primaryLight,
    primaryVariant = primaryDark,
    secondary = secondary,
    surface = surfaceColorDark,
    background = backgroundColorDark,
)

private val LightColorPalette = lightColors(
    primary = primary,
    primaryVariant = primaryDark,
    secondary = secondary,
    background = backgroundColorLight,
    surface = surfaceColorLight,
    error = errorColor
    /* Other default colors to override

    */
)

@Composable
fun AppTheme(theme: Themes = Themes.LIGHT, content: @Composable() () -> Unit) {
    val colors = when (theme) {
        Themes.LIGHT -> LightColorPalette
        Themes.DARK -> DarkColorPalette
    }

    MaterialTheme(
        colors = colors,
        content = content,
    )
}

@get:Composable
val Colors.primaryTextColor: Color
    get() = if (isLight) mainText else mainTextDark

@get:Composable
val Colors.inversePrimaryTextColor: Color
    get() = if (isLight) mainTextDark else mainText

@get:Composable
val Colors.secondaryTextColor: Color
    get() = if (isLight) secondaryText else secondaryTextDark

@get:Composable
val Colors.accent: Color
    get() = primaryLight

@get:Composable
val Colors.primaryGray: Color
    get() = primaryGrayLight

@get:Composable
val Colors.accentGray: Color
    get() = accentGrayLight

@get:Composable
val Colors.headerBackground: Color
    get() {
        return if (isLight)
            headerBackgroundLight
        else
            headerBackgroundDark
    }

@get:Composable
val Colors.addFile: Color
    get() = addFileLight

@get:Composable
val Colors.deleteFile: Color
    get() = deleteFileLight

@get:Composable
val Colors.modifyFile: Color
    get() = modifyFileLight

@get:Composable
val Colors.conflictFile: Color
    get() = conflictFileLight

@get:Composable
val Colors.headerText: Color
    get() = if (isLight) primary else mainTextDark


val Colors.tabColorActive: Color
    get() = if (isLight) primary else tabColorActiveDark


val Colors.tabColorInactive: Color
    get() = if (isLight) primaryLight else tabColorInactiveDark


enum class Themes(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark")
}

val themesList = listOf(
    Themes.LIGHT,
    Themes.DARK,
)