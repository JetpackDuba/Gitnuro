package com.jetpackduba.gitnuro.models

import org.eclipse.jgit.lib.GpgConfig

enum class GitSigningSettingsScope {
    GLOBAL,
    REPOSITORY,
}

enum class GitSigningSettingsField {
    FORMAT,
    OPENPGP_PROGRAM,
    SIGNING_KEY,
    SIGN_COMMITS,
    SIGN_TAGS,
}

data class GitSigningSettings(
    val format: GpgConfig.GpgFormat = GpgConfig.GpgFormat.OPENPGP,
    val openPgpProgram: String = "",
    val signingKey: String = "",
    val signCommitsByDefault: Boolean = false,
    val signTagsByDefault: Boolean = false,
)

data class GitSigningKeyOption(
    val value: String,
    val title: String,
    val subtitle: String = "",
) {
    val label = if (subtitle.isBlank()) title else "$title — $subtitle"
}

data class GitSigningSettingsOverrides(
    val format: Boolean = false,
    val openPgpProgram: Boolean = false,
    val signingKey: Boolean = false,
    val signCommitsByDefault: Boolean = false,
    val signTagsByDefault: Boolean = false,
) {
    val hasOverrides: Boolean =
        format || openPgpProgram || signingKey || signCommitsByDefault || signTagsByDefault
}

data class RepositoryGitSigningSettings(
    val settings: GitSigningSettings,
    val overrides: GitSigningSettingsOverrides,
)

data class GitSigningKeyDiscoveryResult(
    val options: List<GitSigningKeyOption>,
    val message: String,
)
