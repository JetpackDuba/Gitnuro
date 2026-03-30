package com.jetpackduba.gitnuro.domain.models.ui

sealed interface SelectedItem {
    data object None : SelectedItem
    data object UncommittedChanges : SelectedItem
    sealed class CommitBasedItem(val commit: com.jetpackduba.gitnuro.domain.models.Commit) : SelectedItem
    class Ref(val ref: org.eclipse.jgit.lib.Ref, revCommit: com.jetpackduba.gitnuro.domain.models.Commit) : CommitBasedItem(revCommit)
    class Commit(revCommit: com.jetpackduba.gitnuro.domain.models.Commit) : CommitBasedItem(revCommit)
    class Stash(revCommit: com.jetpackduba.gitnuro.domain.models.Commit) : CommitBasedItem(revCommit)
}