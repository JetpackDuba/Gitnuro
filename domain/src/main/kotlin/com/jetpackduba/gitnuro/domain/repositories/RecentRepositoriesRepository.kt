package com.jetpackduba.gitnuro.domain.repositories

import kotlinx.coroutines.flow.Flow

interface RecentRepositoriesRepository {
    val recentRepositoriesPaths: Flow<Set<String>>
    suspend fun updateRecentRepository(repositoriesPaths: Set<String>)
}