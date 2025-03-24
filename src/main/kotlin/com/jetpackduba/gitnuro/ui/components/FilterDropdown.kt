package com.jetpackduba.gitnuro.ui.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.extensions.lowercaseContains
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.dropdown
import com.jetpackduba.gitnuro.ui.dropdowns.DropDownOption
import org.jetbrains.compose.resources.painterResource

@Preview
@Composable
fun FilterDropdownPreview() {
    val items = listOf(
        DropDownOption("", "Test1"),
        DropDownOption("", "Test2"),
        DropDownOption("", "Test3"),
        DropDownOption("", "Test4"),
    )

    FilterDropdown(
        dropdownItems = items,
        currentOption = items[0],
        onOptionSelected = {}
    )
}

@Composable
fun <T> FilterDropdown(
    dropdownItems: List<DropDownOption<T>>,
    currentOption: DropDownOption<T>?,
    width: Dp = 240.dp,
    onOptionSelected: (DropDownOption<T>) -> Unit,
) {
    var showDropdown by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("") }
    val filterFocusRequester = remember { FocusRequester() }
    val filteredDropdownItems =
        remember(filter, dropdownItems) { dropdownItems.filter { it.optionName.lowercaseContains(filter) } }

    Box {
        OutlinedButton(
            onClick = { showDropdown = true },
            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = MaterialTheme.colors.background),
            modifier = Modifier.width(width)
        ) {
            Text(
                text = currentOption?.optionName ?: "",
                style = MaterialTheme.typography.body1,
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

        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false },
            modifier = Modifier.width(width),
        ) {
            DropdownMenuItem(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                AdjustableOutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    modifier = Modifier.focusable(showDropdown)
                        .focusRequester(filterFocusRequester)
                )

                LaunchedEffect(showDropdown) {
                    if (showDropdown) {
                        filterFocusRequester.requestFocus()
                    }
                }
            }

            for (dropDownOption in filteredDropdownItems) {
                DropdownMenuItem(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onOptionSelected(dropDownOption)
                        showDropdown = false
                    }
                ) {
                    Text(dropDownOption.optionName, style = MaterialTheme.typography.body2)
                }
            }
        }
    }
}