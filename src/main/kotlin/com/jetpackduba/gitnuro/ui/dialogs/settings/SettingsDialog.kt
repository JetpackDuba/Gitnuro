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
import com.jetpackduba.gitnuro.generated.resources.*
import com.jetpackduba.gitnuro.managers.Error
import com.jetpackduba.gitnuro.preferences.AvatarProviderType
import com.jetpackduba.gitnuro.repositories.DEFAULT_UI_SCALE
import com.jetpackduba.gitnuro.theme.*
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.components.AppSwitch
import com.jetpackduba.gitnuro.ui.components.PrimaryButton
import com.jetpackduba.gitnuro.ui.components.ScrollableColumn
import com.jetpackduba.gitnuro.ui.context_menu.ContextMenuElement
import com.jetpackduba.gitnuro.ui.context_menu.DropDownMenu
import com.jetpackduba.gitnuro.ui.dialogs.MaterialDialog
import com.jetpackduba.gitnuro.ui.dialogs.errors.ErrorDialog
import com.jetpackduba.gitnuro.ui.dropdowns.DropDownOption
import com.jetpackduba.gitnuro.viewmodels.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import java.time.Instant

sealed interface SettingsEntry {
    data class Section(val name: String) : SettingsEntry

    data class Entry(
        val icon: DrawableResource,
        val name: String,
        val content: @Composable (SettingsViewModel) -> Unit,
    ) :
        SettingsEntry
}

val settings = listOf(
    SettingsEntry.Section("User interface"),
    SettingsEntry.Entry(Res.drawable.palette, "Appearance") { Appearance(it) },
    SettingsEntry.Entry(Res.drawable.layout, "Layout") { Layout(it) },
    SettingsEntry.Entry(Res.drawable.schedule, "Date/Time") { DateTime(it) },
    SettingsEntry.Section("GIT"),
    SettingsEntry.Entry(Res.drawable.branch, "Branches") { Branches(it) },
    SettingsEntry.Entry(Res.drawable.cloud, "Remote actions") { RemoteActions(it) },

    SettingsEntry.Section("Network"),
    SettingsEntry.Entry(Res.drawable.network, "Proxy") { Proxy(it) },
    SettingsEntry.Entry(Res.drawable.password, "Authentication") { Authentication(it) },
    SettingsEntry.Entry(Res.drawable.security, "Security") { Security(it) },

    SettingsEntry.Section("Tools"),
    SettingsEntry.Entry(Res.drawable.terminal, "Terminal") { Terminal(it) },
    SettingsEntry.Entry(Res.drawable.info, "Logs") { Logs(it) },
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
    onDismiss: () -> Unit,
) {
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
                                    Section(settingEntry.name)
                                }

                                is SettingsEntry.Entry -> Entry(
                                    icon = settingEntry.icon,
                                    name = settingEntry.name,
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