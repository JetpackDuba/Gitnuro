package com.jetpackduba.gitnuro.domain.models.ui

// TODO Display name should be a UI responsibility and not domain
enum class Theme(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    DARK_GRAY("Dark gray"),
    CUSTOM("Custom");
}