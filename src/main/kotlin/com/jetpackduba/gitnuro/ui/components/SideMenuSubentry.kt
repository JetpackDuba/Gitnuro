@file:OptIn(ExperimentalComposeUiApi::class)

package com.jetpackduba.gitnuro.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


const val ENTRY_HEIGHT = 36

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SideMenuSubentry(
    text: String,
    iconResourcePath: String,
    extraPadding: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    additionalInfo: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .height(ENTRY_HEIGHT.dp)
            .fillMaxWidth()
            .run {
                if (onClick != null)
                    combinedClickable(onClick = onClick, onDoubleClick = onDoubleClick)
                else
                    this
            }
            .padding(start = extraPadding),
//            .background(background),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconResourcePath),
            contentDescription = null,
            modifier = Modifier
                .padding(start = 32.dp, end = 8.dp)
                .size(16.dp),
            tint = MaterialTheme.colors.primaryVariant,
        )

        Text(
            text = text,
            modifier = Modifier.weight(1f, fill = true),
            maxLines = 1,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onBackground,
            softWrap = false,
        )

        additionalInfo()
    }
}