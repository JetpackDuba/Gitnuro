package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.app.generated.resources.Res
import com.jetpackduba.gitnuro.app.generated.resources.delete
import com.jetpackduba.gitnuro.app.generated.resources.delete_branch_confirmation_dialog_action
import com.jetpackduba.gitnuro.app.generated.resources.delete_branch_confirmation_dialog_subtitle
import com.jetpackduba.gitnuro.app.generated.resources.delete_branch_confirmation_dialog_title
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.ui.dialogs.base.IconBasedDialog
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Preview
@Composable
fun DeleteBranchConfirmationDialogPreview() {
    DeleteBranchConfirmationDialogView(
        branches = listOf(Branch("", "example", isLocal = true, trackingBranch = null)),
        onDelete = {},
        onDismiss = {},
    )
}

@Composable
fun DeleteBranchConfirmationDialog(
    viewModel: DeleteBranchConfirmationViewModel,
    onDismiss: () -> Unit,
) {
    DeleteBranchConfirmationDialogView(
        branches = listOf(viewModel.targetBranch),
        onDismiss = onDismiss,
        onDelete = {
            viewModel.deleteBranch()
            onDismiss()
        }
    )
}


@Composable
private fun DeleteBranchConfirmationDialogView(
    branches: List<Branch>,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    IconBasedDialog(
        icon = painterResource(Res.drawable.delete),
        title = stringResource(Res.string.delete_branch_confirmation_dialog_title),
        subtitle = stringResource(Res.string.delete_branch_confirmation_dialog_subtitle),
        primaryActionText = stringResource(Res.string.delete_branch_confirmation_dialog_action),
        primaryActionBackgroundColor = MaterialTheme.colors.error,
        primaryActionTextColor = MaterialTheme.colors.onError,
        onDismiss = onDismiss,
        onPrimaryActionClicked = onDelete,
        beforeActionsFocusRequester = null,
        actionsFocusRequester = null,
        afterActionsFocusRequester = null,
        content = {
            LazyColumn(
                modifier = Modifier
                    .size(width = 320.dp, height = 144.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colors.background),
            ) {
                items (branches){ branch ->
                    Text(
                        branch.simpleNameWithRemote,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                            .padding(8.dp),
                    )
                }
            }
        }
    )
}