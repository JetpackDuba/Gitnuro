package com.jetpackduba.gitnuro.di.factories

import com.jetpackduba.gitnuro.credentials.HttpCredentialsProvider
import dagger.assisted.AssistedFactory
import org.eclipse.jgit.api.Git

@AssistedFactory
interface HttpCredentialsFactory {
    fun create(git: Git?): HttpCredentialsProvider
}