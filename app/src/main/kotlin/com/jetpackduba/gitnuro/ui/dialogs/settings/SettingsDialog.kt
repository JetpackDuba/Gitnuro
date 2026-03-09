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
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.extensions.handOnHover
import com.jetpackduba.gitnuro.extensions.toSmartSystemString
import com.jetpackduba.gitnuro.app.generated.resources.*
import com.jetpackduba.gitnuro.domain.models.AppConfig
import com.jetpackduba.gitnuro.domain.models.Error
import com.jetpackduba.gitnuro.domain.models.AvatarProviderType
import com.jetpackduba.gitnuro.domain.models.ProxyType
import com.jetpackduba.gitnuro.domain.models.ui.LinesHeightType
import com.jetpackduba.gitnuro.domain.models.ui.Theme
import com.jetpackduba.gitnuro.theme.*
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.components.AppSwitch
import com.jetpackduba.gitnuro.ui.components.PrimaryButton
import com.jetpackduba.gitnuro.ui.components.ScrollableColumn
import com.jetpackduba.gitnuro.ui.context_menu.ContextMenuElement
import com.jetpackduba.gitnuro.ui.context_menu.DropDownMenu
import com.jetpackduba.gitnuro.ui.dialogs.base.MaterialDialog
import com.jetpackduba.gitnuro.ui.dialogs.errors.ErrorDialog
import com.jetpackduba.gitnuro.ui.dropdowns.DropDownOption
import com.jetpackduba.gitnuro.viewmodels.SettingsAction
import com.jetpackduba.gitnuro.viewmodels.SettingsViewModel
import com.jetpackduba.gitnuro.viewmodels.SettingsViewState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        val content: @Composable (SettingsViewState, onAction: (SettingsAction) -> Unit) -> Unit,
    ) :
        SettingsEntry
}

val settings = listOf(
    SettingsEntry.Section(Res.string.settings_section_user_interface),
    SettingsEntry.Entry(
        Res.drawable.palette,
        Res.string.settings_entry_appearance
    ) { state, onAction ->
        Appearance(state, onAction)
    },
    SettingsEntry.Entry(Res.drawable.layout, Res.string.settings_entry_layout) { state, onAction ->
        Layout(state, onAction)
    },
    SettingsEntry.Entry(Res.drawable.schedule, Res.string.settings_entry_datetime) { state, onAction ->
        DateTime(
            state,
            onAction
        )
    },

    SettingsEntry.Section(Res.string.settings_section_git),
    SettingsEntry.Entry(Res.drawable.folder, Res.string.settings_entry_environment) { state, onAction ->
        Environment(
            state,
            onAction
        )
    },
    SettingsEntry.Entry(Res.drawable.branch, Res.string.settings_entry_branches) { state, onAction ->
        Branches(
            state,
            onAction
        )
    },
    SettingsEntry.Entry(
        Res.drawable.cloud,
        Res.string.settings_entry_remote_actions
    ) { state, onAction -> RemoteActions(state, onAction) },

    SettingsEntry.Section(Res.string.settings_section_network),
    SettingsEntry.Entry(Res.drawable.network, Res.string.settings_entry_proxy) { state, onAction ->
        Proxy(
            state,
            onAction
        )
    },
    SettingsEntry.Entry(
        Res.drawable.password,
        Res.string.settings_entry_auth
    ) { state, onAction -> Authentication(state, onAction) },
    SettingsEntry.Entry(Res.drawable.security, Res.string.settings_entry_security) { state, onAction ->
        Security(
            state,
            onAction
        )
    },

    SettingsEntry.Section(Res.string.settings_section_tools),
    SettingsEntry.Entry(Res.drawable.terminal, Res.string.settings_entry_terminal) { state, onAction ->
        Terminal(
            state,
            onAction
        )
    },
    SettingsEntry.Entry(Res.drawable.info, Res.string.settings_entry_logs) { state, onAction -> Logs(state, onAction) },
)

val linesHeightTypesList = listOf(
    DropDownOption(LinesHeightType.SPACED, "Spaced"),
    DropDownOption(LinesHeightType.COMPACT, "Compact"),
)

@Composable
fun SettingsDialog(
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit,
) {
    var selectedCategory by remember {
        mutableStateOf(
            settings.filterIsInstance<SettingsEntry.Entry>().first()
        )
    }

    val settingsViewState by settingsViewModel.settingsViewState.collectAsState()

    MaterialDialog(
        background = MaterialTheme.colors.surface,
        onCloseRequested = {
            onDismiss()
        },
        paddingHorizontal = 0.dp,
        paddingVertical = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .height(720.dp)
                .width(1000.dp)
        ) {
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
                    selectedCategory.content(settingsViewState) { action ->
                        settingsViewModel.onAction(action)
                    }
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
fun Proxy(settingsViewState: SettingsViewState, onAction: (SettingsAction) -> Unit) {
    val useProxy = settingsViewState.useProxy
    val proxyUseAuth = settingsViewState.proxyUseAuth

    val proxyTypes = listOf(ProxyType.HTTP, ProxyType.SOCKS)
    val proxyTypesDropDownOptions = proxyTypes.map { DropDownOption(it, it.name) }

    Column {
        SettingToggle(
            title = "Use proxy",
            subtitle = "Set up your proxy configuration if needed",
            value = useProxy,
            onValueChanged = {
                onAction(SettingsAction.SetConfig(AppConfig.UseProxy(it)))
            },
        )

        SettingDropDown(
            title = "Proxy type",
            subtitle = "Pick between HTTP or SOCKS",
            dropDownOptions = proxyTypesDropDownOptions,
            currentOption = settingsViewState.proxyType,
            onOptionSelected = {
                onAction(SettingsAction.SetConfig(AppConfig.ProxyProxyType(it.value)))
            }
        )

        SettingTextInput(
            title = "Host name",
            subtitle = "",
            value = settingsViewState.proxyHostName.orEmpty(),
            enabled = useProxy,
            onValueChanged = {
                onAction(SettingsAction.SetConfig(AppConfig.ProxyHostName(it)))
            },
        )

        SettingIntInput(
            title = "Port number",
            subtitle = "",
            value = settingsViewState.proxyPortNumber ?: 0,
            onValueChanged = {
                onAction(SettingsAction.SetConfig(AppConfig.ProxyPortNumber(it)))
            },
            enabled = useProxy,
        )

        SettingToggle(
            title = "Proxy authentication",
            subtitle = "Use your credentials to provide your identity the proxy server",
            value = proxyUseAuth,
            onValueChanged = {
                onAction(SettingsAction.SetConfig(AppConfig.ProxyUseAuth(it)))
            }
        )

        SettingTextInput(
            title = "Login",
            subtitle = "",
            value = settingsViewState.proxyHostUser.orEmpty(),
            enabled = useProxy && proxyUseAuth,
            onValueChanged = {
                onAction(SettingsAction.SetConfig(AppConfig.ProxyHostPassword(it)))
            },
        )


        SettingTextInput(
            title = "Password",
            subtitle = "",
            value = settingsViewState.proxyHostPassword.orEmpty(),
            enabled = useProxy && proxyUseAuth,
            isPassword = true,
            onValueChanged = {
                onAction(SettingsAction.SetConfig(AppConfig.ProxyHostPassword(it)))
            },
        )

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
private fun RemoteActions(settingsViewState: SettingsViewState, onAction: (SettingsAction) -> Unit) {
    val pullRebase = settingsViewState.pullWithRebase
    val pushWithLease = settingsViewState.pushWithLease

    SettingToggle(
        title = "Pull with rebase as default",
        subtitle = "Rebase changes instead of merging when pulling",
        value = pullRebase,
        onValueChanged = { value ->
            onAction(SettingsAction.SetConfig(AppConfig.PullWithRebase(value)))
        }
    )

    SettingToggle(
        title = "Force push with lease",
        subtitle = "Check if the local version remote branch is up to date to avoid accidentally overriding unintended commits",
        value = pushWithLease,
        onValueChanged = { value ->
            onAction(SettingsAction.SetConfig(AppConfig.PushWithLease(value)))
        }
    )
}


@Composable
private fun Authentication(settingsViewState: SettingsViewState, onAction: (SettingsAction) -> Unit) {
    val cacheCredentialsInMemory = settingsViewState.cacheCredentialsInMemory

    SettingToggle(
        title = "Cache HTTP credentials in memory",
        subtitle = "If active, HTTP Credentials will be remembered until Gitnuro is closed",
        value = cacheCredentialsInMemory,
        onValueChanged = { value ->
            onAction(SettingsAction.SetConfig(AppConfig.CacheCredentialsInMemory(value)))
        }
    )
}

@Composable
private fun Security(settingsViewState: SettingsViewState, onAction: (SettingsAction) -> Unit) {
    val verifySsl = settingsViewState.verifySsl

    SettingToggle(
        title = "Do not verify SSL security",
        subtitle = "If active, you may connect to the remote server via insecure HTTPS connection",
        value = !verifySsl,
        onValueChanged = { value ->
            onAction(SettingsAction.SetConfig(AppConfig.CacheCredentialsInMemory(!value)))
        }
    )
}

@Composable
fun Terminal(settingsViewState: SettingsViewState, onAction: (SettingsAction) -> Unit) {
    val terminalPath = settingsViewState.terminalPath.orEmpty()

    SettingTextInput(
        title = "Custom terminal path",
        subtitle = "If empty, Gitnuro will try to open the default terminal emulator",
        value = terminalPath,
        onValueChanged = { value ->
            onAction(SettingsAction.SetConfig(AppConfig.TerminalPath(value)))
        },
    )
}

@Composable
fun Logs(settingsViewState: SettingsViewState, onAction: (SettingsAction) -> Unit) {
    SettingButton(
        title = "Logs",
        subtitle = "Open the logs folder",
        buttonText = "Open folder",
        onClick = {
            onAction(SettingsAction.OpenLogsFolder)
        }
    )
}


@Composable
private fun Environment(settingsViewState: SettingsViewState, onAction: (SettingsAction) -> Unit) {
    //var defaultCloneDir by remember { mutableStateOf(settingsViewModel.defaultCloneDir) }
    var defaultCloneDir = settingsViewState.cloneDefaultDirectory.orEmpty()

    SettingTextInput(
        title = stringResource(Res.string.settings_environment_default_clone_directory_title),
        subtitle = stringResource(Res.string.settings_environment_default_clone_directory_description),
        value = defaultCloneDir,
        onValueChanged = { value ->
            defaultCloneDir = value
            onAction(SettingsAction.SetConfig(AppConfig.CloneDefaultDirectory(value)))
        },
    )
}

@Composable
private fun Branches(settingsViewState: SettingsViewState, onAction: (SettingsAction) -> Unit) {
    val fastForwardMerge = settingsViewState.fastForwardMerge
    val mergeAutoStash = settingsViewState.autoStashOnMerge

    SettingToggle(
        title = "Fast-forward merge",
        subtitle = "Try to fast-forward merges when possible",
        value = fastForwardMerge,
        onValueChanged = { value ->
            onAction(SettingsAction.SetConfig(AppConfig.FastForwardMerge(value)))
        }
    )

    SettingToggle(
        title = "Automatically stash uncommitted changes before merge",
        subtitle = "To avoid losing work if the merge is aborted, the app can create a snapshot of the uncommitted changes",
        value = mergeAutoStash,
        onValueChanged = { value ->
            onAction(SettingsAction.SetConfig(AppConfig.AutoStashOnMerge(value)))
        }
    )
}

@Composable
private fun Layout(settingsViewState: SettingsViewState, onAction: (SettingsAction) -> Unit) {
    val swapUncommittedChanges = settingsViewState.swapStatusPanes

    SettingToggle(
        title = "Swap position for staged/unstaged views",
        subtitle = "Show the list of unstaged changes above the list of staged changes",
        value = swapUncommittedChanges,
        onValueChanged = { value ->
            onAction(SettingsAction.SetConfig(AppConfig.SwapStatusPanes(value)))
        }
    )
}

@Composable
private fun DateTime(settingsViewState: SettingsViewState, onAction: (SettingsAction) -> Unit) {
    val useDefault = settingsViewState.dateFormatUseDefault
    val customFormat = settingsViewState.dateFormatCustomFormat
    val is24h = settingsViewState.dateFormatIs24h
    val useRelative = settingsViewState.dateFormatUseRelative


    var isError by remember { mutableStateOf(false) }

    val currentInstant = remember { Instant.now() }

    val currentDateSystemDefault =
        currentInstant.toSmartSystemString(allowRelative = false, useSystemDefaultFormat = true)

    SettingToggle(
        title = "Use system's Date/Time format",
        subtitle = "If enabled, current date would be shown as \"$currentDateSystemDefault\"",
        value = useDefault,
        onValueChanged = { value ->
            onAction(SettingsAction.SetConfig(AppConfig.DateFormatUseDefault(value)))
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
            /*TODO
            customFormat = value
            if (settingsViewModel.isValidDateFormat(value)) {
                settingsViewModel.dateFormat = dateFormat.copy(customFormat = value)
                isError = false
            } else {
                isError = true
            }
            */
        },
        enabled = !useDefault,
    )

    val is24hSubtitle = if (is24h) { // TODO Fix this
        "17:30"
    } else {
        "05:30 PM"
    }

    SettingToggle(
        title = "Use 24h time",
        subtitle = is24hSubtitle,
        enabled = !useDefault,
        value = is24h,
        onValueChanged = { value ->
            onAction(SettingsAction.SetConfig(AppConfig.DateFormatIs24h(value)))
        }
    )

    SettingToggle(
        title = "Relative date",
        subtitle = "Use \"Today\" and \"Yesterday\" instead of the date",
        value = useRelative,
        onValueChanged = { value ->
            onAction(SettingsAction.SetConfig(AppConfig.DateFormatUseRelative(value)))
        }
    )
}

@Composable
private fun Appearance(settingsViewState: SettingsViewState, onAction: (SettingsAction) -> Unit) {
    val currentTheme = settingsViewState.theme
    val currentLinesHeightType = settingsViewState.linesHeightType
    val avatarProvider = settingsViewState.avatarProvider
    val (errorToDisplay, setErrorToDisplay) = remember { mutableStateOf<Error?>(null) }

    SettingDropDown(
        title = "Theme",
        subtitle = "Select the UI theme between light and dark mode",
        dropDownOptions = themeLists,
        currentOption = currentTheme,
        onOptionSelected = { themeDropDown ->
            onAction(SettingsAction.SetConfig(AppConfig.Theme(themeDropDown.value)))
        }
    )

    if (currentTheme == Theme.Custom) {
        SettingButton(
            title = "Custom theme",
            subtitle = "Select a JSON file to load the custom theme",
            buttonText = "Open file",
            onClick = {
                /*TODO
                val filePath = settingsViewModel.openFileDialog()

                if (filePath != null) {
                    val error = settingsViewModel.saveCustomTheme(filePath)

                    // We check if it's null because setting errorToDisplay to null could possibly hide
                    // other errors that are being displayed
                    if (error != null) {
                        setErrorToDisplay(error)
                    }
                }*/
            }
        )
    }

    SettingDropDown(
        title = "Lists spacing (Beta)",
        subtitle = "Spacing around lists items",
        dropDownOptions = linesHeightTypesList,
        currentOption = currentLinesHeightType,
        onOptionSelected = { dropDown ->
            onAction(SettingsAction.SetConfig(AppConfig.LinesHeight(dropDown.value)))
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
        val scaleUi = settingsViewState.scaleUi ?: density

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
            onAction(SettingsAction.SetConfig(AppConfig.ScaleUi(newValue.value)))
        }
    )

    SettingDropDown(
        title = "Avatar provider",
        subtitle = "When using a provider, the e-mail addresses will be hashed using SHA256",
        currentOption = avatarProvider,
        dropDownOptions = listOf(
            DropDownOption(AvatarProviderType.None, "None"),
            DropDownOption(AvatarProviderType.Gravatar, "Gravatar"),
        ),
        onOptionSelected = {
            onAction(SettingsAction.SetConfig(AppConfig.AvatarProvider(it.value)))
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
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FieldTitles(title, subtitle)

        Spacer(modifier = Modifier.weight(1f))

        Box {
            DropDownMenu(
                showIcons = false,
                items = {
                    dropDownOptions.map {
                        ContextMenuElement.ContextTextEntry(it.optionName, onClick = { onOptionSelected(it) })
                    }
                },
            ) {
                Row(
                    modifier = Modifier.width(180.dp)
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
                        text = dropDownOptions.first { it.value == currentOption }.optionName,
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
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FieldTitles(title, subtitle, enabled)

        Spacer(modifier = Modifier.weight(1f))

        AppSwitch(
            enabled = enabled,
            isChecked = value,
            onValueChanged = onValueChanged,
        )
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

