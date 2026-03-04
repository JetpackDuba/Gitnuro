package com.jetpackduba.gitnuro.domain.repositories

interface ClipboardRepository {
    fun copy(content: String)
}