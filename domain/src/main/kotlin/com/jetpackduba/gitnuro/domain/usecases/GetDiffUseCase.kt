package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.common.extensions.TAG
import com.jetpackduba.gitnuro.common.printError
import com.jetpackduba.gitnuro.domain.errors.okOrNull
import com.jetpackduba.gitnuro.domain.exceptions.MissingDiffEntryException
import com.jetpackduba.gitnuro.domain.interfaces.IFormatDiffGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGenerateSplitHunkFromDiffResultGitAction
import com.jetpackduba.gitnuro.domain.models.DiffResult
import com.jetpackduba.gitnuro.domain.models.DiffTextViewType
import com.jetpackduba.gitnuro.domain.models.DiffType
import com.jetpackduba.gitnuro.domain.models.ViewDiffResult
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.services.AppSettingsService
import kotlinx.coroutines.flow.first
import org.eclipse.jgit.diff.DiffEntry
import javax.inject.Inject

class GetDiffUseCase @Inject constructor(
    private val formatDiffGitAction: IFormatDiffGitAction,
    private val generateSplitHunkFromDiffResultGitAction: IGenerateSplitHunkFromDiffResultGitAction,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val settings: AppSettingsService,
    private val refreshStatusUseCase: RefreshStatusUseCase,
) {
    suspend operator fun invoke(diffType: DiffType): ViewDiffResult {
        val repositoryPath = repositoryDataRepository.repositoryPath ?: return ViewDiffResult.None
        val diffViewType = settings.diffTextViewType.first()
        val isDisplayFullFile = settings.diffDisplayFullFile.first()

        return try {
            val diffFormat = formatDiffGitAction(repositoryPath, diffType, isDisplayFullFile).okOrNull()!!
            val diffEntry = diffFormat.diffEntry
            if (
                diffViewType == DiffTextViewType.Split &&
                diffFormat is DiffResult.Text &&
                diffEntry.changeType != DiffEntry.ChangeType.ADD &&
                diffEntry.changeType != DiffEntry.ChangeType.DELETE
            ) {
                val splitHunkList = generateSplitHunkFromDiffResultGitAction(diffFormat)
                ViewDiffResult.Loaded(
                    diffType,
                    DiffResult.TextSplit(diffEntry, splitHunkList)
                )
            } else {
                ViewDiffResult.Loaded(diffType, diffFormat)
            }

        } catch (ex: Exception) {
            printError(TAG, ex.message.orEmpty(), ex)

            if (ex is MissingDiffEntryException) {
                refreshStatusUseCase
            } else {
                ex.printStackTrace()
            }

            ViewDiffResult.DiffNotFound(diffType)
        }
    }
}