package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.services.IGitProviderService
import javax.inject.Inject

class CleanRepositoriesResourcesUseCase @Inject constructor(
    private val gitProviderService: IGitProviderService,
) {
    operator fun invoke(otherRepositories: List<String>) {
        gitProviderService.cleanupExcept(otherRepositories.toSet())
    }
}