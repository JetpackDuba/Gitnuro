package com.jetpackduba.gitnuro.domain.models

// TODO rename to LfsOperationType after refactoring
enum class OperationType(val value: String) {
    UPLOAD("upload"),
    DOWNLOAD("download")
}