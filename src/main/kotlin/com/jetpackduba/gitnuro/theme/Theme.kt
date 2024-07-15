@file:Suppress("unused")

package com.jetpackduba.gitnuro.theme

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.ui.dropdowns.DropDownOption

private val defaultAppTheme: ColorsScheme = darkBlueTheme
private var appTheme: ColorsScheme = defaultAppTheme
internal val LocalLinesHeight = compositionLocalOf { normalLineHeight }

class LinesHeight internal constructor(
    val fileHeight: Dp,
    val logCommitHeight: Dp,
    val sidePanelItemHeight: Dp,
)

val normalLineHeight = LinesHeight(
    fileHeight = 40.dp,
    logCommitHeight = 38.dp,
    sidePanelItemHeight = 36.dp
)

val compactLineHeight = LinesHeight(
    fileHeight = 34.dp,
    logCommitHeight = 34.dp,
    sidePanelItemHeight = 34.dp
)

enum class LinesHeightType(val value: Int) {
    NORMAL(0),
    COMPACT(1);

    companion object {
        fun fromInt(value: Int) = entries.first { it.value == value }
    }
}

@Composable
fun AppTheme(
    selectedTheme: Theme = Theme.DARK,
    linesHeightType: LinesHeightType = LinesHeightType.NORMAL,
    customTheme: ColorsScheme?,
    content: @Composable() () -> Unit
) {
    val theme = when (selectedTheme) {
        Theme.LIGHT -> lightTheme
        Theme.DARK -> darkBlueTheme
        Theme.DARK_GRAY -> darkGrayTheme
        Theme.CUSTOM -> customTheme ?: defaultAppTheme
    }

    val lineHeight = when (linesHeightType) {
        LinesHeightType.NORMAL -> normalLineHeight
        LinesHeightType.COMPACT -> compactLineHeight
    }

    appTheme = theme

    val composeColors = theme.toComposeColors()
    val compositionValues = arrayOf(LocalLinesHeight provides lineHeight)

    CompositionLocalProvider(values = compositionValues) {
        MaterialTheme(
            colors = composeColors,
            content = content,
            typography = typography(composeColors),
        )
    }

}

val MaterialTheme.linesHeight: LinesHeight
    @Composable
    @ReadOnlyComposable
    get() = LocalLinesHeight.current

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

val Colors.diffKeyword: Color
    get() = appTheme.diffKeyword

val Colors.diffAnnotation: Color
    get() = appTheme.diffAnnotation

val Colors.diffComment: Color
    get() = appTheme.diffComment

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