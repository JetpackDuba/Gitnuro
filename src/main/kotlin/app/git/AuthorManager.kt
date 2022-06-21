package app.git

import app.models.AuthorInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileBasedConfig
import javax.inject.Inject

class AuthorManager @Inject constructor() {
    suspend fun loadAuthor(git: Git) = withContext(Dispatchers.IO) {
        val config = git.repository.config
        val globalConfig = git.repository.config.baseConfig

        val repoConfig = FileBasedConfig((config as FileBasedConfig).file, git.repository.fs)
        repoConfig.load()

        val globalName = globalConfig.getString("user", null, "name")
        val globalEmail = globalConfig.getString("user", null, "email")

        val name = repoConfig.getString("user", null, "name")
        val email = repoConfig.getString("user", null, "email")

        return@withContext AuthorInfo(
            globalName,
            globalEmail,
            name,
            email,
        )
    }

    suspend fun saveAuthorInfo(git: Git, newAuthorInfo: AuthorInfo) = withContext(Dispatchers.IO) {
        val config = git.repository.config
        val globalConfig = git.repository.config.baseConfig
        val repoConfig = FileBasedConfig((config as FileBasedConfig).file, git.repository.fs)
        repoConfig.load()

        if (globalConfig is FileBasedConfig) {
            globalConfig.setStringProperty("user", null, "name", newAuthorInfo.globalName)
            globalConfig.setStringProperty("user", null, "email", newAuthorInfo.globalEmail)
            globalConfig.save()
        }

        config.setStringProperty("user", null, "name", newAuthorInfo.name)
        config.setStringProperty("user", null, "email", newAuthorInfo.email)
        config.save()
    }

    private fun FileBasedConfig.setStringProperty(
        section: String,
        subsection: String?,
        name: String,
        value: String?,
    ) {
        if(value == null) {
            unset(section, subsection, name)
        } else {
            setString(section, subsection, name, value)
        }
    }
}