package com.jetpackduba.gitnuro.domain.usecases.repositories.recent

import com.jetpackduba.gitnuro.common.extensions.lockAndUse
import com.jetpackduba.gitnuro.domain.repositories.RecentRepositoriesRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class AddRecentRepositoryUseCase @Inject constructor(
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

            // Remove any previously existing path
            recentRepositoriesPaths.removeIf { it == repositoryPath }

            // Add the latest one to the beginning
            recentRepositoriesPaths.add(0, repositoryPath)

            recentRepositoriesRepository.updateRecentRepository(recentRepositoriesPaths.toSet())
        }
    }
}