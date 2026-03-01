package com.jetpackduba.gitnuro.git.actions

import com.jetpackduba.gitnuro.git.Action
import com.jetpackduba.gitnuro.TaskType
import com.jetpackduba.gitnuro.extensions.simpleName
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.branches.CheckoutRefUseCase
import com.jetpackduba.gitnuro.git.branches.MergeBranchUseCase
import com.jetpackduba.gitnuro.git.rebase.RebaseBranchUseCase
import com.jetpackduba.gitnuro.models.positiveNotification
import com.jetpackduba.gitnuro.models.warningNotification
import com.jetpackduba.gitnuro.repositories.AppSettingsRepository
import javax.inject.Inject

class RefCheckoutAction @Inject constructor(
    private val checkoutRefUseCase: CheckoutRefUseCase,
    private val tabState: TabState,
) : IAction<Action.RefCheckout> {

    override fun invoke(action: Action.RefCheckout) {
        val ref = action.ref

        tabState.safeProcessing(
            refreshType = RefreshType.ALL_DATA,
            title = "Branch checkout",
            subtitle = "Checking out branch ${ref.simpleName}",
            taskType = TaskType.CHECKOUT_BRANCH,
        ) { git ->
            checkoutRefUseCase(git, ref)

            positiveNotification("\"${ref.simpleName}\" checked out")
        }
    }
}