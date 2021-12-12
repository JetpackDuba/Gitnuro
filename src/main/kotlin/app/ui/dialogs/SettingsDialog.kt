@file:Suppress("UNUSED_PARAMETER")

package app.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.AppPreferences
import app.theme.primaryTextColor
import app.theme.themesList

@Composable
fun SettingsDialog(
    appPreferences: AppPreferences,
    onDismiss: () -> Unit,
) {
    var showThemeDropdown by remember { mutableStateOf(false) }
    val currentTheme by appPreferences.themeState.collectAsState()

    MaterialDialog {
        Column {
            Text(
                text = "Settings",
                color = MaterialTheme.colors.primaryTextColor,
            )

            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Theme",
                    color = MaterialTheme.colors.primaryTextColor,
                )
                Spacer(modifier = Modifier.width(300.dp))
                OutlinedButton(onClick = { showThemeDropdown = true }) {
                    Text(
                        currentTheme.displayName,
                        color = MaterialTheme.colors.primaryTextColor,
                    )

                    DropdownMenu(
                        expanded = showThemeDropdown,
                        onDismissRequest = { showThemeDropdown = false },
                    ) {
                        for (theme in themesList) {
                            DropdownMenuItem(
                                onClick = {
                                    appPreferences.theme = theme
                                    showThemeDropdown = false
                                }
                            ) { Text(theme.displayName) }
                        }
                    }
                }
            }

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