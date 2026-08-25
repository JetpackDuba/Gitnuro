package com.jetpackduba.gitnuro.di.modules

import com.jetpackduba.gitnuro.common.TabScope
import com.jetpackduba.gitnuro.data.git.GetWorktreePathGitAction
import com.jetpackduba.gitnuro.data.git.author.LoadAuthorGitAction
import com.jetpackduba.gitnuro.data.git.author.SaveAuthorGitAction
import com.jetpackduba.gitnuro.data.git.branches.*
import com.jetpackduba.gitnuro.data.git.config.LoadSignOffConfigGitAction
import com.jetpackduba.gitnuro.data.git.config.SaveLocalRepositoryConfigGitAction
import com.jetpackduba.gitnuro.data.git.diff.*
import com.jetpackduba.gitnuro.data.git.lfs.*
import com.jetpackduba.gitnuro.data.git.log.*
import com.jetpackduba.gitnuro.data.git.rebase.*
import com.jetpackduba.gitnuro.data.git.remote_operations.*
import com.jetpackduba.gitnuro.data.git.remotes.AddRemoteGitAction
import com.jetpackduba.gitnuro.data.git.remotes.DeleteRemoteGitAction
import com.jetpackduba.gitnuro.data.git.remotes.GetRemotesGitAction
import com.jetpackduba.gitnuro.data.git.remotes.UpdateRemoteGitAction
import com.jetpackduba.gitnuro.data.git.repository.*
import com.jetpackduba.gitnuro.data.git.stash.*
import com.jetpackduba.gitnuro.data.git.submodules.*
import com.jetpackduba.gitnuro.data.git.tags.CreateTagGitAction
import com.jetpackduba.gitnuro.data.git.tags.DeleteTagGitAction
import com.jetpackduba.gitnuro.data.git.tags.GetTagsGitAction
import com.jetpackduba.gitnuro.data.git.workspace.*
import com.jetpackduba.gitnuro.data.log.GetFileCommitsAction
import com.jetpackduba.gitnuro.domain.GraphRevWalker
import com.jetpackduba.gitnuro.domain.interfaces.*
import dagger.Binds
import dagger.Module

@Module
interface TabScopeGitActionsModule {

    @Binds
    @TabScope
    fun bindsAbortRebaseGitAction(action: AbortRebaseGitAction): IAbortRebaseGitAction

    @Binds
    @TabScope
    fun bindsAddRemoteGitAction(action: AddRemoteGitAction): IAddRemoteGitAction

    @Binds
    @TabScope
    fun bindsAddSubmoduleGitAction(action: AddSubmoduleGitAction): IAddSubmoduleGitAction

    @Binds
    @TabScope
    fun bindsApplyStashGitAction(action: ApplyStashGitAction): IApplyStashGitAction

    @Binds
    @TabScope
    fun bindsAuthenticateLfsServerWithSshGitAction(action: AuthenticateLfsServerWithSshGitAction): IAuthenticateLfsServerWithSshGitAction

    @Binds
    @TabScope
    fun bindsBlameFileGitAction(action: BlameFileGitAction): IBlameFileGitAction

    @Binds
    @TabScope
    fun bindsCanGenerateTextDiffGitAction(action: CanGenerateTextDiffGitAction): ICanGenerateTextDiffGitAction

    @Binds
    @TabScope
    fun bindsCheckHasPreviousCommitsGitAction(action: CheckHasPreviousCommitsGitAction): ICheckHasPreviousCommitsGitAction

    @Binds
    @TabScope
    fun bindsCheckHasUncommittedChangesGitAction(action: CheckHasUncommittedChangesGitAction): ICheckHasUncommittedChangesGitAction

    @Binds
    @TabScope
    fun bindsCheckoutCommitGitAction(action: CheckoutCommitGitAction): ICheckoutCommitGitAction

    @Binds
    @TabScope
    fun bindsCheckoutRefGitAction(action: CheckoutBranchGitAction): ICheckoutBranchGitAction

    @Binds
    @TabScope
    fun bindsCherryPickCommitGitAction(action: CherryPickCommitGitAction): ICherryPickCommitGitAction

    @Binds
    @TabScope
    fun bindsCloneRepositoryGitAction(action: CloneRepositoryGitAction): ICloneRepositoryGitAction

    @Binds
    @TabScope
    fun bindsContinueRebaseGitAction(action: ContinueRebaseGitAction): IContinueRebaseGitAction

    @Binds
    @TabScope
    fun bindsCreateBranchGitAction(action: CreateBranchGitAction): ICreateBranchGitAction

    @Binds
    @TabScope
    fun bindsCreateSnapshotStashGitAction(action: CreateSnapshotStashGitAction): ICreateSnapshotStashGitAction

    @Binds
    @TabScope
    fun bindsCreateTagGitAction(action: CreateTagGitAction): ICreateTagGitAction

    @Binds
    @TabScope
    fun bindsDeInitializeSubmoduleGitAction(action: DeInitializeSubmoduleGitAction): IDeInitializeSubmoduleGitAction

    @Binds
    @TabScope
    fun bindsDeleteBranchGitAction(action: DeleteBranchGitAction): IDeleteBranchGitAction

    @Binds
    @TabScope
    fun bindsDeleteFileGitAction(action: DeleteFileGitAction): IDeleteFileGitAction

    @Binds
    @TabScope
    fun bindsDeleteLocallyRemoteBranchesGitAction(action: DeleteLocallyRemoteBranchesGitAction): IDeleteLocallyRemoteBranchesGitAction

    @Binds
    @TabScope
    fun bindsDeleteRemoteBranchGitAction(action: DeleteRemoteBranchGitAction): IDeleteRemoteBranchGitAction

    @Binds
    @TabScope
    fun bindsDeleteRemoteGitAction(action: DeleteRemoteGitAction): IDeleteRemoteGitAction

    @Binds
    @TabScope
    fun bindsDeleteStashGitAction(action: DeleteStashGitAction): IDeleteStashGitAction

    @Binds
    @TabScope
    fun bindsDeleteSubmoduleGitAction(action: DeleteSubmoduleGitAction): IDeleteSubmoduleGitAction

    @Binds
    @TabScope
    fun bindsDeleteTagGitAction(action: DeleteTagGitAction): IDeleteTagGitAction

    @Binds
    @TabScope
    fun bindsDiscardEntriesGitAction(action: DiscardEntriesGitAction): IDiscardEntriesGitAction

    @Binds
    @TabScope
    fun bindsDiscardUnstagedHunkLineGitAction(action: DiscardUnstagedHunkLineGitAction): IDiscardUnstagedHunkLineGitAction

    @Binds
    @TabScope
    fun bindsDoCommitGitAction(action: DoCommitGitAction): IDoCommitGitAction

    @Binds
    @TabScope
    fun bindsDownloadLfsObjectGitAction(action: DownloadLfsObjectGitAction): IDownloadLfsObjectGitAction

    @Binds
    @TabScope
    fun bindsFetchAllRemotesGitAction(action: FetchAllRemotesGitAction): IFetchAllRemotesGitAction

    @Binds
    @TabScope
    fun bindsFindCommitGitAction(action: FindCommitGitAction): IFindCommitGitAction

    @Binds
    @TabScope
    fun bindsFormatDiffGitAction(action: FormatDiffGitAction): IFormatDiffGitAction

    @Binds
    @TabScope
    fun bindsFormatHunksGitAction(action: FormatHunksGitAction): IFormatHunksGitAction

    @Binds
    @TabScope
    fun bindsGenerateSplitHunkFromDiffResultGitAction(action: GenerateSplitHunkFromDiffResultGitAction): IGenerateSplitHunkFromDiffResultGitAction

    @Binds
    @TabScope
    fun bindsGetBranchesGitAction(action: GetBranchesGitAction): IGetBranchesGitAction

    @Binds
    @TabScope
    fun bindsGetCurrentBranchGitAction(action: GetCurrentBranchGitAction): IGetCurrentBranchGitAction

    @Binds
    @TabScope
    fun bindsGetCommitDiffEntriesGitAction(action: GetCommitDiffEntriesGitAction): IGetCommitDiffEntriesGitAction

    @Binds
    @TabScope
    fun bindsGetCommitFromHashGitAction(action: GetCommitFromHashGitAction): IGetCommitFromHashGitAction

    @Binds
    @TabScope
    fun bindsGetCommitFromRebaseLineGitAction(action: GetCommitFromRebaseLineGitAction): IGetCommitFromRebaseLineGitAction

    @Binds
    @TabScope
    fun bindsGetDiffContentGitAction(action: GetDiffContentGitAction): IGetDiffContentGitAction

    @Binds
    @TabScope
    fun bindsGetDiffEntryFromStatusEntryGitAction(action: GetDiffEntryFromStatusEntryGitAction): IGetDiffEntryFromStatusEntryGitAction

    @Binds
    @TabScope
    fun bindsGetIgnoreRulesGitAction(action: GetIgnoreRulesGitAction): IGetIgnoreRulesGitAction

    @Binds
    @TabScope
    fun bindsGetSpecificCommitMessageGitAction(action: GetSpecificCommitMessageGitAction): IGetSpecificCommitMessageGitAction

    @Binds
    @TabScope
    fun bindsGetLastCommitMessageGitAction(action: GetLastCommitMessageGitAction): IGetLastCommitMessageGitAction

    @Binds
    @TabScope
    fun bindsGetLfsObjectsGitAction(action: GetLfsObjectsGitAction): IGetLfsObjectsGitAction

    @Binds
    @TabScope
    fun bindsGetLinesFromRawTextGitAction(action: GetLinesFromRawTextGitAction): IGetLinesFromRawTextGitAction

    @Binds
    @TabScope
    fun bindsGetLinesFromTextGitAction(action: GetLinesFromTextGitAction): IGetLinesFromTextGitAction

    @Binds
    @TabScope
    fun bindsGetRebaseAmendCommitIdGitAction(action: GetRebaseAmendCommitIdGitAction): IGetRebaseAmendCommitIdGitAction

    @Binds
    @TabScope
    fun bindsGetRebaseInteractiveStateGitAction(action: GetRebaseInteractiveStateGitAction): IGetRebaseInteractiveStateGitAction

    @Binds
    @TabScope
    fun bindsGetRebaseInteractiveTodoLinesGitAction(action: GetRebaseInteractiveTodoLinesGitAction): IGetRebaseInteractiveTodoLinesGitAction

    @Binds
    @TabScope
    fun bindsGetRemoteBranchesGitAction(action: GetRemoteBranchesGitAction): IGetRemoteBranchesGitAction

    @Binds
    @TabScope
    fun bindsGetRemotesGitAction(action: GetRemotesGitAction): IGetRemotesGitAction

    @Binds
    @TabScope
    fun bindsGetRepositoryStateGitAction(action: GetRepositoryStateGitAction): IGetRepositoryStateGitAction

    @Binds
    @TabScope
    fun bindsGetStashListGitAction(action: GetStashListGitAction): IGetStashListGitAction

    @Binds
    @TabScope
    fun bindsGetStatusGitAction(action: GetStatusGitAction): IGetStatusGitAction

    @Binds
    @TabScope
    fun bindsGetTagsGitAction(action: GetTagsGitAction): IGetTagsGitAction

    @Binds
    @TabScope
    fun bindsGetWorktreePathGitAction(action: GetWorktreePathGitAction): IGetWorktreePathGitAction

    @Binds
    @TabScope
    fun bindsGetSubmodulesGitAction(action: GetSubmodulesGitAction): IGetSubmodulesGitAction

    @Binds
    @TabScope
    fun bindsGetTrackingBranchGitAction(action: GetTrackingBranchGitAction): IGetTrackingBranchGitAction

    @Binds
    @TabScope
    fun bindsHasPullResultConflictsGitAction(action: HasPullResultConflictsGitAction): IHasPullResultConflictsGitAction

    @Binds
    @TabScope
    fun bindsInitializeAllSubmodulesGitAction(action: InitializeAllSubmodulesGitAction): IInitializeAllSubmodulesGitAction

    @Binds
    @TabScope
    fun bindsInitializeSubmoduleGitAction(action: InitializeSubmoduleGitAction): IInitializeSubmoduleGitAction

    @Binds
    @TabScope
    fun bindsInitLocalRepositoryGitAction(action: InitLocalRepositoryGitAction): IInitLocalRepositoryGitAction

    @Binds
    @TabScope
    fun bindsLoadAuthorGitAction(action: LoadAuthorGitAction): ILoadAuthorGitAction

    @Binds
    @TabScope
    fun bindsLoadSignOffConfigGitAction(action: LoadSignOffConfigGitAction): ILoadSignOffConfigGitAction

    @Binds
    @TabScope
    fun bindsMergeBranchGitAction(action: MergeBranchGitAction): IMergeBranchGitAction

    @Binds
    @TabScope
    fun bindsOpenRepositoryGitAction(action: OpenRepositoryGitAction): IOpenRepositoryGitAction

    @Binds
    @TabScope
    fun bindsPopStashGitAction(action: PopStashGitAction): IPopStashGitAction

    @Binds
    @TabScope
    fun bindsPersistCommitMessageGitAction(action: PersistCommitMessageGitAction): IPersistCommitMessageGitAction

    @Binds
    @TabScope
    fun bindsProvideLfsCredentialsGitAction(action: ProvideLfsCredentialsGitAction): IProvideLfsCredentialsGitAction

    @Binds
    @TabScope
    fun bindsPullBranchGitAction(action: PullBranchGitAction): IPullBranchGitAction

    @Binds
    @TabScope
    fun bindsPushBranchGitAction(action: PushBranchGitAction): IPushBranchGitAction

    @Binds
    @TabScope
    fun bindsRebaseBranchGitAction(action: RebaseBranchGitAction): IRebaseBranchGitAction

    @Binds
    @TabScope
    fun bindsRenameBranchGitAction(action: RenameBranchGitAction): IRenameBranchGitAction

    @Binds
    @TabScope
    fun bindsResetHunkGitAction(action: ResetHunkGitAction): IResetHunkGitAction

    @Binds
    @TabScope
    fun bindsResetRepositoryStateGitAction(action: ResetRepositoryStateGitAction): IResetRepositoryStateGitAction

    @Binds
    @TabScope
    fun bindsResetToCommitGitAction(action: ResetToCommitGitAction): IResetToCommitGitAction

    @Binds
    @TabScope
    fun bindsResumeRebaseInteractiveGitAction(action: ResumeRebaseInteractiveGitAction): IResumeRebaseInteractiveGitAction

    @Binds
    @TabScope
    fun bindsRevertCommitGitAction(action: RevertCommitGitAction): IRevertCommitGitAction

    @Binds
    @TabScope
    fun bindsSaveAuthorGitAction(action: SaveAuthorGitAction): ISaveAuthorGitAction

    @Binds
    @TabScope
    fun bindsSaveLocalRepositoryConfigGitAction(action: SaveLocalRepositoryConfigGitAction): ISaveLocalRepositoryConfigGitAction

    @Binds
    @TabScope
    fun bindsSetTrackingBranchGitAction(action: SetTrackingBranchGitAction): ISetTrackingBranchGitAction

    @Binds
    @TabScope
    fun bindsSkipRebaseGitAction(action: SkipRebaseGitAction): ISkipRebaseGitAction

    @Binds
    @TabScope
    fun bindsStageAllGitAction(action: StageAllGitAction): IStageAllGitAction

    @Binds
    @TabScope
    fun bindsStageByDirectoryGitAction(action: StageByDirectoryGitAction): IStageByDirectoryGitAction

    @Binds
    @TabScope
    fun bindsStageEntryGitAction(action: StageEntryGitAction): IStageEntryGitAction

    @Binds
    @TabScope
    fun bindsStageHunkGitAction(action: StageHunkGitAction): IStageHunkGitAction

    @Binds
    @TabScope
    fun bindsStageHunkLineGitAction(action: StageHunkLineGitAction): IStageHunkLineGitAction

    @Binds
    @TabScope
    fun bindsStageUntrackedFileGitAction(action: StageUntrackedFileGitAction): IStageUntrackedFileGitAction

    @Binds
    @TabScope
    fun bindsStartRebaseInteractiveGitAction(action: StartRebaseInteractiveGitAction): IStartRebaseInteractiveGitAction

    @Binds
    @TabScope
    fun bindsStashChangesGitAction(action: StashChangesGitAction): IStashChangesGitAction

    @Binds
    @TabScope
    fun bindsSyncSubmoduleGitAction(action: SyncSubmoduleGitAction): ISyncSubmoduleGitAction

    @Binds
    @TabScope
    fun bindsTextDiffFromDiffLinesGitAction(action: TextDiffFromDiffLinesGitAction): ITextDiffFromDiffLinesGitAction

    @Binds
    @TabScope
    fun bindsUnstageAllGitAction(action: UnstageAllGitAction): IUnstageAllGitAction

    @Binds
    @TabScope
    fun bindsUnstageByDirectoryGitAction(action: UnstageByDirectoryGitAction): IUnstageByDirectoryGitAction

    @Binds
    @TabScope
    fun bindsUnstageEntryGitAction(action: UnstageEntryGitAction): IUnstageEntryGitAction

    @Binds
    @TabScope
    fun bindsUnstageHunkGitAction(action: UnstageHunkGitAction): IUnstageHunkGitAction

    @Binds
    @TabScope
    fun bindsUnstageHunkLineGitAction(action: UnstageHunkLineGitAction): IUnstageHunkLineGitAction

    @Binds
    @TabScope
    fun bindsUpdateRemoteGitAction(action: UpdateRemoteGitAction): IUpdateRemoteGitAction

    @Binds
    @TabScope
    fun bindsUpdateSubmoduleGitAction(action: UpdateSubmoduleGitAction): IUpdateSubmoduleGitAction

    @Binds
    @TabScope
    fun bindsUploadLfsObjectGitAction(action: UploadLfsObjectGitAction): IUploadLfsObjectGitAction

    @Binds
    @TabScope
    fun bindsVerifyUploadLfsObjectGitAction(action: VerifyUploadLfsObjectGitAction): IVerifyUploadLfsObjectGitAction

    @Binds
    @TabScope
    fun bindsGetFileCommitsAction(action: GetFileCommitsAction): IGetFileCommitsAction

    @Binds
    fun bindsGraphRevWalker(walker: JGitGraphRevWalker): GraphRevWalker

    @Binds
    @TabScope
    fun bindsGetPersistedCommitMessagesGitAction(walker: GetPersistedCommitMessagesGitAction): IGetPersistedCommitMessagesGitAction
}
