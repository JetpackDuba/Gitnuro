package app.ui.context_menu

import androidx.compose.ui.graphics.painter.Painter

data class DropDownContentData(
    val label: String,
    val icon: String? = null,
    val onClick: () -> Unit,
)
