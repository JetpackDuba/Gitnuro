package com.jetpackduba.gitnuro.domain.usecases.repositories.recent

import com.jetpackduba.gitnuro.common.extensions.lockAndUse
import com.jetpackduba.gitnuro.domain.models.GlobalConfig
import com.jetpackduba.gitnuro.domain.repositories.RecentRepositoriesRepository
import com.jetpackduba.gitnuro.domain.services.GlobalSettingsService
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import kotlin.collections.orEmpty
import kotlin.collections.toMutableList

class RemoveRecentRepositoryUseCase @Inject constructor(
    private val recentRepositoriesRepository: RecentRepositoriesRepository,
    private val recentRepositoriesMutex: RecentRepositoriesMutex
) {
    suspend operator fun invoke(repositoryPath: String)  {
        recentRepositoriesMutex.lockAndUse {
            val recentRepositoriesPaths = recentRepositoriesRepository
                .recentRepositoriesPaths
                .firstOrNull()
                .orEmpty()
                .toMutableList()

            recentRepositoriesPaths.removeIf { it == repositoryPath }

            recentRepositoriesRepository.updateRecentRepository(recentRepositoriesPaths.toSet())
        }
    }
}