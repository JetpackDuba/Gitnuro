package app.ui.context_menu

import androidx.compose.ui.graphics.vector.ImageVector

data class DropDownContentData(
    val label: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit,
)
