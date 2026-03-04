package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.git.branches.CheckoutRefGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.simpleName
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class RefCheckoutUseCase @Inject constructor(
    private val checkoutRefGitAction: CheckoutRefGitAction,
    private val tabState: TabInstanceRepository,
) {

    fun invoke(ref: Ref) {

        tabState.safeProcessing(
            refreshType = RefreshType.ALL_DATA,
            title = "Branch checkout",
            subtitle = "Checking out branch ${ref.simpleName}",
            taskType = TaskType.CHECKOUT_BRANCH,
        ) { git ->
            checkoutRefGitAction(git, ref)

            positiveNotification("\"${ref.simpleName}\" checked out")
        }
    }
}