package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.common.printLog
import com.jetpackduba.gitnuro.common.use
import com.jetpackduba.gitnuro.domain.exceptions.HookException
import com.jetpackduba.gitnuro.domain.extensions.isMerging
import com.jetpackduba.gitnuro.data.git.author.LoadAuthorGitAction
import com.jetpackduba.gitnuro.data.git.config.LoadSignOffConfigGitAction
import com.jetpackduba.gitnuro.data.git.config.LocalConfigConstants
import com.jetpackduba.gitnuro.data.git.repository.GetRepositoryStateGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IDoCommitGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.AbortedByHookException
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.revwalk.RevCommit
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import javax.inject.Inject

private const val TAG = "DoCommitGitAction"

class DoCommitGitAction @Inject constructor(
    private val loadSignOffConfigGitAction: LoadSignOffConfigGitAction,
    private val loadAuthorGitAction: LoadAuthorGitAction,
    private val getRepositoryStateGitAction: GetRepositoryStateGitAction,
) : IDoCommitGitAction {
    override suspend operator fun invoke(
        git: Git,
        message: String,
        amend: Boolean,
        author: PersonIdent?,
    ): RevCommit = withContext(Dispatchers.IO) {

        val signOffConfig = loadSignOffConfigGitAction(git.repository)

        val finalMessage = if (signOffConfig.isEnabled) {
            val authorToSign = author ?: loadAuthorGitAction(git).toPersonIdent()

            val signature = signOffConfig.format
                .replace(LocalConfigConstants.SignOff.DEFAULT_SIGN_OFF_FORMAT_USER, authorToSign.name)
                .replace(LocalConfigConstants.SignOff.DEFAULT_SIGN_OFF_FORMAT_EMAIL, authorToSign.emailAddress)

            "$message\n\n$signature"
        } else
            message

        val state = getRepositoryStateGitAction(git)
        val isMerging = state.isMerging
        val output = ByteArrayOutputStream()
        val printStream = PrintStream(output, true, Charsets.UTF_8)

        try {
            use(output, printStream) {
                git.commit()
                    .setMessage(finalMessage)
                    .setAllowEmpty(amend || isMerging) // Only allow empty commits when amending
                    .setAmend(amend)
                    .setHookErrorStream(printStream)
                    .setHookOutputStream(printStream)
                    .setAuthor(author)
                    .call()
            }
        } catch (ex: Exception) {
            if (ex is AbortedByHookException) {
                val out = output.toString(Charsets.UTF_8)
                printLog(TAG, out)

                throw HookException(out)
            }


            throw ex
        }
    }
}