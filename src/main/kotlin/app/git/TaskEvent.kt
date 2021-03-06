package app.git

import org.eclipse.jgit.revwalk.RevCommit

sealed interface TaskEvent {
    data class RebaseInteractive(val revCommit: RevCommit): TaskEvent
}