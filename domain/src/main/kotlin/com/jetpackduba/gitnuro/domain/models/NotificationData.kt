package com.jetpackduba.gitnuro.domain.models


//data class Notification(val type: NotificationType, val text: String)
data class NotificationData(val type: NotificationType, val content: String)

enum class NotificationType {
    Warning,
    Positive,
    Error,
}

fun positiveNotification(text: String) = NotificationData(NotificationType.Positive, text)
fun errorNotification(text: String) = NotificationData(NotificationType.Error, text)
fun warningNotification(text: String) = NotificationData(NotificationType.Warning, text)


fun TaskType.successTitle(): String? {
    return when (this) {
        TaskType.StageLine -> "File line staged"
        TaskType.UnstageLine -> "File line unstaged"
        TaskType.DiscardFile -> "File discarded"
        TaskType.DeleteFile -> "File deleted"
        TaskType.AmendCommit -> "Commit amended"
        TaskType.RevertCommit -> "Commit reverted"
        TaskType.CherryPickCommit -> "Commit cherry-picked"
        TaskType.CheckoutCommit -> "Commit checked out"
        TaskType.ResetToCommit -> "Reset to commit completed"
        TaskType.CheckoutBranch -> "Branch checked out"
        TaskType.CheckoutRemoteBranch -> "Remote branch checked out"
        TaskType.CreateBranch -> "Branch created"
        TaskType.DeleteBranch -> "Branch deleted"
        TaskType.RenameBranch -> "Branch renamed"
        TaskType.MergeBranch -> "Merge completed"
        TaskType.RebaseBranch -> "Rebase completed"
        TaskType.RebaseInteractive -> "Interactive rebase completed"
        TaskType.ContinueRebase -> "Rebase continued"
        TaskType.AbortRebase -> "Rebase aborted"
        TaskType.SkipRebase -> "Rebase step skipped"
        TaskType.ChangeBranchUpstream -> "Upstream branch changed"
        TaskType.PullFromBranch -> "Pulled from branch"
        TaskType.PushToBranch -> "Pushed to branch"
        TaskType.DeleteRemoteBranch -> "Remote branch deleted"
        TaskType.Pull -> "Pull completed"
        TaskType.Push -> "Push completed"
        TaskType.Fetch -> "Fetch completed"
        TaskType.Stash -> "Changes stashed"
        TaskType.ApplyStash -> "Stash applied"
        TaskType.PopStash -> "Stash popped"
        TaskType.DeleteStash -> "Stash deleted"
        TaskType.CreateTag -> "Tag created"
        TaskType.CheckoutTag -> "Tag checked out"
        TaskType.DeleteTag -> "Tag deleted"
        TaskType.AddSubmodule -> "Submodule added"
        TaskType.DeleteSubmodule -> "Submodule deleted"
        TaskType.InitSubmodule -> "Submodule initialized"
        TaskType.DeinitSubmodule -> "Submodule deinitialized"
        TaskType.SyncSubmodule -> "Submodule synchronized"
        TaskType.UpdateSubmodule -> "Submodule updated"
        TaskType.ResetRepoState -> "Repository state reset"
        TaskType.AddRemote -> "Remote added"
        TaskType.DeleteRemote -> "Remote deleted"
        TaskType.SaveAuthor -> "Author updated"
        TaskType.UpdateRemote -> "Remote updated"
        else -> null
    }
}