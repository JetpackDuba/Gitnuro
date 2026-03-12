package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.credentials.CredentialsHandler
import org.eclipse.jgit.api.Git

interface IHandleTransportGitAction {
    suspend operator fun <R> invoke(git: Git?, block: suspend CredentialsHandler.() -> R): R
}