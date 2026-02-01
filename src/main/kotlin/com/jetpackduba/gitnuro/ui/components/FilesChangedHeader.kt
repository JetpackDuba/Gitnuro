package com.jetpackduba.gitnuro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.extensions.handOnHover
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.list
import com.jetpackduba.gitnuro.generated.resources.search
import com.jetpackduba.gitnuro.generated.resources.tree
import com.jetpackduba.gitnuro.theme.tertiarySurface
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Stable
@Immutable
data class ActionInfo(
    val applyToOneTitle: String,
    val applyToAllTitle: String,
    val icon: DrawableResource,
    val color: Color,
    val textColor: Color,
)

@Composable
fun FilesChangedHeader(
    title: String,
    showAsTree: Boolean,
    showSearch: Boolean,
    actionInfo: ActionInfo? = null,
    onAllAction: (() -> Unit)? = null,
    onAlternateShowAsTree: () -> Unit,
    onSearchFilterToggled: (Boolean) -> Unit,
    onSearchFocused: () -> Unit,
    searchFilter: TextFieldValue,
    onSearchFilterChanged: (TextFieldValue) -> Unit,
) {
    val searchFocusRequester = remember { FocusRequester() }

    /**
     * State used to prevent the text field from getting the focus when returning from another tab
     */
    var requestFocus by remember { mutableStateOf(false) }

    val headerHoverInteraction = remember { MutableInteractionSource() }
    val isHeaderHovered by headerHoverInteraction.collectIsHoveredAsState()
    Column {
        Row(
            modifier = Modifier
                .height(34.dp)
                .fillMaxWidth()
                .background(color = MaterialTheme.colors.tertiarySurface)
                .hoverable(headerHoverInteraction),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .padding(start = 16.dp, end = 8.dp)
                    .weight(1f),
                text = title,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Left,
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body2,
                maxLines = 1,
            )

            IconButton(
                onClick = {
                    onAlternateShowAsTree()
                },
                modifier = Modifier.handOnHover()
            ) {
                Icon(
                    painter = painterResource(if (showAsTree) Res.drawable.list else Res.drawable.tree),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colors.onBackground,
                )
            }

            IconButton(
                onClick = {
                    onSearchFilterToggled(!showSearch)

                    if (!showSearch)
                        requestFocus = true
                },
                modifier = Modifier.handOnHover()
            ) {
                Icon(
                    painter = painterResource(Res.drawable.search),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colors.onBackground,
                )
            }

            if (actionInfo != null && onAllAction != null) {
                SecondaryButtonCompactable(
                    text = actionInfo.applyToAllTitle,
                    icon = actionInfo.icon,
                    isParentHovered = isHeaderHovered,
                    backgroundButton = actionInfo.color,
                    onBackgroundColor = actionInfo.textColor,
                    onClick = onAllAction,
                    modifier = Modifier.padding(start = 4.dp, end = 16.dp),
                )
            }
        }

        if (showSearch) {
            SearchTextField(
                searchFilter = searchFilter,
                onSearchFilterChanged = onSearchFilterChanged,
                searchFocusRequester = searchFocusRequester,
                onSearchFocused = onSearchFocused,
                onClose = { onSearchFilterToggled(false) },
            )
        }

        LaunchedEffect(showSearch, requestFocus) {
            if (showSearch && requestFocus) {
                searchFocusRequester.requestFocus()
                requestFocus = false
            }
        }
    }
}