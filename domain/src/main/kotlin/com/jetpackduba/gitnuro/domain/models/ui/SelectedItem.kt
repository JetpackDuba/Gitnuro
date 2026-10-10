package com.jetpackduba.gitnuro.domain.models.ui

import androidx.compose.runtime.Immutable
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.models.Tag

@Immutable
sealed interface SelectedItem {
    @Immutable
    data object None : SelectedItem

    @Immutable
    data object UncommittedChanges : SelectedItem

    @Immutable
    sealed interface CommitBasedItem : SelectedItem {
        val commit: Commit
    }

    @Immutable
    data class BranchItem(val branch: Branch, override val commit: Commit) : CommitBasedItem

    @Immutable
    data class TagItem(val tag: Tag, override val commit: Commit) : CommitBasedItem

    @Immutable
    data class CommitItem(override val commit: Commit, val isStash: Boolean, val scrollToItem: Boolean) : CommitBasedItem
}