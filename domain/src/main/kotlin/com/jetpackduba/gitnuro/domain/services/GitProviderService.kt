package com.jetpackduba.gitnuro.domain.services

interface IGitProviderService {
    fun cleanupExcept(repositoriesToKeep: Set<String>)
}