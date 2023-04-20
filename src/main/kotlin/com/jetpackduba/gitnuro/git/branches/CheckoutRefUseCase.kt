package com.jetpackduba.gitnuro.git.branches

import com.jetpackduba.gitnuro.extensions.isBranch
import com.jetpackduba.gitnuro.extensions.simpleName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class CheckoutRefUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, ref: Ref): Unit = withContext(Dispatchers.IO) {
        git.checkout().apply {
            setName(ref.name)
            if (ref.isBranch && ref.name.startsWith("refs/remotes/")) {
                setCreateBranch(true)
                setName(ref.simpleName)
                setStartPoint(ref.name)
                setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
            }
            call()
        }
    }
}