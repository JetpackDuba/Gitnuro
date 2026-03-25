package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.extensions.toMutableSetAndAddAll
import com.jetpackduba.gitnuro.domain.models.DiffSelected
import com.jetpackduba.gitnuro.domain.models.DiffType
import com.jetpackduba.gitnuro.domain.models.EntryType
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

class AddSelectedDiffUseCase @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
) {
    operator fun invoke(diffType: List<DiffType.CommitDiff>, addToExisting: Boolean) {
        val diffSelectedValue = repositoryDataRepository.diffSelected.value

        val newDiffSelected =
            if (addToExisting && diffSelectedValue is DiffSelected.CommitedChanges) {
                diffSelectedValue.copy(items = diffSelectedValue.items.toMutableSetAndAddAll(diffType))
            } else {
                DiffSelected.CommitedChanges(diffType.toSet())
            }

        repositoryDataRepository.updateDiffSelected(newDiffSelected)
    }

    operator fun invoke(
        diffEntries: List<DiffType.UncommittedDiff>,
        addToExisting: Boolean,
        entryType: EntryType,
    ) {
        val diffSelectedValue = repositoryDataRepository.diffSelected.value

        val newDiffSelected =
            if (addToExisting && diffSelectedValue is DiffSelected.UncommittedChanges && diffSelectedValue.entryType == entryType) {
                diffSelectedValue.copy(items = diffSelectedValue.items.toMutableSetAndAddAll(diffEntries))
            } else {
                DiffSelected.UncommittedChanges(entryType, diffEntries.toSet())
            }

        repositoryDataRepository.updateDiffSelected(newDiffSelected)
    }
}