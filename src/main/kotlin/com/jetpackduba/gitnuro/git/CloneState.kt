package com.jetpackduba.gitnuro.git

import org.eclipse.jgit.transport.RemoteRefUpdate
import java.io.File

sealed interface CloneState {
    data object None : CloneState
    data class Cloning(val taskName: String, val progress: Int, val total: Int) : CloneState
    data object Cancelling : CloneState
    data class Fail(val reason: String) : CloneState
    data class Completed(val repoDir: File) : CloneState
}

val RemoteRefUpdate.Status.isRejected: Boolean
    get() {
        return this == RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD ||
                this == RemoteRefUpdate.Status.REJECTED_NODELETE ||
                this == RemoteRefUpdate.Status.REJECTED_REMOTE_CHANGED ||
                this == RemoteRefUpdate.Status.REJECTED_OTHER_REASON
    }

val RemoteRefUpdate.statusMessage: String
    get() {
        return when (this.status) {
            RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD -> "Failed to push some refs to ${this.remoteName}. " +
                    "Updates were rejected because the remote contains work that you do not have locally. Pulling changes from remote may help."

            RemoteRefUpdate.Status.REJECTED_NODELETE -> "Could not delete ref because the remote doesn't support deleting refs."
            RemoteRefUpdate.Status.REJECTED_REMOTE_CHANGED -> "Ref rejected, old object id in remote has changed."
            RemoteRefUpdate.Status.REJECTED_OTHER_REASON -> this.message ?: "Push rejected for unknown reasons."
            else -> ""
        }

    }