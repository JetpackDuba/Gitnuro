package com.jetpackduba.gitnuro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.extensions.handOnHover
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding

@Composable
fun SearchTextField(
    searchFilter: TextFieldValue,
    onSearchFilterChanged: (TextFieldValue) -> Unit,
    searchFocusRequester: FocusRequester,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.matchesBinding(KeybindingOption.EXIT) && keyEvent.type == KeyEventType.KeyDown) {
                    onClose()
                    true
                } else
                    false
            },
    ) {
        AdjustableOutlinedTextField(
            value = searchFilter,
            onValueChange = {
                onSearchFilterChanged(it)
            },
            hint = "Search files by name or path",
            modifier = Modifier.fillMaxWidth()
                .focusable()
                .focusRequester(searchFocusRequester),
            trailingIcon = {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(16.dp)
                        .handOnHover(),
                ) {
                    Icon(
                        painterResource(AppIcons.CLOSE),
                        contentDescription = null,
                        tint = MaterialTheme.colors.onBackground
                    )
                }
            }
        )
    }
}