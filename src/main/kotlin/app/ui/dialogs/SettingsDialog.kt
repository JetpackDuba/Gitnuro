package app.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.preferences.AppPreferences
import app.DropDownOption
import app.theme.*
import app.ui.components.AdjustableOutlinedTextField
import app.ui.openFileDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsDialog(
    appPreferences: AppPreferences,
    onDismiss: () -> Unit,
) {
    val currentTheme by appPreferences.themeState.collectAsState()
    val commitsLimitEnabled by appPreferences.commitsLimitEnabledFlow.collectAsState()
    val ffMerge by appPreferences.ffMergeFlow.collectAsState()
    var commitsLimit by remember { mutableStateOf(appPreferences.commitsLimit) }

    MaterialDialog(
        onCloseRequested = {
            savePendingSettings(
                appPreferences = appPreferences,
                commitsLimit = commitsLimit,
            )

            onDismiss()
        }
    ) {
        Column(modifier = Modifier.width(720.dp)) {
            Text(
                text = "Settings",
                color = MaterialTheme.colors.primaryTextColor,
                fontSize = 20.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp, start = 8.dp)
            )

            SettingDropDown(
                title = "Theme",
                subtitle = "Select the UI theme between light and dark mode",
                dropDownOptions = themesList,
                currentOption = currentTheme,
                onOptionSelected = { theme ->
                    appPreferences.theme = theme
                }
            )

            if (currentTheme == Themes.CUSTOM) {
                SettingButton(
                    title = "Custom theme",
                    subtitle = "Select a JSON file to load the custom theme",
                    buttonText = "Open file",
                    onClick = {
                        val filePath = openFileDialog()

                        if (filePath != null) {
                            appPreferences.saveCustomTheme(filePath)
                        }
                    }
                )
            }

            SettingToogle(
                title = "Limit log commits",
                subtitle = "Turning off this may affect the performance",
                value = commitsLimitEnabled,
                onValueChanged = { value ->
                    appPreferences.commitsLimitEnabled = value
                }
            )

            SettingIntInput(
                title = "Max commits",
                subtitle = "Increasing this value may affect the performance",
                value = commitsLimit,
                enabled = commitsLimitEnabled,
                onValueChanged = { value ->
                    commitsLimit = value
                }
            )

            SettingToogle(
                title = "Fast-forward merge",
                subtitle = "Try to fast-forward merges when possible",
                value = ffMerge,
                onValueChanged = { value ->
                    appPreferences.ffMerge = value
                }
            )

            TextButton(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .align(Alignment.End),
                colors = textButtonColors(),
                onClick = {
                    savePendingSettings(
                        appPreferences = appPreferences,
                        commitsLimit = commitsLimit,
                    )

                    onDismiss()
                }
            ) {
                Text(
                    "Close",
                    style = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.primaryVariant)
                )
            }
        }
    }
}

fun savePendingSettings(
    appPreferences: AppPreferences,
    commitsLimit: Int,
) {
    if (appPreferences.commitsLimit != commitsLimit) {
        appPreferences.commitsLimit = commitsLimit
    }
}

@Composable
fun <T : DropDownOption> SettingDropDown(
    title: String,
    subtitle: String,
    dropDownOptions: List<T>,
    onOptionSelected: (T) -> Unit,
    currentOption: T,
) {
    var showThemeDropdown by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FieldTitles(title, subtitle)

        Spacer(modifier = Modifier.weight(1f))

        Box {
            OutlinedButton(onClick = { showThemeDropdown = true }) {
                Text(
                    text = currentOption.optionName,
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.primaryTextColor,
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
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FieldTitles(title, subtitle)

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(onClick = onClick) {
            Text(
                text = buttonText,
                color = MaterialTheme.colors.primaryTextColor,
                style = MaterialTheme.typography.body1,
            )
        }
    }
}

@Composable
fun SettingToogle(
    title: String,
    subtitle: String,
    value: Boolean,
    onValueChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FieldTitles(title, subtitle)

        Spacer(modifier = Modifier.weight(1f))

        Switch(value, onCheckedChange = onValueChanged)
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
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
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
            maxLines = 1,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
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
            color = MaterialTheme.colors.primaryTextColor,
            style = MaterialTheme.typography.body1,
        )

        Text(
            text = subtitle,
            color = MaterialTheme.colors.secondaryTextColor,
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