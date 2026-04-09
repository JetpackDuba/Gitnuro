package com.jetpackduba.gitnuro.git.signing

import com.jetpackduba.gitnuro.extensions.nullIfEmpty
import com.jetpackduba.gitnuro.managers.IShellManager
import com.jetpackduba.gitnuro.models.GitSigningKeyDiscoveryResult
import com.jetpackduba.gitnuro.models.GitSigningKeyOption
import com.jetpackduba.gitnuro.models.GitSigningSettings
import com.jetpackduba.gitnuro.models.GitSigningSettingsField
import com.jetpackduba.gitnuro.models.GitSigningSettingsOverrides
import com.jetpackduba.gitnuro.models.RepositoryGitSigningSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.lib.ConfigConstants
import org.eclipse.jgit.lib.GpgConfig
import org.eclipse.jgit.lib.StoredConfig
import org.eclipse.jgit.storage.file.FileBasedConfig
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.util.SystemReader
import java.io.File
import javax.inject.Inject

class LoadGlobalGitSigningSettingsUseCase @Inject constructor() {
    suspend operator fun invoke(): GitSigningSettings = withContext(Dispatchers.IO) {
        SystemReader.getInstance().userConfig.toGitSigningSettings()
    }
}

class SaveGlobalGitSigningSettingsUseCase @Inject constructor() {
    suspend operator fun invoke(settings: GitSigningSettings) = withContext(Dispatchers.IO) {
        val config = SystemReader.getInstance().userConfig
        config.applyGitSigningSettings(settings)
        config.save()
    }
}

class LoadRepositoryGitSigningSettingsUseCase @Inject constructor() {
    suspend operator fun invoke(repositoryPath: String): RepositoryGitSigningSettings = withContext(Dispatchers.IO) {
        openRepositoryConfig(repositoryPath) { repository ->
            val localConfig = FileBasedConfig(File(repository.directory, "config"), repository.fs).apply {
                load()
            }

            RepositoryGitSigningSettings(
                settings = repository.config.toGitSigningSettings(),
                overrides = localConfig.toGitSigningSettingsOverrides(),
            )
        }
    }
}

class SaveRepositoryGitSigningSettingsUseCase @Inject constructor() {
    suspend operator fun invoke(
        repositoryPath: String,
        settings: GitSigningSettings,
    ) = withContext(Dispatchers.IO) {
        openRepositoryConfig(repositoryPath) { repository ->
            repository.config.applyGitSigningSettings(settings)
            repository.config.save()
        }
    }
}

class UnsetRepositoryGitSigningSettingUseCase @Inject constructor() {
    suspend operator fun invoke(
        repositoryPath: String,
        field: GitSigningSettingsField,
    ) = withContext(Dispatchers.IO) {
        openRepositoryConfig(repositoryPath) { repository ->
            repository.config.unsetGitSigningSetting(field)
            repository.config.save()
        }
    }
}

class ClearRepositoryGitSigningOverridesUseCase @Inject constructor() {
    suspend operator fun invoke(repositoryPath: String) = withContext(Dispatchers.IO) {
        openRepositoryConfig(repositoryPath) { repository ->
            GitSigningSettingsField.entries.forEach { field ->
                repository.config.unsetGitSigningSetting(field)
            }
            repository.config.save()
        }
    }
}

class DiscoverGitSigningKeysUseCase @Inject constructor(
    private val shellManager: IShellManager,
) {
    suspend operator fun invoke(
        format: GpgConfig.GpgFormat,
        openPgpProgram: String,
    ): GitSigningKeyDiscoveryResult = withContext(Dispatchers.IO) {
        when (format) {
            GpgConfig.GpgFormat.SSH -> discoverSshKeys()
            else -> discoverOpenPgpKeys(openPgpProgram)
        }
    }

    private fun discoverOpenPgpKeys(openPgpProgram: String): GitSigningKeyDiscoveryResult {
        val programs = candidateOpenPgpPrograms(openPgpProgram)

        programs.forEach { program ->
            val output = shellManager.runCommand(
                listOf(
                    program,
                    "--list-secret-keys",
                    "--fingerprint",
                    "--keyid-format=long",
                    "--with-colons",
                )
            )

            if (output != null) {
                val keys = parseOpenPgpKeys(output)

                return if (keys.isEmpty()) {
                    GitSigningKeyDiscoveryResult(
                        options = emptyList(),
                        message = "No OpenPGP secret keys detected. Enter a key ID manually if needed.",
                    )
                } else {
                    GitSigningKeyDiscoveryResult(
                        options = keys,
                        message = "Detected ${keys.size} OpenPGP secret key(s).",
                    )
                }
            }
        }

        val message = if (openPgpProgram.isBlank()) {
            "Unable to detect an OpenPGP program. Configure GPG or enter a key ID manually."
        } else {
            "Unable to list OpenPGP keys with \"$openPgpProgram\"."
        }

        return GitSigningKeyDiscoveryResult(
            options = emptyList(),
            message = message,
        )
    }

    private fun discoverSshKeys(): GitSigningKeyDiscoveryResult {
        val sshDir = File(System.getProperty("user.home"), ".ssh")
        if (!sshDir.exists() || !sshDir.isDirectory) {
            return GitSigningKeyDiscoveryResult(
                options = emptyList(),
                message = "No ~/.ssh directory found. Enter a private key path manually if needed.",
            )
        }

        val keys = sshDir.listFiles()
            .orEmpty()
            .filter { it.isFile }
            .sortedBy { it.name }
            .filter { isSshPrivateKey(it) }
            .map { keyFile ->
                GitSigningKeyOption(
                    value = keyFile.absolutePath,
                    title = keyFile.name,
                    subtitle = describeSshKey(keyFile),
                )
            }

        val message = if (keys.isEmpty()) {
            "No SSH private keys were detected in ~/.ssh."
        } else {
            "Detected ${keys.size} SSH private key(s) in ~/.ssh."
        }

        return GitSigningKeyDiscoveryResult(
            options = keys,
            message = message,
        )
    }

    internal fun parseOpenPgpKeys(output: String): List<GitSigningKeyOption> {
        data class ParsedKey(
            val keyId: String,
            var fingerprint: String = "",
            var userId: String = "",
        )

        val parsedKeys = mutableListOf<ParsedKey>()
        var currentKey: ParsedKey? = null

        output.lineSequence().forEach { line ->
            val parts = line.split(':')
            when (parts.firstOrNull()) {
                "sec" -> {
                    currentKey?.let(parsedKeys::add)
                    currentKey = ParsedKey(keyId = parts.getOrNull(4).orEmpty())
                }

                "fpr" -> {
                    currentKey?.let { key ->
                        if (key.fingerprint.isBlank()) {
                            key.fingerprint = parts.getOrNull(9).orEmpty()
                        }
                    }
                }

                "uid" -> {
                    currentKey?.let { key ->
                        if (key.userId.isBlank()) {
                            key.userId = parts.getOrNull(9).orEmpty()
                        }
                    }
                }
            }
        }

        currentKey?.let(parsedKeys::add)

        return parsedKeys
            .mapNotNull { key ->
                val value = key.fingerprint.ifBlank { key.keyId }.ifBlank { return@mapNotNull null }
                val title = key.userId.ifBlank { value }
                val subtitle = buildString {
                    if (key.keyId.isNotBlank()) {
                        append(key.keyId)
                    }

                    if (key.fingerprint.isNotBlank() && key.fingerprint != key.keyId) {
                        if (isNotBlank()) {
                            append(" • ")
                        }
                        append(key.fingerprint.takeLast(16))
                    }
                }

                GitSigningKeyOption(
                    value = value,
                    title = title,
                    subtitle = subtitle,
                )
            }
            .distinctBy { it.value }
    }

    internal fun isSshPrivateKey(file: File): Boolean {
        if (file.name in SSH_NON_PRIVATE_KEY_FILES) {
            return false
        }

        if (file.name.endsWith(".pub") || file.name.endsWith(".txt")) {
            return false
        }

        val firstLine = runCatching {
            file.bufferedReader().use { it.readLine().orEmpty() }
        }.getOrDefault("")

        return SSH_PRIVATE_KEY_HEADERS.any { header -> firstLine.contains(header) }
    }

    private fun describeSshKey(file: File): String {
        val publicKeyFile = File("${file.absolutePath}.pub")
        if (!publicKeyFile.exists() || !publicKeyFile.isFile) {
            return "Private key file"
        }

        val publicKeyContent = runCatching {
            publicKeyFile.readText().trim()
        }.getOrDefault("")

        val parts = publicKeyContent.split(" ").filter { it.isNotBlank() }
        val type = parts.getOrNull(0).orEmpty()
        val comment = parts.drop(2).joinToString(" ")

        return buildString {
            append(type.ifBlank { "Public key available" })
            if (comment.isNotBlank()) {
                append(" • ")
                append(comment)
            }
        }
    }

    private fun candidateOpenPgpPrograms(openPgpProgram: String): List<String> {
        return buildList {
            if (openPgpProgram.isNotBlank()) {
                add(openPgpProgram)
            }

            add("gpg")
            add("gpg2")
        }.distinct()
    }

    companion object {
        private val SSH_NON_PRIVATE_KEY_FILES = setOf(
            "authorized_keys",
            "config",
            "known_hosts",
            "known_hosts.old",
            "allowed_signers",
            "revoked_keys",
        )

        private val SSH_PRIVATE_KEY_HEADERS = listOf(
            "BEGIN OPENSSH PRIVATE KEY",
            "BEGIN RSA PRIVATE KEY",
            "BEGIN EC PRIVATE KEY",
            "BEGIN DSA PRIVATE KEY",
            "BEGIN PRIVATE KEY",
        )
    }
}

private fun Config.toGitSigningSettings(): GitSigningSettings {
    val format = when (
        getEnum(
            ConfigConstants.CONFIG_GPG_SECTION,
            null,
            ConfigConstants.CONFIG_KEY_FORMAT,
            GpgConfig.GpgFormat.OPENPGP,
        )
    ) {
        GpgConfig.GpgFormat.SSH -> GpgConfig.GpgFormat.SSH
        else -> GpgConfig.GpgFormat.OPENPGP
    }

    val openPgpProgram = getString(
        ConfigConstants.CONFIG_GPG_SECTION,
        GpgConfig.GpgFormat.OPENPGP.toConfigValue(),
        ConfigConstants.CONFIG_KEY_PROGRAM,
    ) ?: getString(
        ConfigConstants.CONFIG_GPG_SECTION,
        null,
        ConfigConstants.CONFIG_KEY_PROGRAM,
    ).orEmpty()

    return GitSigningSettings(
        format = format,
        openPgpProgram = openPgpProgram,
        signingKey = getString(
            ConfigConstants.CONFIG_USER_SECTION,
            null,
            ConfigConstants.CONFIG_KEY_SIGNINGKEY,
        ).orEmpty(),
        signCommitsByDefault = getBoolean(
            ConfigConstants.CONFIG_COMMIT_SECTION,
            null,
            ConfigConstants.CONFIG_KEY_GPGSIGN,
            false,
        ),
        signTagsByDefault = getBoolean(
            ConfigConstants.CONFIG_TAG_SECTION,
            null,
            ConfigConstants.CONFIG_KEY_GPGSIGN,
            false,
        ),
    )
}

private fun Config.toGitSigningSettingsOverrides(): GitSigningSettingsOverrides {
    return GitSigningSettingsOverrides(
        format = hasExplicitValue(
            ConfigConstants.CONFIG_GPG_SECTION,
            null,
            ConfigConstants.CONFIG_KEY_FORMAT,
        ),
        openPgpProgram = hasExplicitValue(
            ConfigConstants.CONFIG_GPG_SECTION,
            GpgConfig.GpgFormat.OPENPGP.toConfigValue(),
            ConfigConstants.CONFIG_KEY_PROGRAM,
        ) || hasExplicitValue(
            ConfigConstants.CONFIG_GPG_SECTION,
            null,
            ConfigConstants.CONFIG_KEY_PROGRAM,
        ),
        signingKey = hasExplicitValue(
            ConfigConstants.CONFIG_USER_SECTION,
            null,
            ConfigConstants.CONFIG_KEY_SIGNINGKEY,
        ),
        signCommitsByDefault = hasExplicitValue(
            ConfigConstants.CONFIG_COMMIT_SECTION,
            null,
            ConfigConstants.CONFIG_KEY_GPGSIGN,
        ),
        signTagsByDefault = hasExplicitValue(
            ConfigConstants.CONFIG_TAG_SECTION,
            null,
            ConfigConstants.CONFIG_KEY_GPGSIGN,
        ),
    )
}

private fun StoredConfig.applyGitSigningSettings(settings: GitSigningSettings) {
    setEnum(
        ConfigConstants.CONFIG_GPG_SECTION,
        null,
        ConfigConstants.CONFIG_KEY_FORMAT,
        settings.format,
    )
    setStringProperty(
        ConfigConstants.CONFIG_GPG_SECTION,
        GpgConfig.GpgFormat.OPENPGP.toConfigValue(),
        ConfigConstants.CONFIG_KEY_PROGRAM,
        settings.openPgpProgram.nullIfEmpty,
    )
    setStringProperty(
        ConfigConstants.CONFIG_GPG_SECTION,
        null,
        ConfigConstants.CONFIG_KEY_PROGRAM,
        settings.openPgpProgram.nullIfEmpty,
    )
    setStringProperty(
        ConfigConstants.CONFIG_USER_SECTION,
        null,
        ConfigConstants.CONFIG_KEY_SIGNINGKEY,
        settings.signingKey.nullIfEmpty,
    )
    setBoolean(
        ConfigConstants.CONFIG_COMMIT_SECTION,
        null,
        ConfigConstants.CONFIG_KEY_GPGSIGN,
        settings.signCommitsByDefault,
    )
    setBoolean(
        ConfigConstants.CONFIG_TAG_SECTION,
        null,
        ConfigConstants.CONFIG_KEY_GPGSIGN,
        settings.signTagsByDefault,
    )
}

private fun <T> openRepositoryConfig(repositoryPath: String, block: (org.eclipse.jgit.lib.Repository) -> T): T {
    val repository = FileRepositoryBuilder()
        .setWorkTree(File(repositoryPath))
        .readEnvironment()
        .findGitDir(File(repositoryPath))
        .build()

    return repository.use(block)
}

private fun Config.hasExplicitValue(
    section: String,
    subsection: String?,
    name: String,
): Boolean {
    return getNames(section, subsection).contains(name)
}

private fun StoredConfig.unsetGitSigningSetting(field: GitSigningSettingsField) {
    when (field) {
        GitSigningSettingsField.FORMAT -> {
            unset(ConfigConstants.CONFIG_GPG_SECTION, null, ConfigConstants.CONFIG_KEY_FORMAT)
        }

        GitSigningSettingsField.OPENPGP_PROGRAM -> {
            unset(
                ConfigConstants.CONFIG_GPG_SECTION,
                GpgConfig.GpgFormat.OPENPGP.toConfigValue(),
                ConfigConstants.CONFIG_KEY_PROGRAM,
            )
            unset(ConfigConstants.CONFIG_GPG_SECTION, null, ConfigConstants.CONFIG_KEY_PROGRAM)
        }

        GitSigningSettingsField.SIGNING_KEY -> {
            unset(ConfigConstants.CONFIG_USER_SECTION, null, ConfigConstants.CONFIG_KEY_SIGNINGKEY)
        }

        GitSigningSettingsField.SIGN_COMMITS -> {
            unset(ConfigConstants.CONFIG_COMMIT_SECTION, null, ConfigConstants.CONFIG_KEY_GPGSIGN)
        }

        GitSigningSettingsField.SIGN_TAGS -> {
            unset(ConfigConstants.CONFIG_TAG_SECTION, null, ConfigConstants.CONFIG_KEY_GPGSIGN)
        }
    }
}

private fun StoredConfig.setStringProperty(
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
