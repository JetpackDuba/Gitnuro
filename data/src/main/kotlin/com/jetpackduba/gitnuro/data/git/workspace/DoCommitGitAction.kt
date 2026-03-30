package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.common.use
import com.jetpackduba.gitnuro.data.git.author.LoadAuthorGitAction
import com.jetpackduba.gitnuro.data.git.config.LoadSignOffConfigGitAction
import com.jetpackduba.gitnuro.data.git.config.LocalConfigConstants
import com.jetpackduba.gitnuro.data.git.jgit
import com.jetpackduba.gitnuro.data.git.repository.GetRepositoryStateGitAction
import com.jetpackduba.gitnuro.data.mappers.JGitCommitMapper
import com.jetpackduba.gitnuro.data.mappers.JGitIdentityMapper
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GenericError
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.errors.HookRejectionError
import com.jetpackduba.gitnuro.domain.extensions.isMerging
import com.jetpackduba.gitnuro.domain.interfaces.IDoCommitGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.models.Identity
import com.jetpackduba.gitnuro.domain.usecases.GetAuthorUseCase
import org.eclipse.jgit.api.errors.AbortedByHookException
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import javax.inject.Inject

private const val TAG = "DoCommitGitAction"

class DoCommitGitAction @Inject constructor(
    private val getRepositoryStateGitAction: GetRepositoryStateGitAction,
    private val commitMapper: JGitCommitMapper,
    private val identityMapper: JGitIdentityMapper,
) : IDoCommitGitAction {
    override suspend operator fun invoke(
        repositoryPath: String,
        message: String,
        amend: Boolean,
        author: Identity?,
    ): Either<Commit, GitError> = jgit(
        repositoryPath,
        errorHandle = { ex ->
            if (ex is AbortedByHookException) {
//                val out = output.toString(Charsets.UTF_8)
//                printLog(TAG, out)

                // TODO Do we need to read the output as it was done before the refactor?
                HookRejectionError(ex.hookStdErr)
            } else {
                GenericError(ex.message.orEmpty())
            }
        }
    ) {
        val state = getRepositoryStateGitAction(this)
        val isMerging = state.isMerging
        val output = ByteArrayOutputStream()
        val printStream = PrintStream(output, true, Charsets.UTF_8)

        use(output, printStream) {
            val commit = this.commit()
                .setMessage(message)
                .setAllowEmpty(amend || isMerging) // Only allow empty commits when amending
                .setAmend(amend)
                .setHookErrorStream(printStream)
                .setHookOutputStream(printStream)
                .run {
                    if (author != null) {
                        setAuthor(identityMapper.toData(author))
                    } else {
                        this
                    }
                }
                .call()

            commitMapper.toDomain(commit)
        }
    }
}