package com.jetpackduba.gitnuro.data.git.author

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.interfaces.ISaveAuthorGitAction
import com.jetpackduba.gitnuro.domain.models.AuthorInfo
import org.eclipse.jgit.storage.file.FileBasedConfig
import javax.inject.Inject

class SaveAuthorGitAction @Inject constructor(
    private val jgit: JGit,
) : ISaveAuthorGitAction {
    override suspend operator fun invoke(repositoryPath: String, newAuthorInfo: AuthorInfo) =
        jgit.provide(repositoryPath) { git ->
            val config = git.repository.config
            val globalConfig = git.repository.config.baseConfig
            val repoConfig = FileBasedConfig((config as FileBasedConfig).file, git.repository.fs)
            repoConfig.load()

            if (globalConfig is FileBasedConfig) {
                val canonicalConfigFile = globalConfig.file.canonicalFile
                val globalRepoConfig = FileBasedConfig(canonicalConfigFile, git.repository.fs)

                globalRepoConfig.load()
                globalRepoConfig.setStringProperty("user", null, "name", newAuthorInfo.globalName)
                globalRepoConfig.setStringProperty("user", null, "email", newAuthorInfo.globalEmail)
                globalRepoConfig.save()
            }

            config.setStringProperty("user", null, "name", newAuthorInfo.name)
            config.setStringProperty("user", null, "email", newAuthorInfo.email)
            config.save()
        }
}

private fun FileBasedConfig.setStringProperty(
    section: String,
    subsection: String?,
    name: String,
    value: String?,
) {
    if (value == null) {
        unset(section, subsection, name)
    } else {
        setString(section, subsection, name, value)
    }
}