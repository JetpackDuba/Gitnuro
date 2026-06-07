package com.jetpackduba.gitnuro.repositoryopen

import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.models.Tag

sealed interface LogAction {
    data class Merge(val branch: Branch) : LogAction
    data class Rebase(val branch: Branch) : LogAction
    data class DeleteBranch(val branch: Branch) : LogAction
    data class CheckoutCommit(val commit: Commit) : LogAction
    data class RevertCommit(val commit: Commit) : LogAction
    data class CherryPickCommit(val commit: Commit) : LogAction
    data class CheckoutRemoteBranch(val branch: Branch) : LogAction
    data class CheckoutBranch(val branch: Branch) : LogAction
    data class RebaseInteractive(val commit: Commit) : LogAction
    data class CommitSelected(val commit: Commit) : LogAction
    data object UncommittedChangesSelected : LogAction
    data class DeleteStash(val commit: Commit) : LogAction
    data class ApplyStash(val commit: Commit) : LogAction
    data class PopStash(val commit: Commit) : LogAction
    data class CheckoutTag(val tag: Tag) : LogAction
    data class DeleteRemoteBranch(val branch: Branch) : LogAction
    data class DeleteTag(val tag: Tag) : LogAction
    data class PushToRemoteBranch(val branch: Branch) : LogAction
    data class PullFromRemoteBranch(val branch: Branch) : LogAction
    data class SearchValueChange(val filter: String) : LogAction
}