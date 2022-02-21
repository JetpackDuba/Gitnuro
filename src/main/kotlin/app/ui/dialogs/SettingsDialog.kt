package app.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.AppPreferences
import app.DropDownOption
import app.theme.Themes
import app.theme.primaryTextColor
import app.theme.themesList

@Composable
fun SettingsDialog(
    appPreferences: AppPreferences,
    onDismiss: () -> Unit,
) {
    val currentTheme by appPreferences.themeState.collectAsState()

    MaterialDialog {
        Column(modifier = Modifier.width(500.dp)) {
            Text(
                text = "Settings",
                color = MaterialTheme.colors.primaryTextColor,
            )

            SettingDropDown(
                title = "Theme",
                dropDownOptions = themesList,
                currentOption = currentTheme,
                onOptionSelected = { theme ->
                    appPreferences.theme = theme
                }
            )

            TextButton(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .align(Alignment.End),
                onClick = onDismiss
            ) {
                Text("Close")
            }
        }
    }
}

@Composable
fun <T: DropDownOption> SettingDropDown(
    title: String,
    dropDownOptions: List<T>,
    onOptionSelected: (T) -> Unit,
    currentOption: T,
) {
    var showThemeDropdown by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = MaterialTheme.colors.primaryTextColor,
        )
        Spacer(modifier = Modifier.weight(1f))
        Box {
            OutlinedButton(onClick = { showThemeDropdown = true }) {
                Text(
                    currentOption.optionName,
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
fun <T: DropDownOption> SettingTextInput(
    title: String,
    dropDownOptions: List<T>,
    onOptionSelected: (T) -> Unit,
    currentOption: T,
) {
    var showThemeDropdown by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = MaterialTheme.colors.primaryTextColor,
        )
        Spacer(modifier = Modifier.width(300.dp))
        Box {
            OutlinedButton(onClick = { showThemeDropdown = true }) {
                Text(
                    currentOption.optionName,
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