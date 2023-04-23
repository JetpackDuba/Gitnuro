package com.jetpackduba.gitnuro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun SearchTextField(
    searchFilter: TextFieldValue,
    onSearchFilterChanged: (TextFieldValue) -> Unit,
    searchFocusRequester: FocusRequester,
) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        AdjustableOutlinedTextField(
            value = searchFilter,
            onValueChange = {
                onSearchFilterChanged(it)
            },
            hint = "Search files by name or path",
            modifier = Modifier.fillMaxWidth()
                .focusable()
                .focusRequester(searchFocusRequester)
        )
    }
}