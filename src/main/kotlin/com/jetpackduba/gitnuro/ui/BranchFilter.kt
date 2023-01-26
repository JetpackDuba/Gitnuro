package com.jetpackduba.gitnuro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.components.gitnuroViewModel
import com.jetpackduba.gitnuro.viewmodels.BranchFilterViewModel

@Composable
fun BranchFilter(
    branchFilterViewModel: BranchFilterViewModel = gitnuroViewModel()
) {
    val filterKeyword by branchFilterViewModel.keyword.collectAsState()

    Box(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        AdjustableOutlinedTextField(
            value = filterKeyword,
            onValueChange = { branchFilterViewModel.newBranchFilter(keyword = it) },
            hint = "Filter Branches",
            textStyle = MaterialTheme.typography.caption,
            leadingIconResourcePath = "branch_filter.svg",
            maxLines = 1,
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .heightIn(min = 8.dp)
                .defaultMinSize(minHeight = 8.dp),
        )
    }

}