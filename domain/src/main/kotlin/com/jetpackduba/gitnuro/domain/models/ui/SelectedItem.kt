package com.jetpackduba.gitnuro.domain.models.ui

import org.eclipse.jgit.revwalk.RevCommit

sealed interface SelectedItem {
    data object None : SelectedItem
    data object UncommittedChanges : SelectedItem
    sealed class CommitBasedItem(val revCommit: RevCommit) : SelectedItem
    class Ref(val ref: org.eclipse.jgit.lib.Ref, revCommit: RevCommit) : CommitBasedItem(revCommit)
    class Commit(revCommit: RevCommit) : CommitBasedItem(revCommit)
    class Stash(revCommit: RevCommit) : CommitBasedItem(revCommit)
}