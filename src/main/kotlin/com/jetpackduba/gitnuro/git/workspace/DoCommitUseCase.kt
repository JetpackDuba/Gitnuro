package com.jetpackduba.gitnuro.git.workspace

import com.jetpackduba.gitnuro.extensions.isMerging
import com.jetpackduba.gitnuro.git.AppGpgSigner
import com.jetpackduba.gitnuro.git.author.LoadAuthorUseCase
import com.jetpackduba.gitnuro.git.config.LoadSignOffConfigUseCase
import com.jetpackduba.gitnuro.git.config.LocalConfigConstants
import com.jetpackduba.gitnuro.git.repository.GetRepositoryStateUseCase
import com.jetpackduba.gitnuro.repositories.AppSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class DoCommitUseCase @Inject constructor(
    private val loadSignOffConfigUseCase: LoadSignOffConfigUseCase,
    private val loadAuthorUseCase: LoadAuthorUseCase,
    private val getRepositoryStateUseCase: GetRepositoryStateUseCase,
    private val appGpgSigner: AppGpgSigner,
    private val appSettingsRepository: AppSettingsRepository,
) {
    suspend operator fun invoke(
        git: Git,
        message: String,
        amend: Boolean,
        author: PersonIdent?,
    ): RevCommit = withContext(Dispatchers.IO) {

        var finalMessage = signOff(message, git, author)
        finalMessage = appendBranchName(finalMessage, git)

        val state = getRepositoryStateUseCase(git)
        val isMerging = state.isMerging

        git.commit()
            .setMessage(finalMessage)
            .setAllowEmpty(amend || isMerging) // Only allow empty commits when amending
            .setAmend(amend)
            .setAuthor(author)
            .call()
    }

    private suspend fun signOff(message: String, git: Git, author: PersonIdent?): String {
        val signOffConfig = loadSignOffConfigUseCase(git.repository)
        if (signOffConfig.isEnabled) {
            val authorToSign = author ?: loadAuthorUseCase(git).toPersonIdent()

            val signature = signOffConfig.format
                .replace(LocalConfigConstants.SignOff.DEFAULT_SIGN_OFF_FORMAT_USER, authorToSign.name)
                .replace(LocalConfigConstants.SignOff.DEFAULT_SIGN_OFF_FORMAT_EMAIL, authorToSign.emailAddress)

            return "$message\n\n$signature"
        } else
            return message
    }

    private fun appendBranchName(message: String, git: Git): String {
        val branchName = git.repository.branch
        if (appSettingsRepository.includeBranchName && !message.contains(branchName))
            return "[$branchName] $message"
        return message
    }
}