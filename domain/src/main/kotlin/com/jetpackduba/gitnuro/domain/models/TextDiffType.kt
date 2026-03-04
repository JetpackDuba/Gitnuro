package com.jetpackduba.gitnuro.domain.models

enum class TextDiffType(val value: Int) {
    SPLIT(0),
    UNIFIED(1);

    // TODO This should be in the data layer as the domain doesn't care of how this is persisted
    companion object {
        fun fromValue(diffTypeValue: Int): TextDiffType {
            return when (diffTypeValue) {
                TextDiffType.SPLIT.value -> TextDiffType.SPLIT
                TextDiffType.UNIFIED.value -> TextDiffType.UNIFIED
                else -> throw NotImplementedError("Diff type not implemented")
            }
        }
    }
}