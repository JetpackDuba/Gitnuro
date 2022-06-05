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
import app.AppPreferences
import app.DropDownOption
import app.theme.outlinedTextFieldColors
import app.theme.primaryTextColor
import app.theme.textButtonColors
import app.theme.themesList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsDialog(
    appPreferences: AppPreferences,
    onDismiss: () -> Unit,
) {
    val currentTheme by appPreferences.themeState.collectAsState()
    val commitsLimitEnabled by appPreferences.commitsLimitEnabledFlow.collectAsState()
    var commitsLimit by remember { mutableStateOf(appPreferences.commitsLimit) }

    MaterialDialog {
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
                Text("Close")
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
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = title,
                color = MaterialTheme.colors.primaryTextColor,
                fontSize = 16.sp,
            )

            Text(
                text = subtitle,
                color = MaterialTheme.colors.primaryTextColor,
                modifier = Modifier.padding(top = 4.dp),
                fontSize = 12.sp,
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        Box {
            OutlinedButton(onClick = { showThemeDropdown = true }) {
                Text(
                    text = currentOption.optionName,
                    color = MaterialTheme.colors.primaryTextColor,
                    fontSize = 14.sp,
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
        Column(
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = MaterialTheme.colors.primaryTextColor,
                fontSize = 16.sp,
            )

            Text(
                text = subtitle,
                color = MaterialTheme.colors.primaryTextColor,
                modifier = Modifier.padding(top = 4.dp),
                fontSize = 12.sp,
            )
        }

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
        Column(
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = MaterialTheme.colors.primaryTextColor,
                fontSize = 16.sp,
            )

            Text(
                text = subtitle,
                color = MaterialTheme.colors.primaryTextColor,
                modifier = Modifier.padding(top = 4.dp),
                fontSize = 12.sp,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        var text by remember {
            mutableStateOf(value.toString())
        }

        var isError by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        OutlinedTextField(
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

private fun isValidInt(value: String): Boolean {
    return try {
        value.toInt()
        true
    } catch (ex: Exception) {
        false
    }
}