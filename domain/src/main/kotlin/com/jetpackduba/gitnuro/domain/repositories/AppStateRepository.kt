package com.jetpackduba.gitnuro.domain.repositories

interface AppStateRepository {
    val isProcessing: Boolean
    val repositoryPath: String?
}