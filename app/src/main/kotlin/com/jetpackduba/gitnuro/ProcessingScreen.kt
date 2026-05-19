package com.jetpackduba.gitnuro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.domain.models.TaskType

@Composable
fun ProcessingScreen(
    processingTask: TaskType,
    onCancelOnGoingTask: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface)
            .onPreviewKeyEvent { true } // Disable all keyboard events
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                getTitle(processingTask),
                style = MaterialTheme.typography.h3,
                color = MaterialTheme.colors.onBackground,
                modifier = Modifier.padding(bottom = 8.dp),
            )
// TODO Does this need to be restored?
//            if (processingState.subtitle.isNotEmpty()) {
//                Text(
//                    processingState.subtitle,
//                    style = MaterialTheme.typography.body1,
//                    color = MaterialTheme.colors.onBackground,
//                    modifier = Modifier.padding(bottom = 32.dp),
//                )
//            }

            LinearProgressIndicator(
                modifier = Modifier.width(280.dp)
                    .padding(bottom = 32.dp),
                color = MaterialTheme.colors.secondary,
            )
        }
    }
}

@Composable
fun getTitle(taskType: TaskType): String {
    return when (taskType) {
        TaskType.AbortRebase -> "Aborting rebase"
        TaskType.AddRemote -> "Adding remote"
        TaskType.AddSubmodule -> "Adding submodule"
        TaskType.AmendCommit -> "Amending commit"
        TaskType.ApplyStash -> "Applying stash"
        TaskType.BlameFile -> "Blaming file"
        TaskType.ChangeBranchUpstream -> "Changing branch upstream"
        TaskType.ChangesDetection -> "Detecting changes"
        TaskType.CheckoutBranch -> "Checking out branch"
        TaskType.CheckoutCommit -> "Checking out commit"
        TaskType.CheckoutRemoteBranch -> "Checking out remote branch"
        TaskType.CheckoutTag -> "Checking out tag"
        TaskType.CherryPickCommit -> "Cherry picking commit"
        TaskType.ContinueRebase -> "Continuing rebase"
        TaskType.CreateBranch -> "Creating branch"
        TaskType.CreateTag -> "Creating tag"
        TaskType.DeinitSubmodule -> "Deinitializing submodule"
        TaskType.DeleteBranch -> "Deleting branch"
        TaskType.DeleteFile -> "Deleting file"
        TaskType.DeleteRemote -> "Deleting remote"
        TaskType.DeleteRemoteBranch -> "Deleting remote branch"
        TaskType.DeleteStash -> "Deleting stash"
        TaskType.DeleteSubmodule -> "Deleting submodule"
        TaskType.DeleteTag -> "Deleting tag"
        TaskType.DiscardFile -> "Discarding file"
        TaskType.DoCommit -> "Committing"
        TaskType.Fetch -> "Fetching"
        TaskType.GetCommitDiffEntries -> "Getting commit diff entries"
        TaskType.GetCommitForRebase -> "Getting commit for rebase"
        TaskType.GetFileCommits -> "Getting file commits"
        TaskType.GetLinesForRebaseInteractive -> "Getting lines for interactive rebase"
        TaskType.HistoryFile -> "Viewing file history"
        TaskType.InitSubmodule -> "Initializing submodule"
        TaskType.LoadAuthor -> "Loading author"
        TaskType.MergeBranch -> "Merging branch"
        TaskType.PersistCommitMessage -> "Saving commit message"
        TaskType.PopStash -> "Popping stash"
        TaskType.Pull -> "Pulling"
        TaskType.PullFromBranch -> "Pulling from branch"
        TaskType.Push -> "Pushing"
        TaskType.PushToBranch -> "Pushing to branch"
        TaskType.RebaseBranch -> "Rebasing branch"
        TaskType.RebaseInteractive -> "Rebasing interactively"
        TaskType.RenameBranch -> "Renaming branch"
        TaskType.RepositoryClone -> "Cloning repository"
        TaskType.RepositoryOpen -> "Opening repository"
        TaskType.ResetRepoState -> "Resetting repository state"
        TaskType.ResetToCommit -> "Resetting to commit"
        TaskType.RevertCommit -> "Reverting commit"
        TaskType.SaveAuthor -> "Saving author"
        TaskType.SaveCustomTheme -> "Saving custom theme"
        TaskType.SkipRebase -> "Skipping rebase"
        TaskType.StageAllFiles -> "Staging all files"
        TaskType.StageDir -> "Staging directory"
        TaskType.StageFile -> "Staging file"
        TaskType.StageHunk -> "Staging hunk"
        TaskType.StageLine -> "Staging line"
        TaskType.Stash -> "Stashing"
        TaskType.SyncSubmodule -> "Synchronizing submodule"
        TaskType.Unspecified -> "No task specified"
        TaskType.UnstageAllFiles -> "Unstaging all files"
        TaskType.UnstageDir -> "Unstaging directory"
        TaskType.UnstageFile -> "Unstaging file"
        TaskType.UnstageHunk -> "Unstaging hunk"
        TaskType.UnstageLine -> "Unstaging line"
        TaskType.UpdateSubmodule -> "Updating submodule"
        TaskType.RefreshBranches -> ""
        TaskType.RefreshLog -> ""
        TaskType.RefreshRemotes -> ""
        TaskType.RefreshRepositoryState -> ""
        TaskType.RefreshStashes -> ""
        TaskType.RefreshStatus -> ""
        TaskType.RefreshSubmodules -> ""
        TaskType.RefreshTags -> ""
        TaskType.GetWorktree -> ""
    }
}
