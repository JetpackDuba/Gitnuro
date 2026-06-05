package com.jetpackduba.gitnuro.domain.models.ui

import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.models.Tag

sealed interface SelectedItem {
    data object None : SelectedItem
    data object UncommittedChanges : SelectedItem
    sealed interface CommitBasedItem : SelectedItem {
        val commit: Commit
    }

    data class BranchItem(val branch: Branch, override val commit: Commit) : CommitBasedItem
    data class TagItem(val tag: Tag, override val commit: Commit) : CommitBasedItem
    data class CommitItem(override val commit: Commit, val isStash: Boolean) : CommitBasedItem
}