package com.jetpackduba.gitnuro.ui.dialogs.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.managers.Error
import com.jetpackduba.gitnuro.preferences.DEFAULT_UI_SCALE
import com.jetpackduba.gitnuro.theme.*
import com.jetpackduba.gitnuro.ui.components.*
import com.jetpackduba.gitnuro.ui.dialogs.ErrorDialog
import com.jetpackduba.gitnuro.ui.dialogs.MaterialDialog
import com.jetpackduba.gitnuro.ui.dropdowns.DropDownOption
import com.jetpackduba.gitnuro.viewmodels.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface SettingsEntry {
    data class Section(val name: String) : SettingsEntry

    data class Entry(val icon: String, val name: String, val content: @Composable (SettingsViewModel) -> Unit) :
        SettingsEntry
}

val settings = listOf(
    SettingsEntry.Section("User interface"),
    SettingsEntry.Entry(AppIcons.PALETTE, "Appearance") { Appearance(it) },
    SettingsEntry.Entry(AppIcons.LAYOUT, "Layout") { Layout(it) },

    SettingsEntry.Section("GIT"),
    SettingsEntry.Entry(AppIcons.LIST, "Commits history") { CommitsHistory(it) },
    SettingsEntry.Entry(AppIcons.BRANCH, "Branches") { Branches(it) },
    SettingsEntry.Entry(AppIcons.CLOUD, "Remote actions") { RemoteActions(it) },

    SettingsEntry.Section("Network"),
    SettingsEntry.Entry(AppIcons.NETWORK, "Proxy") { Proxy(it) },
    SettingsEntry.Entry(AppIcons.PASSWORD, "Authentication") { Authentication(it) },
    SettingsEntry.Entry(AppIcons.SECURITY, "Security") { Security(it) },

    SettingsEntry.Section("Tools"),
    SettingsEntry.Entry(AppIcons.TERMINAL, "Terminal") { Terminal(it) },
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

    var currentProxyType by remember {
        mutableStateOf(proxyTypesDropDownOptions.first { it.value == settingsViewModel.proxyType })
    }

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
            currentOption = currentProxyType,
            onOptionSelected = {
                currentProxyType = it
                settingsViewModel.proxyType = it.value
            }
        )

        SettingTextInput(
            title = "Host name",
            subtitle = "",
            value = hostName,
            onValueChanged = {
                hostName = it
                settingsViewModel.proxyHostName = it
            },
            enabled = useProxy,
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
            onValueChanged = {
                user = it
                settingsViewModel.proxyHostUser = it
            },
            enabled = useProxy && useAuth,
        )


        SettingTextInput(
            title = "Password",
            subtitle = "",
            value = password,
            onValueChanged = {
                password = it
                settingsViewModel.proxyHostPassword = it
            },
            isPassword = true,
            enabled = useProxy && useAuth,
        )

    }
}

@Composable
fun SettingsDialog(
    settingsViewModel: SettingsViewModel = gitnuroViewModel(),
    onDismiss: () -> Unit,
) {

    LaunchedEffect(Unit) {
        settingsViewModel.resetInfo()
    }

    var selectedCategory by remember {
        mutableStateOf<SettingsEntry.Entry>(
            settings.filterIsInstance(SettingsEntry.Entry::class.java).first()
        )
    }

    MaterialDialog(
        background = MaterialTheme.colors.surface,
        onCloseRequested = {
            settingsViewModel.savePendingChanges()

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
                    ScrollableLazyColumn(
                        modifier = Modifier
                    ) {
                        itemsIndexed(settings) { index, settingEntry ->
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
                        settingsViewModel.savePendingChanges()
                        onDismiss()
                    },
                )
            }

        }
    }
}

@Composable
private fun Entry(icon: String, name: String, isSelected: Boolean, onClick: () -> Unit) {
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
        fontWeight = FontWeight.Bold,
    )
}


@Composable
private fun CommitsHistory(settingsViewModel: SettingsViewModel) {
    val commitsLimitEnabled by settingsViewModel.commitsLimitEnabledFlow.collectAsState()
    var commitsLimit by remember { mutableStateOf(settingsViewModel.commitsLimit) }

    SettingToggle(
        title = "Limit log commits",
        subtitle = "Turning off this may affect the performance",
        value = commitsLimitEnabled,
        onValueChanged = { value ->
            settingsViewModel.commitsLimitEnabled = value
        }
    )

    SettingIntInput(
        title = "Max commits",
        subtitle = "Increasing this value may affect the performance",
        value = commitsLimit,
        enabled = commitsLimitEnabled,
        onValueChanged = { value ->
            commitsLimit = value
            settingsViewModel.commitsLimit = value
        }
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
        }
    )
}


@Composable
private fun Branches(settingsViewModel: SettingsViewModel) {
    val ffMerge by settingsViewModel.ffMergeFlow.collectAsState()

    SettingToggle(
        title = "Fast-forward merge",
        subtitle = "Try to fast-forward merges when possible",
        value = ffMerge,
        onValueChanged = { value ->
            settingsViewModel.ffMerge = value
        }
    )
}

@Composable
private fun Layout(settingsViewModel: SettingsViewModel) {
    val swapUncommitedChanges by settingsViewModel.swapUncommittedChangesFlow.collectAsState()

    SettingToggle(
        title = "Swap position for staged/unstaged views",
        subtitle = "Show the list of unstaged changes above the list of staged changes",
        value = swapUncommitedChanges,
        onValueChanged = { value ->
            settingsViewModel.swapUncommitedChanges = value
        }
    )
}

@Composable
private fun Appearance(settingsViewModel: SettingsViewModel) {
    val currentTheme by settingsViewModel.themeState.collectAsState()
    val (errorToDisplay, setErrorToDisplay) = remember { mutableStateOf<Error?>(null) }

    SettingDropDown(
        title = "Theme",
        subtitle = "Select the UI theme between light and dark mode",
        dropDownOptions = themeLists,
        currentOption = DropDownOption(currentTheme, currentTheme.displayName),
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

    val density = LocalDensity.current.density
    var options by remember {
        mutableStateOf(
            listOf(
                DropDownOption(1f, "100%"),
                DropDownOption(1.25f, "125%"),
                DropDownOption(1.5f, "150%"),
                DropDownOption(1.75f, "175%"),
                DropDownOption(2f, "200%"),
                DropDownOption(2.5f, "250%"),
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
        currentOption = scaleValue,
        onOptionSelected = { newValue ->
            scaleValue = newValue
            settingsViewModel.scaleUi = newValue.value
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
    currentOption: DropDownOption<T>,
) {
    var showThemeDropdown by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FieldTitles(title, subtitle)

        Spacer(modifier = Modifier.weight(1f))

        Box {
            OutlinedButton(
                onClick = { showThemeDropdown = true },
                colors = ButtonDefaults.outlinedButtonColors(backgroundColor = MaterialTheme.colors.background),
                modifier = Modifier.width(180.dp)
            ) {
                Text(
                    text = currentOption.optionName,
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )

                Icon(
                    painter = painterResource(AppIcons.DROPDOWN),
                    contentDescription = null,
                    tint = MaterialTheme.colors.onBackground,
                )
            }

            DropdownMenu(
                expanded = showThemeDropdown,
                onDismissRequest = { showThemeDropdown = false },
            ) {
                for (dropDownOption in dropDownOptions) {
                    DropdownMenuItem(
                        onClick = {
                            showThemeDropdown = false
                            onOptionSelected(dropDownOption)
                        }
                    ) {
                        Text(dropDownOption.optionName)
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
    value: Boolean,
    onValueChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FieldTitles(title, subtitle)

        Spacer(modifier = Modifier.weight(1f))

        AppSwitch(
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
        FieldTitles(title, subtitle)

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
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FieldTitles(title, subtitle)

        Spacer(modifier = Modifier.weight(1f))

        var text by remember {
            mutableStateOf(value)
        }

        AdjustableOutlinedTextField(
            value = text,
            modifier = Modifier.width(240.dp),
            isError = false,
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
) {
    Column(
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Medium,
        )

        Text(
            text = subtitle,
            color = MaterialTheme.colors.onBackground,
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