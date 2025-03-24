package com.jetpackduba.gitnuro.managers

import com.jetpackduba.gitnuro.TaskType
import com.jetpackduba.gitnuro.di.TabScope
import com.jetpackduba.gitnuro.exceptions.GitnuroException
import com.jetpackduba.gitnuro.models.Notification
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

const val NOTIFICATION_DURATION = 2_500L

@TabScope
class ErrorsManager @Inject constructor(
    private val coroutineScope: CoroutineScope,
) {
    private val _errorsList = MutableStateFlow(listOf<Error>())
    val errorsList: StateFlow<List<Error>>
        get() = _errorsList

    private val _error = MutableSharedFlow<Error?>()
    val error: SharedFlow<Error?> = _error

    private val _notification = MutableStateFlow<Map<Long, Notification>>(hashMapOf())
    val notification: StateFlow<Map<Long, Notification>> = _notification

    private val notificationsMutex = Mutex()

    suspend fun emitNotification(notification: Notification) = coroutineScope.launch {
        val time = System.currentTimeMillis()
        notificationsMutex.withLock {
            _notification.update { notifications ->
                notifications
                    .toMutableMap()
                    .apply { put(time, notification) }
            }
        }

        launch {
            delay(NOTIFICATION_DURATION)

            notificationsMutex.withLock {
                _notification.update { notifications ->
                    notifications
                        .toMutableMap()
                        .apply { remove(time) }
                }
            }
        }
    }

    suspend fun addError(error: Error) = withContext(Dispatchers.IO) {
        _errorsList.value = _errorsList.value.toMutableList().apply {
            add(error)
        }

        _error.emit(error)
    }

    fun removeError(error: Error) {
        _errorsList.value = _errorsList.value.toMutableList().apply {
            remove(error)
        }
    }
}


data class Error(
    val taskType: TaskType,
    val date: Long,
    val exception: Exception,
    val isUnhandled: Boolean,
) {
    fun errorTitle(): String {
        return when (taskType) {
            TaskType.UNSPECIFIED -> "Error"
            TaskType.STAGE_ALL_FILES -> "Staging all the files failed"
            TaskType.UNSTAGE_ALL_FILES -> "Unstaging all the files failed"
            TaskType.STAGE_FILE -> "File stage failed"
            TaskType.UNSTAGE_FILE -> "File unstage failed"
            TaskType.STAGE_HUNK -> "File stage failed"
            TaskType.UNSTAGE_HUNK -> "Hunk unstage failed"
            TaskType.STAGE_LINE -> "File line stage failed"
            TaskType.UNSTAGE_LINE -> "File line unstage failed"
            TaskType.DISCARD_FILE -> "Discard file failed"
            TaskType.DELETE_FILE -> "Delete file failed"
            TaskType.BLAME_FILE -> "File blaming failed"
            TaskType.HISTORY_FILE -> "Could not load file history"
            TaskType.DO_COMMIT -> "Commit failed"
            TaskType.AMEND_COMMIT -> "Commit amend failed"
            TaskType.REVERT_COMMIT -> "Commit revert failed"
            TaskType.CHERRY_PICK_COMMIT -> "Commit cherry-pick failed"
            TaskType.CHECKOUT_COMMIT -> "Checkout commit failed"
            TaskType.RESET_TO_COMMIT -> "Reset to commit failed"
            TaskType.CHECKOUT_BRANCH -> "Branch checkout failed"
            TaskType.CHECKOUT_REMOTE_BRANCH -> "Remote branch checkout failed"
            TaskType.CREATE_BRANCH -> "Could not create the new branch"
            TaskType.DELETE_BRANCH -> "Could not delete the branch"
            TaskType.MERGE_BRANCH -> "Merge failed"
            TaskType.REBASE_BRANCH -> "Rebase failed"
            TaskType.REBASE_INTERACTIVE -> "Rebase interactive failed"
            TaskType.CONTINUE_REBASE -> "Could not continue rebase"
            TaskType.ABORT_REBASE -> "Could not abort rebase"
            TaskType.SKIP_REBASE -> "Could not skip rebase step"
            TaskType.CHANGE_BRANCH_UPSTREAM -> "Upstream branch change failed"
            TaskType.PULL_FROM_BRANCH -> "Pull from branch failed"
            TaskType.PUSH_TO_BRANCH -> "Push to branch failed"
            TaskType.DELETE_REMOTE_BRANCH -> "Deleting remote branch failed"
            TaskType.PULL -> "Pull failed"
            TaskType.PUSH -> "Push failed"
            TaskType.FETCH -> "Fetch failed"
            TaskType.STASH -> "Stash failed"
            TaskType.APPLY_STASH -> "Apply stash failed"
            TaskType.POP_STASH -> "Pop stash failed"
            TaskType.DELETE_STASH -> "Delete stash failed"
            TaskType.CREATE_TAG -> "Create tag failed"
            TaskType.CHECKOUT_TAG -> "Could not checkout tag's commit"
            TaskType.DELETE_TAG -> "Could not delete tag"
            TaskType.ADD_SUBMODULE -> "Add submodule failed"
            TaskType.DELETE_SUBMODULE -> "Delete submodule failed"
            TaskType.INIT_SUBMODULE -> "Init submodule failed"
            TaskType.DEINIT_SUBMODULE -> "Deinit submodule failed"
            TaskType.SYNC_SUBMODULE -> "Sync submodule failed"
            TaskType.UPDATE_SUBMODULE -> "Update submodule failed"
            TaskType.SAVE_CUSTOM_THEME -> "Failed trying to save the custom theme"
            TaskType.RESET_REPO_STATE -> "Could not reset repository state"
            TaskType.CHANGES_DETECTION -> "Repository changes detection has stopped working"
            TaskType.REPOSITORY_OPEN -> "Could not open the repository"
            TaskType.REPOSITORY_CLONE -> "Could not clone the repository"
            TaskType.ADD_REMOTE -> "Adding remote failed"
            TaskType.DELETE_REMOTE -> "Deleting remote failed"
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
