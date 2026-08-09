package com.jetpackduba.gitnuro.ui

import androidx.compose.runtime.Composable
import com.jetpackduba.gitnuro.app.generated.resources.Res
import com.jetpackduba.gitnuro.app.generated.resources.error_create_branch_already_exists
import com.jetpackduba.gitnuro.app.generated.resources.error_create_branch_name_not_allowed
import com.jetpackduba.gitnuro.app.generated.resources.error_hook_rejection
import com.jetpackduba.gitnuro.app.generated.resources.error_open_repository_dir_not_found
import com.jetpackduba.gitnuro.app.generated.resources.error_open_repository_path_is_not_dir
import com.jetpackduba.gitnuro.app.generated.resources.error_open_repository_repo_not_found
import com.jetpackduba.gitnuro.app.generated.resources.error_open_repository_repository_load
import com.jetpackduba.gitnuro.app.generated.resources.error_repository_path_not_set
import com.jetpackduba.gitnuro.app.generated.resources.error_repository_read_error
import com.jetpackduba.gitnuro.app.generated.resources.error_stash_no_data
import com.jetpackduba.gitnuro.domain.errors.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppError.getErrorText(): String {
    return when (this) {
        is CreateBranchError -> when (this) {
            is CreateBranchError.BranchAlreadyExists -> stringResource(Res.string.error_create_branch_already_exists, this.name)
            is CreateBranchError.NameNotAllowed -> stringResource(Res.string.error_create_branch_name_not_allowed, this.name)
        }

        is GenericError -> this.message
        is HookRejectionError -> stringResource(Res.string.error_hook_rejection, this.message)
        RepositoryPathNotSetError -> stringResource(Res.string.error_repository_path_not_set)
        is RepositoryReadError -> stringResource(Res.string.error_repository_read_error, this.message)
        is StashChangesError -> when (this) {
            StashChangesError.NoDataToStash -> stringResource(Res.string.error_stash_no_data)
        }

        is OpenRepoError -> when (this) {
            OpenRepoError.DirectoryNotFoundError -> stringResource(Res.string.error_open_repository_dir_not_found)
            OpenRepoError.PathIsNotDirectory -> stringResource(Res.string.error_open_repository_path_is_not_dir)
            is OpenRepoError.RepositoryLoadFailed -> stringResource(Res.string.error_open_repository_repository_load, this.error)
            OpenRepoError.RepositoryNotFoundInPath -> stringResource(Res.string.error_open_repository_repo_not_found)
        }
    }
}