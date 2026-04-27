package com.jetpackduba.gitnuro.di.modules

import com.jetpackduba.gitnuro.data.git.GetWorkspacePathGitAction
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
import com.jetpackduba.gitnuro.domain.interfaces.*
import dagger.Binds
import dagger.Module

@Module
interface GitActionsModule {

    @Binds
    fun bindsAbortRebaseGitAction(action: AbortRebaseGitAction): IAbortRebaseGitAction

    @Binds
    fun bindsAddRemoteGitAction(action: AddRemoteGitAction): IAddRemoteGitAction

    @Binds
    fun bindsAddSubmoduleGitAction(action: AddSubmoduleGitAction): IAddSubmoduleGitAction

    @Binds
    fun bindsApplyStashGitAction(action: ApplyStashGitAction): IApplyStashGitAction

    @Binds
    fun bindsAuthenticateLfsServerWithSshGitAction(action: AuthenticateLfsServerWithSshGitAction): IAuthenticateLfsServerWithSshGitAction

    @Binds
    fun bindsCanGenerateTextDiffGitAction(action: CanGenerateTextDiffGitAction): ICanGenerateTextDiffGitAction

    @Binds
    fun bindsCheckHasPreviousCommitsGitAction(action: CheckHasPreviousCommitsGitAction): ICheckHasPreviousCommitsGitAction

    @Binds
    fun bindsCheckHasUncommittedChangesGitAction(action: CheckHasUncommittedChangesGitAction): ICheckHasUncommittedChangesGitAction

    @Binds
    fun bindsCheckoutCommitGitAction(action: CheckoutCommitGitAction): ICheckoutCommitGitAction

    @Binds
    fun bindsCheckoutRefGitAction(action: CheckoutRefGitAction): ICheckoutRefGitAction

    @Binds
    fun bindsCherryPickCommitGitAction(action: CherryPickCommitGitAction): ICherryPickCommitGitAction

    @Binds
    fun bindsCloneRepositoryGitAction(action: CloneRepositoryGitAction): ICloneRepositoryGitAction

    @Binds
    fun bindsContinueRebaseGitAction(action: ContinueRebaseGitAction): IContinueRebaseGitAction

    @Binds
    fun bindsCreateBranchGitAction(action: CreateBranchGitAction): ICreateBranchGitAction

    @Binds
    fun bindsCreateSnapshotStashGitAction(action: CreateSnapshotStashGitAction): ICreateSnapshotStashGitAction

    @Binds
    fun bindsCreateTagGitAction(action: CreateTagGitAction): ICreateTagGitAction

    @Binds
    fun bindsDeInitializeSubmoduleGitAction(action: DeInitializeSubmoduleGitAction): IDeInitializeSubmoduleGitAction

    @Binds
    fun bindsDeleteBranchGitAction(action: DeleteBranchGitAction): IDeleteBranchGitAction

    @Binds
    fun bindsDeleteLocallyRemoteBranchesGitAction(action: DeleteLocallyRemoteBranchesGitAction): IDeleteLocallyRemoteBranchesGitAction

    @Binds
    fun bindsDeleteRemoteBranchGitAction(action: DeleteRemoteBranchGitAction): IDeleteRemoteBranchGitAction

    @Binds
    fun bindsDeleteRemoteGitAction(action: DeleteRemoteGitAction): IDeleteRemoteGitAction

    @Binds
    fun bindsDeleteStashGitAction(action: DeleteStashGitAction): IDeleteStashGitAction

    @Binds
    fun bindsDeleteSubmoduleGitAction(action: DeleteSubmoduleGitAction): IDeleteSubmoduleGitAction

    @Binds
    fun bindsDeleteTagGitAction(action: DeleteTagGitAction): IDeleteTagGitAction

    @Binds
    fun bindsDiscardEntriesGitAction(action: DiscardEntriesGitAction): IDiscardEntriesGitAction

    @Binds
    fun bindsDiscardUnstagedHunkLineGitAction(action: DiscardUnstagedHunkLineGitAction): IDiscardUnstagedHunkLineGitAction

    @Binds
    fun bindsDoCommitGitAction(action: DoCommitGitAction): IDoCommitGitAction

    @Binds
    fun bindsDownloadLfsObjectGitAction(action: DownloadLfsObjectGitAction): IDownloadLfsObjectGitAction

    @Binds
    fun bindsFetchAllRemotesGitAction(action: FetchAllRemotesGitAction): IFetchAllRemotesGitAction

    @Binds
    fun bindsFindCommitGitAction(action: FindCommitGitAction): IFindCommitGitAction

    @Binds
    fun bindsFormatDiffGitAction(action: FormatDiffGitAction): IFormatDiffGitAction

    @Binds
    fun bindsFormatHunksGitAction(action: FormatHunksGitAction): IFormatHunksGitAction

    @Binds
    fun bindsGenerateSplitHunkFromDiffResultGitAction(action: GenerateSplitHunkFromDiffResultGitAction): IGenerateSplitHunkFromDiffResultGitAction

    @Binds
    fun bindsGetBranchesGitAction(action: GetBranchesGitAction): IGetBranchesGitAction

    @Binds
    fun bindsGetCurrentBranchGitAction(action: GetCurrentBranchGitAction): IGetCurrentBranchGitAction

    @Binds
    fun bindsGetCommitDiffEntriesGitAction(action: GetCommitDiffEntriesGitAction): IGetCommitDiffEntriesGitAction

    @Binds
    fun bindsGetCommitFromRebaseLineGitAction(action: GetCommitFromRebaseLineGitAction): IGetCommitFromRebaseLineGitAction

    @Binds
    fun bindsGetDiffContentGitAction(action: GetDiffContentGitAction): IGetDiffContentGitAction

    @Binds
    fun bindsGetDiffEntryFromStatusEntryGitAction(action: GetDiffEntryFromStatusEntryGitAction): IGetDiffEntryFromStatusEntryGitAction

    @Binds
    fun bindsGetIgnoreRulesGitAction(action: GetIgnoreRulesGitAction): IGetIgnoreRulesGitAction

    @Binds
    fun bindsGetSpecificCommitMessageGitAction(action: GetSpecificCommitMessageGitAction): IGetSpecificCommitMessageGitAction

    @Binds
    fun bindsGetLastCommitMessageGitAction(action: GetLastCommitMessageGitAction): IGetLastCommitMessageGitAction

    @Binds
    fun bindsGetLfsObjectsGitAction(action: GetLfsObjectsGitAction): IGetLfsObjectsGitAction

    @Binds
    fun bindsGetLogGitAction(action: GetLogGitAction): IGetLogGitAction

    @Binds
    fun bindsGetLinesFromRawTextGitAction(action: GetLinesFromRawTextGitAction): IGetLinesFromRawTextGitAction

    @Binds
    fun bindsGetLinesFromTextGitAction(action: GetLinesFromTextGitAction): IGetLinesFromTextGitAction

    @Binds
    fun bindsGetRebaseAmendCommitIdGitAction(action: GetRebaseAmendCommitIdGitAction): IGetRebaseAmendCommitIdGitAction

    @Binds
    fun bindsGetRebaseInteractiveStateGitAction(action: GetRebaseInteractiveStateGitAction): IGetRebaseInteractiveStateGitAction

    @Binds
    fun bindsGetRebaseInteractiveTodoLinesGitAction(action: GetRebaseInteractiveTodoLinesGitAction): IGetRebaseInteractiveTodoLinesGitAction

    @Binds
    fun bindsGetRebaseLinesFullMessageGitAction(action: GetRebaseLinesFullMessageGitAction): IGetRebaseLinesFullMessageGitAction

    @Binds
    fun bindsGetRemoteBranchesGitAction(action: GetRemoteBranchesGitAction): IGetRemoteBranchesGitAction

    @Binds
    fun bindsGetRemotesGitAction(action: GetRemotesGitAction): IGetRemotesGitAction

    @Binds
    fun bindsGetRepositoryStateGitAction(action: GetRepositoryStateGitAction): IGetRepositoryStateGitAction

    @Binds
    fun bindsGetStashListGitAction(action: GetStashListGitAction): IGetStashListGitAction

    @Binds
    fun bindsGetStatusGitAction(action: GetStatusGitAction): IGetStatusGitAction

    @Binds
    fun bindsGetTagsGitAction(action: GetTagsGitAction): IGetTagsGitAction

    @Binds
    fun bindsGetWorkspacePathGitAction(action: GetWorkspacePathGitAction): IGetWorkspacePathGitAction

    @Binds
    fun bindsGetSubmodulesGitAction(action: GetSubmodulesGitAction): IGetSubmodulesGitAction

    @Binds
    fun bindsGetTrackingBranchGitAction(action: GetTrackingBranchGitAction): IGetTrackingBranchGitAction

    @Binds
    fun bindsHandleTransportGitAction(action: HandleTransportGitAction): IHandleTransportGitAction

    @Binds
    fun bindsHasPullResultConflictsGitAction(action: HasPullResultConflictsGitAction): IHasPullResultConflictsGitAction

    @Binds
    fun bindsInitializeAllSubmodulesGitAction(action: InitializeAllSubmodulesGitAction): IInitializeAllSubmodulesGitAction

    @Binds
    fun bindsInitializeSubmoduleGitAction(action: InitializeSubmoduleGitAction): IInitializeSubmoduleGitAction

    @Binds
    fun bindsInitLocalRepositoryGitAction(action: InitLocalRepositoryGitAction): IInitLocalRepositoryGitAction

    @Binds
    fun bindsLoadAuthorGitAction(action: LoadAuthorGitAction): ILoadAuthorGitAction

    @Binds
    fun bindsLoadSignOffConfigGitAction(action: LoadSignOffConfigGitAction): ILoadSignOffConfigGitAction

    @Binds
    fun bindsMergeBranchGitAction(action: MergeBranchGitAction): IMergeBranchGitAction

    @Binds
    fun bindsOpenRepositoryGitAction(action: OpenRepositoryGitAction): IOpenRepositoryGitAction

    @Binds
    fun bindsPopLastStashGitAction(action: PopLastStashGitAction): IPopLastStashGitAction

    @Binds
    fun bindsPopStashGitAction(action: PopStashGitAction): IPopStashGitAction

    @Binds
    fun bindsProvideLfsCredentialsGitAction(action: ProvideLfsCredentialsGitAction): IProvideLfsCredentialsGitAction

    @Binds
    fun bindsPullBranchGitAction(action: PullBranchGitAction): IPullBranchGitAction

    @Binds
    fun bindsPullFromSpecificBranchGitAction(action: PullFromSpecificBranchGitAction): IPullFromSpecificBranchGitAction

    @Binds
    fun bindsPushBranchGitAction(action: PushBranchGitAction): IPushBranchGitAction

    @Binds
    fun bindsPushToSpecificBranchGitAction(action: PushToSpecificBranchGitAction): IPushToSpecificBranchGitAction

    @Binds
    fun bindsRebaseBranchGitAction(action: RebaseBranchGitAction): IRebaseBranchGitAction

    @Binds
    fun bindsRenameBranchGitAction(action: RenameBranchGitAction): IRenameBranchGitAction

    @Binds
    fun bindsResetHunkGitAction(action: ResetHunkGitAction): IResetHunkGitAction

    @Binds
    fun bindsResetRepositoryStateGitAction(action: ResetRepositoryStateGitAction): IResetRepositoryStateGitAction

    @Binds
    fun bindsResetToCommitGitAction(action: ResetToCommitGitAction): IResetToCommitGitAction

    @Binds
    fun bindsResumeRebaseInteractiveGitAction(action: ResumeRebaseInteractiveGitAction): IResumeRebaseInteractiveGitAction

    @Binds
    fun bindsRevertCommitGitAction(action: RevertCommitGitAction): IRevertCommitGitAction

    @Binds
    fun bindsSaveAuthorGitAction(action: SaveAuthorGitAction): ISaveAuthorGitAction

    @Binds
    fun bindsSaveLocalRepositoryConfigGitAction(action: SaveLocalRepositoryConfigGitAction): ISaveLocalRepositoryConfigGitAction

    @Binds
    fun bindsSetTrackingBranchGitAction(action: SetTrackingBranchGitAction): ISetTrackingBranchGitAction

    @Binds
    fun bindsSkipRebaseGitAction(action: SkipRebaseGitAction): ISkipRebaseGitAction

    @Binds
    fun bindsStageAllGitAction(action: StageAllGitAction): IStageAllGitAction

    @Binds
    fun bindsStageByDirectoryGitAction(action: StageByDirectoryGitAction): IStageByDirectoryGitAction

    @Binds
    fun bindsStageEntryGitAction(action: StageEntryGitAction): IStageEntryGitAction

    @Binds
    fun bindsStageHunkGitAction(action: StageHunkGitAction): IStageHunkGitAction

    @Binds
    fun bindsStageHunkLineGitAction(action: StageHunkLineGitAction): IStageHunkLineGitAction

    @Binds
    fun bindsStageUntrackedFileGitAction(action: StageUntrackedFileGitAction): IStageUntrackedFileGitAction

    @Binds
    fun bindsStartRebaseInteractiveGitAction(action: StartRebaseInteractiveGitAction): IStartRebaseInteractiveGitAction

    @Binds
    fun bindsStashChangesGitAction(action: StashChangesGitAction): IStashChangesGitAction

    @Binds
    fun bindsSyncSubmoduleGitAction(action: SyncSubmoduleGitAction): ISyncSubmoduleGitAction

    @Binds
    fun bindsTextDiffFromDiffLinesGitAction(action: TextDiffFromDiffLinesGitAction): ITextDiffFromDiffLinesGitAction

    @Binds
    fun bindsUnstageAllGitAction(action: UnstageAllGitAction): IUnstageAllGitAction

    @Binds
    fun bindsUnstageByDirectoryGitAction(action: UnstageByDirectoryGitAction): IUnstageByDirectoryGitAction

    @Binds
    fun bindsUnstageEntryGitAction(action: UnstageEntryGitAction): IUnstageEntryGitAction

    @Binds
    fun bindsUnstageHunkGitAction(action: UnstageHunkGitAction): IUnstageHunkGitAction

    @Binds
    fun bindsUnstageHunkLineGitAction(action: UnstageHunkLineGitAction): IUnstageHunkLineGitAction

    @Binds
    fun bindsUpdateRemoteGitAction(action: UpdateRemoteGitAction): IUpdateRemoteGitAction

    @Binds
    fun bindsUpdateSubmoduleGitAction(action: UpdateSubmoduleGitAction): IUpdateSubmoduleGitAction

    @Binds
    fun bindsUploadLfsObjectGitAction(action: UploadLfsObjectGitAction): IUploadLfsObjectGitAction

    @Binds
    fun bindsVerifyUploadLfsObjectGitAction(action: VerifyUploadLfsObjectGitAction): IVerifyUploadLfsObjectGitAction

    @Binds
    fun bindsGetFileCommitsAction(action: GetFileCommitsAction): IGetFileCommitsAction
}