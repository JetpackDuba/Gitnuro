package com.jetpackduba.gitnuro.data.git.tags

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.data.git.signers.AppGpgSigner
import com.jetpackduba.gitnuro.data.git.signers.SshSigner
import com.jetpackduba.gitnuro.domain.interfaces.ICreateTagGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import javax.inject.Inject
import javax.inject.Provider

private const val GPG_FORMAT_SSH = "ssh"
private const val GPG_FORMAT_GPG = "gpg"

class CreateTagGitAction @Inject constructor(
    private val jgit: JGit,
    private val sshSigner: Provider<SshSigner>,
    private val gpgSigner: Provider<AppGpgSigner>,
) : ICreateTagGitAction {
    override suspend operator fun invoke(repositoryPath: String, tag: String, commit: Commit) =
        jgit.provide(repositoryPath) { git ->
            val commitId =
                ObjectId.fromString(commit.hash) // TODO Should this be used instead of "git.repository.resolve(revCommit.hash) ?: throw Exception("Commit ${revCommit.hash} not found")" used in other places?
            val commit: RevCommit? = RevWalk(git.repository).use { revWalk ->
                revWalk.parseCommit(commitId)
            }

            git
                .tag()
                .setAnnotated(true)
                .setName(tag)
                .setObjectId(commit)
                .run {
                    val signConfig = getTagSigningConfig(git)

                    if (signConfig.isSigningEnabled && signConfig.signingType != null && signConfig.signingKey != null) {
                        val signer = when (signConfig.signingType) {
                            SigningType.SSH -> sshSigner.get()
                            SigningType.GPG -> gpgSigner.get()
                        }

                        setSigned(true)
                            .setSigner(signer)
                            .setSigningKey(signConfig.signingKey)

                    } else {
                        setSigned(false)
                    }
                }
                .call()

            Unit
        }

    private fun getTagSigningConfig(git: Git): TagSignConfig {
        val config = git.repository.config.apply {
            load()
        }

        val signTag = config.getBoolean("tag", null, "gpgSign") ?: false
        val gpgFormat: String? = config.getString("gpg", null, "format")
        val signingKey: String? = config.getString("user", null, "signingkey")

        return if (signTag) {
            val type = when (gpgFormat) {
                GPG_FORMAT_GPG -> SigningType.GPG
                GPG_FORMAT_SSH -> SigningType.SSH
                else -> throw IllegalStateException("Unsupported gpg format: $gpgFormat")
            }

            if (signingKey == null) {
                throw IllegalStateException("Can't sign tags. Signing key not set: $signingKey")
            }

            TagSignConfig(signTag, type, signingKey)
        } else {
            TagSignConfig(isSigningEnabled = false, signingType = null, signingKey = null)
        }
    }
}

private data class TagSignConfig(
    val isSigningEnabled: Boolean,
    val signingType: SigningType?,
    val signingKey: String?,
)

private enum class SigningType {
    SSH,
    GPG,
}