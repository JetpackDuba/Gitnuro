package com.jetpackduba.gitnuro.ui.dialogs.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.preferences.DEFAULT_UI_SCALE
import com.jetpackduba.gitnuro.theme.*
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.components.PrimaryButton
import com.jetpackduba.gitnuro.ui.components.ScrollableColumn
import com.jetpackduba.gitnuro.ui.components.gitnuroViewModel
import com.jetpackduba.gitnuro.ui.dialogs.MaterialDialog
import com.jetpackduba.gitnuro.ui.dropdowns.DropDownOption
import com.jetpackduba.gitnuro.ui.dropdowns.ScaleDropDown
import com.jetpackduba.gitnuro.ui.openFileDialog
import com.jetpackduba.gitnuro.viewmodels.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SettingsCategory(val displayName: String) {
    UI("UI"),
    GIT("Git"),
}


@Composable
fun SettingsDialog(
    settingsViewModel: SettingsViewModel = gitnuroViewModel(),
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
        background = MaterialTheme.colors.surface,
        onCloseRequested = {
            settingsViewModel.savePendingChanges()

            onDismiss()
        }
    ) {
        Column(modifier = Modifier.height(720.dp)) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.h3,
                color = MaterialTheme.colors.onBackground,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )

            Row(modifier = Modifier.weight(1f)) {
                ScrollableColumn(
                    modifier = Modifier
                        .width(200.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colors.background)
                ) {
                    categories.forEach { category ->
                        Category(
                            category = category,
                            isSelected = category == selectedCategory,
                            onClick = { selectedCategory = category }
                        )
                    }
                }


                Column(
                    modifier = Modifier
                        .width(720.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    when (selectedCategory) {
                        SettingsCategory.UI -> UiSettings(settingsViewModel)
                        SettingsCategory.GIT -> GitSettings(settingsViewModel)
                    }
                }
            }

            PrimaryButton(
                text = "Accept",
                modifier = Modifier
                    .padding(end = 8.dp, bottom = 8.dp)
                    .align(Alignment.End),
                onClick = {
                    settingsViewModel.savePendingChanges()
                    onDismiss()
                },
            )
        }
    }
}

@Composable
fun GitSettings(settingsViewModel: SettingsViewModel) {
    val commitsLimitEnabled by settingsViewModel.commitsLimitEnabledFlow.collectAsState()
    val ffMerge by settingsViewModel.ffMergeFlow.collectAsState()
    val pullRebase by settingsViewModel.pullRebaseFlow.collectAsState()
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

    SettingToggle(
        title = "Pull with rebase as default",
        subtitle = "Rebase changes instead of merging when pulling",
        value = pullRebase,
        onValueChanged = { value ->
            settingsViewModel.pullRebase = value
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
    var options by remember {
        mutableStateOf(
            listOf(
                ScaleDropDown(1f, "100%"),
                ScaleDropDown(1.25f, "125%"),
                ScaleDropDown(1.5f, "150%"),
                ScaleDropDown(2f, "200%"),
                ScaleDropDown(2.5f, "250%"),
                ScaleDropDown(3f, "300%"),
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
            matchingOption = ScaleDropDown(scaleUi, "${(scaleUi * 100).toInt()}%")
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
        color = MaterialTheme.colors.onBackground,
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

        Switch(
            checked = value,
            onCheckedChange = onValueChanged,
            colors = SwitchDefaults.colors(uncheckedThumbColor = MaterialTheme.colors.secondary)
        )
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
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FieldTitles(title, subtitle)

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "${minValue.toInt()}%",
            style = MaterialTheme.typography.caption,
        )

        Slider(
            value = value,
            onValueChange = onValueChanged,
            onValueChangeFinished = onValueChangeFinished,
            steps = steps,
            valueRange = minValue..maxValue,
            modifier = Modifier
                .width(200.dp)
                .padding(horizontal = 4.dp)
        )

        Text(
            text = "${maxValue.toInt()}%",
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
        )

        Text(
            text = subtitle,
            color = MaterialTheme.colors.onBackgroundSecondary,
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