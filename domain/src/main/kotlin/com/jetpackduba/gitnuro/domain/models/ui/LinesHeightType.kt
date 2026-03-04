package com.jetpackduba.gitnuro.domain.models.ui

enum class LinesHeightType(val value: Int) {
    SPACED(0),
    COMPACT(1);

    // TODO This should be in the data layer as the domain doesn't care of how this is persisted
    companion object {
        fun fromInt(value: Int) = entries.first { it.value == value }
    }
}