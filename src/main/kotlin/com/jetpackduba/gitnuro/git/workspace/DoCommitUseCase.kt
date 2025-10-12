package com.jetpackduba.gitnuro.git.workspace

import com.jetpackduba.gitnuro.exceptions.HookException
import com.jetpackduba.gitnuro.extensions.isMerging
import com.jetpackduba.gitnuro.git.AppGpgSigner
import com.jetpackduba.gitnuro.git.author.LoadAuthorUseCase
import com.jetpackduba.gitnuro.git.config.LoadSignOffConfigUseCase
import com.jetpackduba.gitnuro.git.config.LocalConfigConstants
import com.jetpackduba.gitnuro.git.repository.GetRepositoryStateUseCase
import com.jetpackduba.gitnuro.logging.printLog
import com.jetpackduba.gitnuro.utils.use
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.AbortedByHookException
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.revwalk.RevCommit
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import javax.inject.Inject

private const val TAG = "DoCommitUseCase"

class DoCommitUseCase @Inject constructor(
    private val loadSignOffConfigUseCase: LoadSignOffConfigUseCase,
    private val loadAuthorUseCase: LoadAuthorUseCase,
    private val getRepositoryStateUseCase: GetRepositoryStateUseCase,
    private val appGpgSigner: AppGpgSigner,
) {
    suspend operator fun invoke(
        git: Git,
        message: String,
        amend: Boolean,
        author: PersonIdent?,
    ): RevCommit = withContext(Dispatchers.IO) {

        val signOffConfig = loadSignOffConfigUseCase(git.repository)

        val finalMessage = if (signOffConfig.isEnabled) {
            val authorToSign = author ?: loadAuthorUseCase(git).toPersonIdent()

            val signature = signOffConfig.format
                .replace(LocalConfigConstants.SignOff.DEFAULT_SIGN_OFF_FORMAT_USER, authorToSign.name)
                .replace(LocalConfigConstants.SignOff.DEFAULT_SIGN_OFF_FORMAT_EMAIL, authorToSign.emailAddress)

            "$message\n\n$signature"
        } else
            message

        val state = getRepositoryStateUseCase(git)
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