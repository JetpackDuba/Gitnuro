package app.ui.context_menu

data class DropDownContentData(
    val label: String,
    val icon: String? = null,
    val onClick: () -> Unit,
)
