package com.jetpackduba.gitnuro.data.git.branches

import com.jetpackduba.gitnuro.domain.interfaces.ICheckoutRefGitAction
import com.jetpackduba.gitnuro.domain.isBranch
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.simpleName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class CheckoutRefGitAction @Inject constructor() : ICheckoutRefGitAction {
    override suspend operator fun invoke(git: Git, ref: Branch): Unit = withContext(Dispatchers.IO) {
        git.checkout().apply {
            setName(ref.name)
            if (ref.name.startsWith("refs/remotes/")) {
                setCreateBranch(true)
                setName(ref.simpleName)
                setStartPoint(ref.name)
                setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
            }
            call()
        }
    }
}