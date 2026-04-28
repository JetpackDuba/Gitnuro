package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.interfaces.ICheckoutRefGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import javax.inject.Inject

class CheckoutBranchUseCase @Inject constructor(
    private val checkoutRefGitAction: ICheckoutRefGitAction,
    private val tabState: TabInstanceRepository,
) {
    fun invoke(ref: Branch) {
        tabState.safeProcessing(
            refreshType = RefreshType.ALL_DATA,
            title = "Branch checkout",
            subtitle = "Checking out branch ${ref.simpleName}",
            taskType = TaskType.CheckoutBranch,
        ) { git ->
            checkoutRefGitAction(git, ref)

            positiveNotification("\"${ref.simpleName}\" checked out")
        }
    }
}