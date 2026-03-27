package com.jetpackduba.gitnuro.repositories

import com.jetpackduba.gitnuro.di.TabScope
import com.jetpackduba.gitnuro.extensions.toMutableSetAndAddAll
import com.jetpackduba.gitnuro.git.DiffType
import com.jetpackduba.gitnuro.git.EntryType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject


sealed class DiffSelected(val entries: Set<DiffType>) {
    data class CommitedChanges(
        val items: Set<DiffType.CommitDiff>
    ) : DiffSelected(items)

    data class UncommittedChanges(
        val entryType: EntryType,
        val items: Set<DiffType.UncommittedDiff>
    ) : DiffSelected(items)
}


@TabScope
class SelectedDiffItemRepository @Inject constructor() {
    private val _diffSelected = MutableStateFlow<DiffSelected?>(null)
    val diffSelected: StateFlow<DiffSelected?> = _diffSelected

    fun addDiffCommited(diffType: List<DiffType.CommitDiff>, addToExisting: Boolean = true) {
        val diffSelectedValue = diffSelected.value

        val newDiffSelected =
            if (addToExisting && diffSelectedValue is DiffSelected.CommitedChanges) {
                diffSelectedValue.copy(items = diffSelectedValue.items.toMutableSetAndAddAll(diffType))
            } else {
                DiffSelected.CommitedChanges(diffType.toSet())
            }

        _diffSelected.value = newDiffSelected
    }

    fun addDiffUncommited(
        diffEntries: List<DiffType.UncommittedDiff>,
        addToExisting: Boolean = true,
        entryType: EntryType,
    ) {
        val diffSelectedValue = diffSelected.value

        val newDiffSelected =
            if (addToExisting && diffSelectedValue is DiffSelected.UncommittedChanges && diffSelectedValue.entryType == entryType) {
                diffSelectedValue.copy(items = diffSelectedValue.items.toMutableSetAndAddAll(diffEntries))
            } else {
                DiffSelected.UncommittedChanges(entryType, diffEntries.toSet())
            }


        _diffSelected.value = newDiffSelected
    }

    fun removeSelectedUncommited(selectedToRemove: Set<DiffType.UncommittedDiff>, entryType: EntryType) {
        val diffSelected = this.diffSelected.value

        if (diffSelected is DiffSelected.UncommittedChanges && diffSelected.entryType == entryType) {
            this._diffSelected.value = diffSelected.copy(items = diffSelected.items - selectedToRemove)
        }
    }

    fun removeSelectedCommited(selectedToRemove: Set<DiffType.CommitDiff>) {
        val diffSelected = this.diffSelected.value

        if (diffSelected is DiffSelected.CommitedChanges) {
            this._diffSelected.value = diffSelected.copy(items = diffSelected.items - selectedToRemove)
        }
    }

    fun clearSelection() {
        _diffSelected.value = null
    }
}
