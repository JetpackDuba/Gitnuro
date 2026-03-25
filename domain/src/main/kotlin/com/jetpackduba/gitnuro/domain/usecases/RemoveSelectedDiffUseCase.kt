package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.models.DiffSelected
import com.jetpackduba.gitnuro.domain.models.DiffType
import com.jetpackduba.gitnuro.domain.models.EntryType
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

class RemoveSelectedDiffUseCase @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
) {
    operator fun invoke(selectedToRemove: Set<DiffType.CommitDiff>) {
        val diffSelected = repositoryDataRepository.diffSelected.value

        if (diffSelected is DiffSelected.CommitedChanges) {
            val newDiffSelected = diffSelected.copy(items = diffSelected.items - selectedToRemove)
            repositoryDataRepository.updateDiffSelected(newDiffSelected)
        }
    }

    operator fun invoke(selectedToRemove: Set<DiffType.UncommittedDiff>, entryType: EntryType) {
        val diffSelected = repositoryDataRepository.diffSelected.value

        if (diffSelected is DiffSelected.UncommittedChanges && diffSelected.entryType == entryType) {
            val newDiffSelected = diffSelected.copy(items = diffSelected.items - selectedToRemove)
            repositoryDataRepository.updateDiffSelected(newDiffSelected)
        }
    }
}