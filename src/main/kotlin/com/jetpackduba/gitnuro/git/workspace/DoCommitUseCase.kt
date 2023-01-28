package com.jetpackduba.gitnuro.git.workspace

import com.jetpackduba.gitnuro.credentials.GpgCredentialsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.transport.CredentialItem
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.URIish
import javax.inject.Inject

class DoCommitUseCase @Inject constructor(
    private val gpgCredentialsProvider: GpgCredentialsProvider,
) {
    suspend operator fun invoke(git: Git, message: String, amend: Boolean): RevCommit = withContext(Dispatchers.IO) {
        git.commit()
            .setMessage(message)
            .setAllowEmpty(amend) // Only allow empty commits when amending
            .setAmend(amend)
            .setCredentialsProvider(gpgCredentialsProvider)
            .call()
    }
}