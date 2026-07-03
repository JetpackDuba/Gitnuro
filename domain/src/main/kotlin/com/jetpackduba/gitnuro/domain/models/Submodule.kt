package com.jetpackduba.gitnuro.domain.models

data class Submodule(
    val indexId: String,
    val path: String,
    val state: SubmoduleState,
)

enum class SubmoduleState {
    MISSING,
    UNINITIALIZED,
    INITIALIZED,
    REV_CHECKED_OUT;

    val isValid: Boolean
        get() = this == INITIALIZED ||
                this == REV_CHECKED_OUT
}