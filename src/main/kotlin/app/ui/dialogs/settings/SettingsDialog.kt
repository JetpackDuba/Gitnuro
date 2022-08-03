package app.ui.dialogs.settings

import androidx.compose.foundation.background
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
import app.DropDownOption
import app.theme.*
import app.ui.components.AdjustableOutlinedTextField
import app.ui.components.ScrollableColumn
import app.ui.dialogs.MaterialDialog
import app.ui.openFileDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import app.extensions.handMouseClickable
import app.preferences.DEFAULT_UI_SCALE
import app.viewmodels.SettingsViewModel

enum class SettingsCategory(val displayName: String) {
    UI("UI"),
    GIT("Git"),
}


@Composable
fun SettingsDialog(
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit,
) {

    LaunchedEffect(Unit) {
        settingsViewModel.resetInfo()
    }

    val categories = remember {
        listOf(
            SettingsCategory.UI,
            SettingsCategory.GIT,
        )
    }

    var selectedCategory by remember { mutableStateOf(SettingsCategory.UI) }

    MaterialDialog(
        onCloseRequested = {
            settingsViewModel.savePendingChanges()

            onDismiss()
        }
    ) {
        Column(modifier = Modifier.height(720.dp)) {
            Text(
                text = "Settings",
                color = MaterialTheme.colors.primaryTextColor,
                fontSize = 20.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp, start = 8.dp)
            )

            Row(modifier = Modifier.weight(1f)) {
                ScrollableColumn(
                    modifier = Modifier
                        .width(200.dp)
                        .fillMaxHeight()
                ) {
                    categories.forEach { category ->
                        Category(
                            category = category,
                            isSelected = category == selectedCategory,
                            onClick = { selectedCategory = category }
                        )
                    }
                }


                Column(modifier = Modifier.width(720.dp)) {
                    when (selectedCategory) {
                        SettingsCategory.UI -> UiSettings(settingsViewModel)
                        SettingsCategory.GIT -> GitSettings(settingsViewModel)
                    }
                }
            }

            TextButton(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .align(Alignment.End),
                colors = textButtonColors(),
                onClick = {
                    settingsViewModel.savePendingChanges()

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

@Composable
fun GitSettings(settingsViewModel: SettingsViewModel) {
    val commitsLimitEnabled by settingsViewModel.commitsLimitEnabledFlow.collectAsState()
    val ffMerge by settingsViewModel.ffMergeFlow.collectAsState()
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
fun UiSettings(settingsViewModel: SettingsViewModel) {
    val currentTheme by settingsViewModel.themeState.collectAsState()

    SettingDropDown(
        title = "Theme",
        subtitle = "Select the UI theme between light and dark mode",
        dropDownOptions = themeLists,
        currentOption = currentTheme,
        onOptionSelected = { theme ->
            settingsViewModel.theme = theme
        }
    )

    if (currentTheme == Theme.CUSTOM) {
        SettingButton(
            title = "Custom theme",
            subtitle = "Select a JSON file to load the custom theme",
            buttonText = "Open file",
            onClick = {
                val filePath = openFileDialog()

                if (filePath != null) {
                    settingsViewModel.saveCustomTheme(filePath)
                }
            }
        )
    }

    val density = LocalDensity.current.density
    var scaleValue by remember {
        val savedScaleUi = settingsViewModel.scaleUi
        val scaleUi = if (savedScaleUi == DEFAULT_UI_SCALE) {
            density
        } else {
            savedScaleUi
        } * 100

        mutableStateOf(scaleUi)
    }

    SettingSlider(
        title = "Scale",
        subtitle = "Adapt the size the UI to your preferred scale",
        value = scaleValue,
        onValueChanged = { newValue ->
            scaleValue = newValue
        },
        onValueChangeFinished = {
            settingsViewModel.scaleUi = scaleValue / 100
        },
        steps = 5,
        minValue = 100f,
        maxValue = 300f,
    )
}

@Composable
fun Category(
    category: SettingsCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor = if (isSelected)
        MaterialTheme.colors.backgroundSelected
    else
        MaterialTheme.colors.background

    Text(
        text = category.displayName,
        modifier = Modifier
            .fillMaxWidth()
            .background(color = backgroundColor)
            .handMouseClickable(onClick)
            .padding(8.dp),
        style = MaterialTheme.typography.body1,
    )
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
fun SettingToggle(
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
fun SettingSlider(
    title: String,
    subtitle: String,
    value: Float,
    minValue: Float,
    maxValue: Float,
    steps: Int,
    onValueChanged: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FieldTitles(title, subtitle)

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "$minValue%",
            style = MaterialTheme.typography.caption,
        )

        Slider(
            value = value,
            onValueChange = onValueChanged,
            onValueChangeFinished = onValueChangeFinished,
            steps = steps,
            valueRange = minValue..maxValue,
            modifier = Modifier.width(200.dp)
        )

        Text(
            text = "$maxValue%",
            style = MaterialTheme.typography.caption,
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

private fun isValidFloat(value: String): Boolean {
    return try {
        value.toFloat()
        true
    } catch (ex: Exception) {
        false
    }
}