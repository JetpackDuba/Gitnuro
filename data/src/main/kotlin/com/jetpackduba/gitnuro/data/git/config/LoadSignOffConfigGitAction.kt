package com.jetpackduba.gitnuro.data.git.config

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.SignOffConstants
import com.jetpackduba.gitnuro.domain.extensions.nullIfEmpty
import com.jetpackduba.gitnuro.domain.interfaces.ILoadSignOffConfigGitAction
import com.jetpackduba.gitnuro.domain.models.SignOffConfig
import org.eclipse.jgit.storage.file.FileBasedConfig
import java.io.File
import javax.inject.Inject


class LoadSignOffConfigGitAction @Inject constructor(
    private val jgit: JGit,
) : ILoadSignOffConfigGitAction {
    override suspend operator fun invoke(repositoryPath: String) = jgit.provide(repositoryPath) { git ->
        val configFile = File(git.repository.directory, LocalConfigConstants.CONFIG_FILE_NAME)
        configFile.createNewFile()

        val config = FileBasedConfig(configFile, git.repository.fs)
        config.load()

        val enabled = config.getBoolean(
            SignOffConstants.SECTION,
            null,
            SignOffConstants.FIELD_ENABLED,
            SignOffConstants.DEFAULT_SIGN_OFF_ENABLED
        )


        val format = config.getString(
            SignOffConstants.SECTION,
            null,
            SignOffConstants.FIELD_FORMAT
        )?.nullIfEmpty ?: SignOffConstants.DEFAULT_SIGN_OFF_FORMAT

        SignOffConfig(enabled, format)
    }
}
