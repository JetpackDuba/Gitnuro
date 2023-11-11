@file:OptIn(ExperimentalComposeUiApi::class)

package com.jetpackduba.gitnuro.ui.components

import androidx.compose.foundation.*
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
import com.jetpackduba.gitnuro.extensions.backgroundIf
import com.jetpackduba.gitnuro.extensions.onDoubleClick
import com.jetpackduba.gitnuro.theme.backgroundSelected


const val ENTRY_HEIGHT = 36

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SideMenuSubentry(
    text: String,
    iconResourcePath: String,
    isSelected: Boolean,
    extraPadding: Dp = 0.dp,
    onClick: () -> Unit,
    onDoubleClick: (() -> Unit)? = null,
    additionalInfo: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .height(ENTRY_HEIGHT.dp)
            .fillMaxWidth()
            .clickable { onClick() }
            .run {
                if (onDoubleClick != null)
                    this.onDoubleClick(onDoubleClick)
                else
                    this
            }
            .padding(start = extraPadding)
            .backgroundIf(isSelected, MaterialTheme.colors.backgroundSelected),
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