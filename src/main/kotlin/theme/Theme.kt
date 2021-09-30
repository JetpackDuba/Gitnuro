package theme

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
        primary = primaryLight,
        primaryVariant = primaryDark,
        secondary = secondary
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
fun GitnuroTheme(darkTheme: Boolean = false, content: @Composable() () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
            colors = colors,
            content = content,
    )
}

@get:Composable
val Colors.primaryTextColor: Color
    get() = if(isLight) mainText else mainTextDark

@get:Composable
val Colors.secondaryTextColor: Color
    get() = if(isLight) secondaryText else secondaryTextDark

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
    get() = headerBackgroundLight

@get:Composable
val Colors.addFile: Color
    get() = addFileLight

@get:Composable
val Colors.deleteFile: Color
    get() = deleteFileLight

@get:Composable
val Colors.modifyFile: Color
    get() = modifyFileLight
