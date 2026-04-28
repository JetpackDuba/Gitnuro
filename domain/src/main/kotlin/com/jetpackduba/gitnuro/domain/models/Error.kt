package com.jetpackduba.gitnuro.domain.models

import com.jetpackduba.gitnuro.domain.exceptions.GitnuroException

data class Error(
    val taskType: TaskType,
    val date: Long,
    val exception: Exception,
    val isUnhandled: Boolean,
) {
    fun errorTitle(): String {
        return when (taskType) {
            TaskType.Unspecified -> "Error"
            TaskType.StageAllFiles -> "Staging all the files failed"
            TaskType.UnstageAllFiles -> "Unstaging all the files failed"
            TaskType.StageFile -> "File stage failed"
            TaskType.UnstageFile -> "File unstage failed"
            TaskType.StageHunk -> "File stage failed"
            TaskType.UnstageHunk -> "Hunk unstage failed"
            TaskType.StageLine -> "File line stage failed"
            TaskType.UnstageLine -> "File line unstage failed"
            TaskType.DiscardFile -> "Discard file failed"
            TaskType.DeleteFile -> "Delete file failed"
            TaskType.BlameFile -> "File blaming failed"
            TaskType.HistoryFile -> "Could not load file history"
            TaskType.DoCommit -> "Commit failed"
            TaskType.AmendCommit -> "Commit amend failed"
            TaskType.RevertCommit -> "Commit revert failed"
            TaskType.CherryPickCommit -> "Commit cherry-pick failed"
            TaskType.CheckoutCommit -> "Checkout commit failed"
            TaskType.ResetToCommit -> "Reset to commit failed"
            TaskType.CheckoutBranch -> "Branch checkout failed"
            TaskType.CheckoutRemoteBranch -> "Remote branch checkout failed"
            TaskType.CreateBranch -> "Could not create the new branch"
            TaskType.DeleteBranch -> "Could not delete the branch"
            TaskType.RenameBranch -> "Could not rename the branch"
            TaskType.MergeBranch -> "Merge failed"
            TaskType.RebaseBranch -> "Rebase failed"
            TaskType.RebaseInteractive -> "Rebase interactive failed"
            TaskType.ContinueRebase -> "Could not continue rebase"
            TaskType.AbortRebase -> "Could not abort rebase"
            TaskType.SkipRebase -> "Could not skip rebase step"
            TaskType.ChangeBranchUpstream -> "Upstream branch change failed"
            TaskType.PullFromBranch -> "Pull from branch failed"
            TaskType.PushToBranch -> "Push to branch failed"
            TaskType.DeleteRemoteBranch -> "Deleting remote branch failed"
            TaskType.Pull -> "Pull failed"
            TaskType.Push -> "Push failed"
            TaskType.Fetch -> "Fetch failed"
            TaskType.Stash -> "Stash failed"
            TaskType.ApplyStash -> "Apply stash failed"
            TaskType.PopStash -> "Pop stash failed"
            TaskType.DeleteStash -> "Delete stash failed"
            TaskType.CreateTag -> "Create tag failed"
            TaskType.CheckoutTag -> "Could not checkout tag's commit"
            TaskType.DeleteTag -> "Could not delete tag"
            TaskType.AddSubmodule -> "Add submodule failed"
            TaskType.DeleteSubmodule -> "Delete submodule failed"
            TaskType.InitSubmodule -> "Init submodule failed"
            TaskType.DeinitSubmodule -> "Deinit submodule failed"
            TaskType.SyncSubmodule -> "Sync submodule failed"
            TaskType.UpdateSubmodule -> "Update submodule failed"
            TaskType.SaveCustomTheme -> "Failed trying to save the custom theme"
            TaskType.ResetRepoState -> "Could not reset repository state"
            TaskType.ChangesDetection -> "Repository changes detection has stopped working"
            TaskType.RepositoryOpen -> "Could not open the repository"
            TaskType.RepositoryClone -> "Could not clone the repository"
            TaskType.AddRemote -> "Adding remote failed"
            TaskType.DeleteRemote -> "Deleting remote failed"
            TaskType.LoadAuthor -> "Loading author failed"
            TaskType.StageDir -> "Staging directory failed"
            TaskType.UnstageDir -> "Unstaging directory failed"
        }
    }
}



fun newErrorNow(
    taskType: TaskType,
    exception: Exception,
): Error {
    return Error(
        taskType = taskType,
        date = System.currentTimeMillis(),
        exception = exception,
        isUnhandled = exception !is GitnuroException
    )
}
