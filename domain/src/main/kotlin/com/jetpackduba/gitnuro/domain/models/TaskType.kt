package com.jetpackduba.gitnuro.domain.models

sealed interface TaskType {
    data object Unspecified : TaskType
    data object StageAllFiles : TaskType
    data class StageDir(val paths: List<String>) : TaskType
    data class StageFile(val paths: List<String>) : TaskType
    data object StageHunk : TaskType
    data object StageLine : TaskType
    data object UnstageAllFiles : TaskType
    data class UnstageDir(val paths: List<String>) : TaskType
    data class UnstageFile(val paths: List<String>) : TaskType
    data object UnstageHunk : TaskType
    data object UnstageLine : TaskType
    data class DiscardFile(val paths: List<String>) : TaskType
    data class DeleteFile(val paths: List<String>) : TaskType
    data object BlameFile : TaskType
    data object HistoryFile : TaskType
    data object DoCommit : TaskType
    data object RevertCommit : TaskType
    data object CherryPickCommit : TaskType
    data object CheckoutCommit : TaskType
    data object ResetToCommit : TaskType
    data object CheckoutBranch : TaskType
    data object CheckoutRemoteBranch : TaskType
    data object CreateBranch : TaskType
    data object DeleteBranch : TaskType
    data object RenameBranch : TaskType
    data object MergeBranch : TaskType
    data object RebaseBranch : TaskType
    data object RebaseInteractive : TaskType
    data object ContinueRebase : TaskType
    data object AbortRebase : TaskType
    data object SkipRebase : TaskType
    data object ChangeBranchUpstream : TaskType
    data object DeleteRemoteBranch : TaskType
    data class Pull(val remoteBranch: Branch?) : TaskType
    data class Push(val remoteBranch: Branch?) : TaskType
    data object Fetch : TaskType
    data object Stash : TaskType
    data object ApplyStash : TaskType
    data object PopStash : TaskType
    data object DeleteStash : TaskType
    data object CreateTag : TaskType
    data object CheckoutTag : TaskType
    data object DeleteTag : TaskType
    data object AddSubmodule : TaskType
    data object DeleteSubmodule : TaskType
    data object InitSubmodule : TaskType
    data object DeinitSubmodule : TaskType
    data object SyncSubmodule : TaskType
    data object UpdateSubmodule : TaskType
    data object SaveCustomTheme : TaskType
    data object ResetRepoState : TaskType
    data object ChangesDetection : TaskType
    data object RepositoryOpen : TaskType
    data object RepositoryClone : TaskType
    data object AddRemote : TaskType
    data object UpdateRemote : TaskType
    data object DeleteRemote : TaskType
    data object LoadAuthor : TaskType
    data object SaveAuthor : TaskType
    data object GetCommitForRebase : TaskType
    data object GetLinesForRebaseInteractive : TaskType
    data object GetFileCommits : TaskType
    data object GetCommitDiffEntries : TaskType
    data object RefreshLog : TaskType
    data object RefreshBranches : TaskType
    data object RefreshStatus : TaskType
    data object RefreshRepositoryState : TaskType
    data object RefreshRemotes : TaskType
    data object RefreshSubmodules : TaskType
    data object RefreshTags : TaskType
    data object RefreshStashes : TaskType
    data object GetWorktree : TaskType
}