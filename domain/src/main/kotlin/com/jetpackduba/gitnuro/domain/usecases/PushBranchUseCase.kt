package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.extensions.runOperationInTabScope
import com.jetpackduba.gitnuro.domain.interfaces.IPushBranchGitAction
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import com.jetpackduba.gitnuro.domain.services.AppSettingsService
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class PushBranchUseCase @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
    private val repositoryStateRepository: RepositoryStateRepository,
    private val pushBranchGitAction: IPushBranchGitAction,
    private val appSettingsService: AppSettingsService,
    private val tabScope: TabCoroutineScope,
    private val refreshLogUseCase: RefreshLogUseCase,
) {
    operator fun invoke(force: Boolean, pushTags: Boolean) {
        val repositoryPath = repositoryDataRepository.repositoryPath ?: return

        repositoryStateRepository.runOperationInTabScope(tabScope) {
            val pushWithLease = appSettingsService.pushWithLease.first()

            pushBranchGitAction(repositoryPath, force, pushTags, pushWithLease) // TODO Fix this after refactor to use cases
        }

        refreshLogUseCase()
    }
}