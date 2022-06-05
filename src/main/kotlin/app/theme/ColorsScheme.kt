package app.theme

import androidx.compose.material.Colors
import androidx.compose.ui.graphics.Color

data class ColorsScheme(
    val primary: Color,
    val primaryVariant: Color,
    val onPrimary: Color,
    val secondary: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val error: Color,
    val onError: Color,
    val background: Color,
    val backgroundSelected: Color,
    val surface: Color,
    val headerBackground: Color,
    val onHeader: Color = primaryText,
    val borderColor: Color,
    val graphHeaderBackground: Color,
    val addFile: Color,
    val deletedFile: Color,
    val modifiedFile: Color,
    val conflictingFile: Color,
    val dialogOverlay: Color,
    val normalScrollbar: Color,
    val hoverScrollbar: Color,
) {
    fun toComposeColors(): Colors {
        return Colors(
            primary = this.primary,
            primaryVariant = this.primaryVariant,
            secondary = this.secondary,
            secondaryVariant = this.secondary,
            background = this.background,
            surface = this.surface,
            error = this.error,
            onPrimary = this.onPrimary,
            onSecondary = this.onPrimary,
            onBackground = this.primaryText,
            onSurface = this.primaryText,
            onError = this.onError,
            isLight = true, // todo what is this used for? Hardcoded value for now
        )
    }
}