package com.jetpackduba.gitnuro.ui.dialogs.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.extensions.handOnHover
import com.jetpackduba.gitnuro.extensions.toSmartSystemString
import com.jetpackduba.gitnuro.generated.resources.*
import com.jetpackduba.gitnuro.managers.Error
import com.jetpackduba.gitnuro.models.GitSigningSettingsField
import com.jetpackduba.gitnuro.models.GitSigningSettingsScope
import com.jetpackduba.gitnuro.preferences.AvatarProviderType
import com.jetpackduba.gitnuro.repositories.DEFAULT_UI_SCALE
import com.jetpackduba.gitnuro.theme.*
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.components.AppSwitch
import com.jetpackduba.gitnuro.ui.components.PrimaryButton
import com.jetpackduba.gitnuro.ui.components.ScrollableColumn
import com.jetpackduba.gitnuro.ui.components.SecondaryButton
import com.jetpackduba.gitnuro.ui.components.tooltip.InstantTooltip
import com.jetpackduba.gitnuro.ui.context_menu.ContextMenuElement
import com.jetpackduba.gitnuro.ui.context_menu.DropDownMenu
import com.jetpackduba.gitnuro.ui.dialogs.base.MaterialDialog
import com.jetpackduba.gitnuro.ui.dialogs.errors.ErrorDialog
import com.jetpackduba.gitnuro.ui.dropdowns.DropDownOption
import com.jetpackduba.gitnuro.viewmodels.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.jgit.lib.GpgConfig
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.time.Instant

sealed interface SettingsEntry {
    data class Section(val name: StringResource) : SettingsEntry

    data class Entry(
        val icon: DrawableResource,
        val name: StringResource,
        val content: @Composable (SettingsViewModel) -> Unit,
    ) :
        SettingsEntry
}

val settings = listOf(
    SettingsEntry.Section(Res.string.settings_section_user_interface),
    SettingsEntry.Entry(Res.drawable.palette, Res.string.settings_entry_appearance) { Appearance(it) },
    SettingsEntry.Entry(Res.drawable.layout, Res.string.settings_entry_layout) { Layout(it) },
    SettingsEntry.Entry(Res.drawable.schedule, Res.string.settings_entry_datetime) { DateTime(it) },

    SettingsEntry.Section(Res.string.settings_section_git),
    SettingsEntry.Entry(Res.drawable.folder, Res.string.settings_entry_environment) { Environment(it) },
    SettingsEntry.Entry(Res.drawable.key, Res.string.settings_entry_signing) { Signing(it) },
    SettingsEntry.Entry(Res.drawable.branch, Res.string.settings_entry_branches) { Branches(it) },
    SettingsEntry.Entry(Res.drawable.cloud, Res.string.settings_entry_remote_actions) { RemoteActions(it) },

    SettingsEntry.Section(Res.string.settings_section_network),
    SettingsEntry.Entry(Res.drawable.network, Res.string.settings_entry_proxy) { Proxy(it) },
    SettingsEntry.Entry(Res.drawable.password, Res.string.settings_entry_auth) { Authentication(it) },
    SettingsEntry.Entry(Res.drawable.security, Res.string.settings_entry_security) { Security(it) },

    SettingsEntry.Section(Res.string.settings_section_tools),
    SettingsEntry.Entry(Res.drawable.terminal, Res.string.settings_entry_terminal) { Terminal(it) },
    SettingsEntry.Entry(Res.drawable.info, Res.string.settings_entry_logs) { Logs(it) },
)

val linesHeightTypesList = listOf(
    DropDownOption(LinesHeightType.SPACED, "Spaced"),
    DropDownOption(LinesHeightType.COMPACT, "Compact"),
)

@Composable
fun Proxy(settingsViewModel: SettingsViewModel) {
    var useProxy by remember { mutableStateOf(settingsViewModel.useProxy) }

    var hostName by remember { mutableStateOf(settingsViewModel.proxyHostName) }
    var portNumber by remember { mutableStateOf(settingsViewModel.proxyPortNumber) }

    var useAuth by remember { mutableStateOf(settingsViewModel.proxyUseAuth) }
    var user by remember { mutableStateOf(settingsViewModel.proxyHostUser) }
    var password by remember { mutableStateOf(settingsViewModel.proxyHostPassword) }

    val proxyTypes = listOf(ProxyType.HTTP, ProxyType.SOCKS)
    val proxyTypesDropDownOptions = proxyTypes.map { DropDownOption(it, it.name) }
    val proxySettings = settingsViewModel.proxyFlow.collectAsState()

    Column {
        SettingToggle(
            title = "Use proxy",
            subtitle = "Set up your proxy configuration if needed",
            value = useProxy,
            onValueChanged = {
                useProxy = it
                settingsViewModel.useProxy = it
            },
        )

        SettingDropDown(
            title = "Proxy type",
            subtitle = "Pick between HTTP or SOCKS",
            dropDownOptions = proxyTypesDropDownOptions,
            currentOption = proxySettings.value.proxyType,
            onOptionSelected = {
                settingsViewModel.proxyType = it.value
            }
        )

        SettingTextInput(
            title = "Host name",
            subtitle = "",
            value = hostName,
            enabled = useProxy,
            onValueChanged = {
                hostName = it
                settingsViewModel.proxyHostName = it
            },
        )

        SettingIntInput(
            title = "Port number",
            subtitle = "",
            value = portNumber,
            onValueChanged = {
                portNumber = it
                settingsViewModel.proxyPortNumber = it
            },
            enabled = useProxy,
        )

        SettingToggle(
            title = "Proxy authentication",
            subtitle = "Use your credentials to provide your identity the proxy server",
            value = useAuth,
            onValueChanged = {
                useAuth = it
                settingsViewModel.proxyUseAuth = it
            }
        )

        SettingTextInput(
            title = "Login",
            subtitle = "",
            value = user,
            enabled = useProxy && useAuth,
            onValueChanged = {
                user = it
                settingsViewModel.proxyHostUser = it
            },
        )


        SettingTextInput(
            title = "Password",
            subtitle = "",
            value = password,
            enabled = useProxy && useAuth,
            isPassword = true,
            onValueChanged = {
                password = it
                settingsViewModel.proxyHostPassword = it
            },
        )

    }
}

@Composable
fun SettingsDialog(
    settingsViewModel: SettingsViewModel,
    repositoryPath: String?,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(repositoryPath) {
        settingsViewModel.setGitSigningRepositoryPath(repositoryPath)
    }

    var selectedCategory by remember {
        mutableStateOf(
            settings.filterIsInstance<SettingsEntry.Entry>().first()
        )
    }

    MaterialDialog(
        background = MaterialTheme.colors.surface,
        onCloseRequested = {
            onDismiss()
        },
        paddingHorizontal = 0.dp,
        paddingVertical = 0.dp,
    ) {
        Row(modifier = Modifier.height(720.dp).width(1000.dp)) {
            Column(
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colors.background)
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.h3,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold,
                )

                Row(modifier = Modifier.weight(1f)) {
                    ScrollableColumn(
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        settings.forEachIndexed { index, settingEntry ->
                            when (settingEntry) {
                                is SettingsEntry.Section -> {
                                    if (index != 0) {
                                        Spacer(Modifier.height(16.dp))
                                    }
                                    Section(stringResource(settingEntry.name))
                                }

                                is SettingsEntry.Entry -> Entry(
                                    icon = settingEntry.icon,
                                    name = stringResource(settingEntry.name),
                                    isSelected = settingEntry == selectedCategory,
                                    onClick = {
                                        selectedCategory = settingEntry
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Column {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, true)
                        .padding(start = 16.dp, top = 64.dp, end = 16.dp)
                ) {
                    selectedCategory.content(settingsViewModel)
                }

                PrimaryButton(
                    text = "Accept",
                    modifier = Modifier
                        .padding(end = 16.dp, bottom = 16.dp)
                        .align(Alignment.End),
                    onClick = {
                        onDismiss()
                    },
                )
            }

        }
    }
}

@Composable
private fun Entry(icon: DrawableResource, name: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected)
        MaterialTheme.colors.backgroundSelected
    else
        MaterialTheme.colors.background

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color = backgroundColor)
            .handMouseClickable(onClick)
            .fillMaxWidth(),
    ) {
        Icon(
            painterResource(icon),
            contentDescription = name,
            tint = MaterialTheme.colors.onBackgroundSecondary,
            modifier = Modifier
                .padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 8.dp)
                .size(24.dp)
        )

        Text(
            text = name,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onBackground,
        )
    }
}

@Composable
private fun Section(name: String) {
    Text(
        text = name.uppercase(),
        color = MaterialTheme.colors.onBackgroundSecondary,
        style = MaterialTheme.typography.body2,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun RemoteActions(settingsViewModel: SettingsViewModel) {
    val pullRebase by settingsViewModel.pullRebaseFlow.collectAsState()
    val pushWithLease by settingsViewModel.pushWithLeaseFlow.collectAsState()

    SettingToggle(
        title = "Pull with rebase as default",
        subtitle = "Rebase changes instead of merging when pulling",
        value = pullRebase,
        onValueChanged = { value ->
            settingsViewModel.pullRebase = value
        }
    )

    SettingToggle(
        title = "Force push with lease",
        subtitle = "Check if the local version remote branch is up to date to avoid accidentally overriding unintended commits",
        value = pushWithLease,
        onValueChanged = { value ->
            settingsViewModel.pushWithLease = value
        }
    )
}


@Composable
private fun Authentication(settingsViewModel: SettingsViewModel) {
    val cacheCredentialsInMemory by settingsViewModel.cacheCredentialsInMemoryFlow.collectAsState()

    SettingToggle(
        title = "Cache HTTP credentials in memory",
        subtitle = "If active, HTTP Credentials will be remembered until Gitnuro is closed",
        value = cacheCredentialsInMemory,
        onValueChanged = { value ->
            settingsViewModel.cacheCredentialsInMemory = value
        }
    )
}

@Composable
private fun Security(settingsViewModel: SettingsViewModel) {
    val verifySsl by settingsViewModel.verifySslFlow.collectAsState()

    SettingToggle(
        title = "Do not verify SSL security",
        subtitle = "If active, you may connect to the remote server via insecure HTTPS connection",
        value = !verifySsl,
        onValueChanged = { value ->
            settingsViewModel.verifySsl = !value
        }
    )
}

@Composable
fun Terminal(settingsViewModel: SettingsViewModel) {
    var commitsLimit by remember { mutableStateOf(settingsViewModel.terminalPath) }

    SettingTextInput(
        title = "Custom terminal path",
        subtitle = "If empty, Gitnuro will try to open the default terminal emulator",
        value = commitsLimit,
        onValueChanged = { value ->
            commitsLimit = value
            settingsViewModel.terminalPath = value
        },
    )
}

@Composable
fun Logs(settingsViewModel: SettingsViewModel) {
    SettingButton(
        title = "Logs",
        subtitle = "Open the logs folder",
        buttonText = "Open folder",
        onClick = {
            settingsViewModel.openLogsFolderInFileExplorer()
        }
    )
}


@Composable
private fun Environment(settingsViewModel: SettingsViewModel) {
    var defaultCloneDir by remember { mutableStateOf(settingsViewModel.defaultCloneDir) }

    SettingTextInput(
        title = stringResource(Res.string.settings_environment_default_clone_directory_title),
        subtitle = stringResource(Res.string.settings_environment_default_clone_directory_description),
        value = defaultCloneDir,
        onValueChanged = { value ->
            defaultCloneDir = value
            settingsViewModel.defaultCloneDir = value
        },
    )
}

@Composable
private fun Signing(settingsViewModel: SettingsViewModel) {
    val signingSettings by settingsViewModel.gitSigningSettingsFlow.collectAsState()
    val signingOverrides by settingsViewModel.gitSigningOverridesFlow.collectAsState()
    val signingScope by settingsViewModel.gitSigningScopeFlow.collectAsState()
    val repositoryPath by settingsViewModel.gitSigningRepositoryPathFlow.collectAsState()
    val discoveredSigningKeys by settingsViewModel.gitSigningKeysFlow.collectAsState()
    val signingKeysMessage by settingsViewModel.gitSigningKeysMessageFlow.collectAsState()
    val isLoadingSigningKeys by settingsViewModel.isLoadingGitSigningKeysFlow.collectAsState()

    var openPgpProgram by remember(signingSettings.openPgpProgram) {
        mutableStateOf(signingSettings.openPgpProgram)
    }
    var signingKey by remember(signingSettings.signingKey) {
        mutableStateOf(signingSettings.signingKey)
    }

    val formatOptions = listOf(
        DropDownOption(GpgConfig.GpgFormat.OPENPGP, "OpenPGP"),
        DropDownOption(GpgConfig.GpgFormat.SSH, "SSH"),
    )

    val scopeOptions = buildList {
        add(DropDownOption(GitSigningSettingsScope.GLOBAL, "Global (~/.gitconfig)"))
        if (repositoryPath != null) {
            add(DropDownOption(GitSigningSettingsScope.REPOSITORY, "Repository (.git/config)"))
        }
    }

    val signingKeyOptions = remember(discoveredSigningKeys, signingKey, isLoadingSigningKeys) {
        buildList {
            if (signingKey.isBlank()) {
                add(
                    DropDownOption(
                        value = "",
                        optionName = if (isLoadingSigningKeys) {
                            "Loading signing keys..."
                        } else {
                            "No signing key selected"
                        },
                    )
                )
            } else if (discoveredSigningKeys.none { it.value == signingKey }) {
                add(DropDownOption(signingKey, "Current value — $signingKey"))
            }

            discoveredSigningKeys.forEach { keyOption ->
                add(DropDownOption(keyOption.value, keyOption.label))
            }

            if (isEmpty()) {
                add(
                    DropDownOption(
                        value = "",
                        optionName = if (isLoadingSigningKeys) {
                            "Loading signing keys..."
                        } else {
                            "No signing keys detected"
                        },
                    )
                )
            }
        }
    }

    val currentSigningKey = signingKeyOptions.firstOrNull { it.value == signingKey }?.value
        ?: signingKeyOptions.first().value

    val signingKeyTextTitle = if (signingSettings.format == GpgConfig.GpgFormat.SSH) {
        "SSH signing key"
    } else {
        "OpenPGP signing key"
    }

    val signingKeyTextSubtitle = if (signingSettings.format == GpgConfig.GpgFormat.SSH) {
        "Absolute path to the private SSH key file used for signing"
    } else {
        "Key ID or fingerprint used for OpenPGP signing"
    }

    val signingKeyDropDownSubtitle = if (isLoadingSigningKeys) {
        "Refreshing available keys..."
    } else {
        signingKeysMessage.ifBlank { "Select a detected signing key or enter one manually below." }
    }

    val isRepositoryScope = signingScope == GitSigningSettingsScope.REPOSITORY
    val formatSubtitle = withOverrideStatus(
        if (isRepositoryScope) "Stored in this repository's .git/config" else "Choose whether Git signs with OpenPGP or SSH keys",
        isRepositoryScope,
        signingOverrides.format,
    )
    val signingKeyEffectiveSubtitle = withOverrideStatus(
        signingKeyTextSubtitle,
        isRepositoryScope,
        signingOverrides.signingKey,
    )
    val openPgpProgramSubtitle = withOverrideStatus(
        "Executable used for OpenPGP signing, for example gpg or gpg2",
        isRepositoryScope,
        signingOverrides.openPgpProgram,
    )
    val signCommitsSubtitle = withOverrideStatus(
        if (isRepositoryScope) "Controls commit.gpgSign in this repository's config" else "Controls commit.gpgSign in your global Git config",
        isRepositoryScope,
        signingOverrides.signCommitsByDefault,
    )
    val signTagsSubtitle = withOverrideStatus(
        if (isRepositoryScope) "Controls tag.gpgSign in this repository's config" else "Controls tag.gpgSign in your global Git config",
        isRepositoryScope,
        signingOverrides.signTagsByDefault,
    )

    if (repositoryPath != null) {
        SettingDropDown(
            title = "Settings scope",
            subtitle = "Choose whether to edit global defaults or repository-specific overrides",
            dropDownOptions = scopeOptions,
            currentOption = signingScope,
            onOptionSelected = { scopeOption ->
                settingsViewModel.setGitSigningScope(scopeOption.value)
            },
            width = 220.dp,
        )
    }

    if (isRepositoryScope && signingOverrides.hasOverrides) {
        SettingButton(
            title = "Repository signing overrides",
            subtitle = "Remove all repository-specific signing settings and inherit the global values again",
            buttonText = "Use global defaults",
            onClick = {
                settingsViewModel.clearRepositoryGitSigningOverrides()
            }
        )
    }

    SettingDropDown(
        title = "Signing format",
        subtitle = formatSubtitle,
        dropDownOptions = formatOptions,
        currentOption = signingSettings.format,
        onOptionSelected = { formatOption ->
            settingsViewModel.setGitSigningFormat(formatOption.value)
        },
        actionButtonText = if (isRepositoryScope && signingOverrides.format) "Use global" else null,
        onActionButtonClick = if (isRepositoryScope && signingOverrides.format) {
            { settingsViewModel.unsetRepositoryGitSigningSetting(GitSigningSettingsField.FORMAT) }
        } else {
            null
        },
    )

    SettingDropDown(
        title = "Signing key",
        subtitle = signingKeyDropDownSubtitle,
        dropDownOptions = signingKeyOptions,
        currentOption = currentSigningKey,
        onOptionSelected = { option ->
            signingKey = option.value
            settingsViewModel.setGitSigningKey(option.value)
        },
        width = 460.dp,
    )

    SettingTextInput(
        title = signingKeyTextTitle,
        subtitle = signingKeyEffectiveSubtitle,
        value = signingKey,
        onValueChanged = { value ->
            signingKey = value
            settingsViewModel.setGitSigningKey(value)
        },
        actionButtonText = if (isRepositoryScope && signingOverrides.signingKey) "Use global" else null,
        onActionButtonClick = if (isRepositoryScope && signingOverrides.signingKey) {
            { settingsViewModel.unsetRepositoryGitSigningSetting(GitSigningSettingsField.SIGNING_KEY) }
        } else {
            null
        },
    )

    if (signingSettings.format == GpgConfig.GpgFormat.OPENPGP) {
        SettingTextInput(
            title = "GPG program",
            subtitle = openPgpProgramSubtitle,
            value = openPgpProgram,
            onValueChanged = { value ->
                openPgpProgram = value
                settingsViewModel.setGitSigningOpenPgpProgram(value)
            },
            actionButtonText = if (isRepositoryScope && signingOverrides.openPgpProgram) "Use global" else null,
            onActionButtonClick = if (isRepositoryScope && signingOverrides.openPgpProgram) {
                { settingsViewModel.unsetRepositoryGitSigningSetting(GitSigningSettingsField.OPENPGP_PROGRAM) }
            } else {
                null
            },
        )

        SettingButton(
            title = "Browse GPG program",
            subtitle = "Select the OpenPGP executable from disk",
            buttonText = "Choose file",
            onClick = {
                settingsViewModel.pickGitSigningOpenPgpProgram()?.let { filePath ->
                    openPgpProgram = filePath
                }
            }
        )
    } else {
        SettingButton(
            title = "Browse SSH key",
            subtitle = "Select the private SSH key file used for signing",
            buttonText = "Choose file",
            onClick = {
                settingsViewModel.pickGitSigningKeyFile()?.let { filePath ->
                    signingKey = filePath
                }
            }
        )
    }

    SettingButton(
        title = "Refresh detected keys",
        subtitle = if (signingSettings.format == GpgConfig.GpgFormat.SSH) {
            "Rescan ~/.ssh for private keys"
        } else {
            "Rescan secret OpenPGP keys using the configured GPG program"
        },
        buttonText = if (isLoadingSigningKeys) "Refreshing..." else "Refresh",
        onClick = {
            settingsViewModel.refreshGitSigningKeys()
        }
    )

    SettingToggle(
        title = "Sign commits by default",
        subtitle = signCommitsSubtitle,
        value = signingSettings.signCommitsByDefault,
        onValueChanged = { value ->
            settingsViewModel.setSignCommitsByDefault(value)
        },
        actionButtonText = if (isRepositoryScope && signingOverrides.signCommitsByDefault) "Use global" else null,
        onActionButtonClick = if (isRepositoryScope && signingOverrides.signCommitsByDefault) {
            { settingsViewModel.unsetRepositoryGitSigningSetting(GitSigningSettingsField.SIGN_COMMITS) }
        } else {
            null
        },
    )

    SettingToggle(
        title = "Sign tags by default",
        subtitle = signTagsSubtitle,
        value = signingSettings.signTagsByDefault,
        onValueChanged = { value ->
            settingsViewModel.setSignTagsByDefault(value)
        },
        actionButtonText = if (isRepositoryScope && signingOverrides.signTagsByDefault) "Use global" else null,
        onActionButtonClick = if (isRepositoryScope && signingOverrides.signTagsByDefault) {
            { settingsViewModel.unsetRepositoryGitSigningSetting(GitSigningSettingsField.SIGN_TAGS) }
        } else {
            null
        },
    )
}

private fun withOverrideStatus(
    baseSubtitle: String,
    isRepositoryScope: Boolean,
    isOverridden: Boolean,
): String {
    if (!isRepositoryScope) {
        return baseSubtitle
    }

    val status = if (isOverridden) {
        "Status: explicitly overridden in this repository"
    } else {
        "Status: inherited from global/default Git config"
    }

    return if (baseSubtitle.isBlank()) status else "$baseSubtitle\n$status"
}

@Composable
private fun Branches(settingsViewModel: SettingsViewModel) {
    val ffMerge by settingsViewModel.ffMergeFlow.collectAsState()
    val mergeAutoStash by settingsViewModel.mergeAutoStashFlow.collectAsState()

    SettingToggle(
        title = "Fast-forward merge",
        subtitle = "Try to fast-forward merges when possible",
        value = ffMerge,
        onValueChanged = { value ->
            settingsViewModel.ffMerge = value
        }
    )

    SettingToggle(
        title = "Automatically stash uncommitted changes before merge",
        subtitle = "To avoid losing work if the merge is aborted, the app can create a snapshot of the uncommitted changes",
        value = mergeAutoStash,
        onValueChanged = { value ->
            settingsViewModel.mergeAutoStash = value
        }
    )
}

@Composable
private fun Layout(settingsViewModel: SettingsViewModel) {
    val swapUncommittedChanges by settingsViewModel.swapUncommittedChangesFlow.collectAsState()

    SettingToggle(
        title = "Swap position for staged/unstaged views",
        subtitle = "Show the list of unstaged changes above the list of staged changes",
        value = swapUncommittedChanges,
        onValueChanged = { value ->
            settingsViewModel.swapUncommittedChanges = value
        }
    )
}

@Composable
private fun DateTime(settingsViewModel: SettingsViewModel) {
    val dateFormat by settingsViewModel.dateFormatFlow.collectAsState()
    var customFormat by remember(settingsViewModel) { mutableStateOf(dateFormat.customFormat) }
    var isError by remember { mutableStateOf(false) }

    val currentInstant = remember { Instant.now() }

    val currentDateSystemDefault =
        currentInstant.toSmartSystemString(allowRelative = false, useSystemDefaultFormat = true)

    SettingToggle(
        title = "Use system's Date/Time format",
        subtitle = "If enabled, current date would be shown as \"$currentDateSystemDefault\"",
        value = dateFormat.useSystemDefault,
        onValueChanged = { value ->
            settingsViewModel.dateFormat = dateFormat.copy(useSystemDefault = value)
        }
    )

    val customFormatSubtitle = if (isError) {
        "Invalid date/time format"
    } else {
        val currentDate = currentInstant.toSmartSystemString(allowRelative = false, useSystemDefaultFormat = false)
        "Current date would be shown as \"$currentDate\""
    }

    SettingTextInput(
        title = "Custom date format",
        subtitle = customFormatSubtitle,
        value = customFormat,
        isError = isError,
        onValueChanged = { value ->
            customFormat = value
            if (settingsViewModel.isValidDateFormat(value)) {
                settingsViewModel.dateFormat = dateFormat.copy(customFormat = value)
                isError = false
            } else {
                isError = true
            }

        },
        enabled = !dateFormat.useSystemDefault,
    )

    val is24hSubtitle = if (dateFormat.is24hours) {
        "17:30"
    } else {
        "05:30 PM"
    }

    SettingToggle(
        title = "Use 24h time",
        subtitle = is24hSubtitle,
        enabled = !dateFormat.useSystemDefault,
        value = dateFormat.is24hours,
        onValueChanged = { value ->
            settingsViewModel.dateFormat = dateFormat.copy(is24hours = value)
        }
    )

    SettingToggle(
        title = "Relative date",
        subtitle = "Use \"Today\" and \"Yesterday\" instead of the date",
        value = dateFormat.useRelativeDate,
        onValueChanged = { value ->
            settingsViewModel.dateFormat = dateFormat.copy(useRelativeDate = value)
        }
    )
}

@Composable
private fun Appearance(settingsViewModel: SettingsViewModel) {
    val currentTheme by settingsViewModel.themeState.collectAsState()
    val currentLinesHeightType by settingsViewModel.linesHeightTypeState.collectAsState()
    val avatarProvider by settingsViewModel.avatarProviderFlow.collectAsState()
    val (errorToDisplay, setErrorToDisplay) = remember { mutableStateOf<Error?>(null) }

    SettingDropDown(
        title = "Theme",
        subtitle = "Select the UI theme between light and dark mode",
        dropDownOptions = themeLists,
        currentOption = currentTheme,
        onOptionSelected = { themeDropDown ->
            settingsViewModel.theme = themeDropDown.value
        }
    )

    if (currentTheme == Theme.CUSTOM) {
        SettingButton(
            title = "Custom theme",
            subtitle = "Select a JSON file to load the custom theme",
            buttonText = "Open file",
            onClick = {
                val filePath = settingsViewModel.openFileDialog()

                if (filePath != null) {
                    val error = settingsViewModel.saveCustomTheme(filePath)

                    // We check if it's null because setting errorToDisplay to null could possibly hide
                    // other errors that are being displayed
                    if (error != null) {
                        setErrorToDisplay(error)
                    }
                }
            }
        )
    }

    SettingDropDown(
        title = "Lists spacing (Beta)",
        subtitle = "Spacing around lists items",
        dropDownOptions = linesHeightTypesList,
        currentOption = currentLinesHeightType,
        onOptionSelected = { dropDown ->
            settingsViewModel.linesHeightType = dropDown.value
        }
    )

    val density = LocalDensity.current.density
    var options by remember {
        mutableStateOf(
            listOf(
                DropDownOption(0.75f, "75%"),
                DropDownOption(1f, "100%"),
                DropDownOption(1.25f, "125%"),
                DropDownOption(1.5f, "150%"),
                DropDownOption(1.75f, "175%"),
                DropDownOption(2f, "200%"),
                DropDownOption(2.25f, "225%"),
                DropDownOption(2.5f, "250%"),
                DropDownOption(2.75f, "275%"),
                DropDownOption(3f, "300%"),
            )
        )
    }

    var scaleValue by remember {
        val savedScaleUi = settingsViewModel.scaleUi
        val scaleUi = if (savedScaleUi == DEFAULT_UI_SCALE) {
            density
        } else {
            savedScaleUi
        }

        var matchingOption = options.firstOrNull { it.value == scaleUi }

        if (matchingOption == null) { // Scale that we haven't taken in consideration
            // Create a new scale and add it to the options list
            matchingOption = DropDownOption(scaleUi, "${(scaleUi * 100).toInt()}%")
            val newOptions = options.toMutableList()
            newOptions.add(matchingOption)
            newOptions.sortBy { it.value }
            options = newOptions
        }

        mutableStateOf(matchingOption)
    }

    SettingDropDown(
        title = "Scale",
        subtitle = "Adapt the size the UI to your preferred scale",
        dropDownOptions = options,
        currentOption = scaleValue.value,
        onOptionSelected = { newValue ->
            scaleValue = newValue
            settingsViewModel.scaleUi = newValue.value
        }
    )

    SettingDropDown(
        title = "Avatar provider",
        subtitle = "When using a provider, the e-mail addresses will be hashed using SHA256",
        currentOption = avatarProvider,
        dropDownOptions = listOf(
            DropDownOption(AvatarProviderType.NONE, "None"),
            DropDownOption(AvatarProviderType.GRAVATAR, "Gravatar"),
        ),
        onOptionSelected = {
            settingsViewModel.avatarProvider = it.value
        }
    )

    if (errorToDisplay != null) {
        ErrorDialog(
            errorToDisplay,
            onAccept = { setErrorToDisplay(null) }
        )
    }
}


@Composable
fun <T> SettingDropDown(
    title: String,
    subtitle: String,
    dropDownOptions: List<DropDownOption<T>>,
    onOptionSelected: (DropDownOption<T>) -> Unit,
    currentOption: T,
    width: Dp = 180.dp,
    actionButtonText: String? = null,
    onActionButtonClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FieldTitles(title, subtitle)

        Spacer(modifier = Modifier.weight(1f))

        val selectedOption = dropDownOptions.first { it.value == currentOption }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (actionButtonText != null && onActionButtonClick != null) {
                SecondaryButton(
                    text = actionButtonText,
                    onClick = onActionButtonClick,
                )

                Spacer(modifier = Modifier.width(8.dp))
            }

            Box {
                DropDownMenu(
                    showIcons = false,
                    items = {
                        dropDownOptions.map {
                            ContextMenuElement.ContextTextEntry(it.optionName, onClick = { onOptionSelected(it) })
                        }
                    },
                ) {
                    InstantTooltip(text = selectedOption.optionName) {
                        Row(
                            modifier = Modifier.width(width)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.1F),
                                    shape = RoundedCornerShape(4.dp),
                                )
                                .clip(shape = RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colors.background)
                                .padding(vertical = 8.dp, horizontal = 12.dp)
                                .handOnHover(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = selectedOption.optionName,
                                style = MaterialTheme.typography.body2,
                                color = MaterialTheme.colors.onBackground,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )

                            Icon(
                                painter = painterResource(Res.drawable.dropdown),
                                contentDescription = null,
                                tint = MaterialTheme.colors.onBackground,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingButton(
    title: String,
    subtitle: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FieldTitles(title, subtitle)

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            text = buttonText,
            onClick = onClick,
        )
    }
}

@Composable
fun SettingToggle(
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    value: Boolean,
    onValueChanged: (Boolean) -> Unit,
    actionButtonText: String? = null,
    onActionButtonClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FieldTitles(title, subtitle, enabled)

        Spacer(modifier = Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (actionButtonText != null && onActionButtonClick != null) {
                SecondaryButton(
                    text = actionButtonText,
                    onClick = onActionButtonClick,
                )

                Spacer(modifier = Modifier.width(8.dp))
            }

            AppSwitch(
                enabled = enabled,
                isChecked = value,
                onValueChanged = onValueChanged,
            )
        }
    }
}

@Composable
fun SettingIntInput(
    title: String,
    subtitle: String,
    value: Int,
    enabled: Boolean = true,
    onValueChanged: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FieldTitles(title, subtitle, enabled)

        Spacer(modifier = Modifier.weight(1f))

        var text by remember {
            mutableStateOf(value.toString())
        }

        var isError by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        AdjustableOutlinedTextField(
            value = text,
            modifier = Modifier.width(136.dp),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            isError = isError,
            enabled = enabled,
            onValueChange = {
                val textFiltered = it.filter { c -> c.isDigit() }
                if (textFiltered.isEmpty() || isValidInt(textFiltered)) {
                    isError = false

                    val newValue = textFiltered.toIntOrNull() ?: 0
                    text = newValue.toString()
                    onValueChanged(newValue)
                } else {
                    scope.launch {
                        isError = true
                        delay(500) // Show an error
                        isError = false
                    }
                }
            },
            colors = outlinedTextFieldColors(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
        )
    }
}

@Composable
fun SettingTextInput(
    title: String,
    subtitle: String,
    value: String,
    enabled: Boolean = true,
    isPassword: Boolean = false,
    onValueChanged: (String) -> Unit,
    isError: Boolean = false,
    actionButtonText: String? = null,
    onActionButtonClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FieldTitles(title, subtitle, enabled)

        Spacer(modifier = Modifier.weight(1f))

        var text by remember {
            mutableStateOf(value)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (actionButtonText != null && onActionButtonClick != null) {
                SecondaryButton(
                    text = actionButtonText,
                    onClick = onActionButtonClick,
                )

                Spacer(modifier = Modifier.width(8.dp))
            }

            AdjustableOutlinedTextField(
                value = text,
                modifier = Modifier.width(240.dp),
                isError = isError,
                enabled = enabled,
                onValueChange = {
                    text = it
                    onValueChanged(it)
                },
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                colors = outlinedTextFieldColors(),
                singleLine = true,
            )
        }
    }
}

@Composable
private fun FieldTitles(
    title: String,
    subtitle: String,
    enabled: Boolean = true,
) {
    Column(
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            color = MaterialTheme.colors.onBackground.copy(alpha = if (enabled) 1F else 0.6F),
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Medium,
        )

        Text(
            text = subtitle,
            color = MaterialTheme.colors.onBackground.copy(alpha = if (enabled) 1F else 0.6F),
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.body2,
        )
    }
}

private fun isValidInt(value: String): Boolean {
    return try {
        value.toInt()
        true
    } catch (ex: Exception) {
        false
    }
}

private fun isValidFloat(value: String): Boolean {
    return try {
        value.toFloat()
        true
    } catch (ex: Exception) {
        false
    }
}

enum class ProxyType(val value: Int) {
    HTTP(1),
    SOCKS(2);

    companion object {
        fun fromInt(value: Int): ProxyType {
            return when (value) {
                HTTP.value -> HTTP
                SOCKS.value -> SOCKS
                else -> throw NotImplementedError("Proxy type unknown")
            }
        }
    }
}