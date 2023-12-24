package com.jetpackduba.gitnuro.git.branches

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.CheckoutCommand
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectIdRef
import org.junit.jupiter.api.Test

class CheckoutRefUseCaseTest {
    private val gitMock: Git = mockk()
    private val refMock: ObjectIdRef.PeeledNonTag = mockk(relaxed = true)

    @Test
    fun `invokes git checkout when ref is a local branch`() {
        val checkoutCommand = mockk<CheckoutCommand>(relaxed = true)
        val branchName = "refs/heads/feature-branch"
        every { refMock.name } returns branchName
        every { gitMock.checkout() } returns checkoutCommand

        every { checkoutCommand.setName(any()) } returns checkoutCommand

        runBlocking {
            CheckoutRefUseCase().invoke(gitMock, refMock)
        }

        verify { checkoutCommand.setName(branchName) }
    }

    @Test
    fun `invokes git checkout when ref is a remote branch`() {
        val checkoutCommand = mockk<CheckoutCommand>(relaxed = true)
        val branchName = "refs/remotes/origin/feature-branch"
        every { refMock.name } returns branchName
        every { gitMock.checkout() } returns checkoutCommand

        every { checkoutCommand.setCreateBranch(true) } returns checkoutCommand
        every { checkoutCommand.setStartPoint(any() as String) } returns checkoutCommand
        every { checkoutCommand.setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM) } returns checkoutCommand

        runBlocking {
            CheckoutRefUseCase().invoke(gitMock, refMock)
        }

        verify {
            checkoutCommand.setCreateBranch(true)
            checkoutCommand.setStartPoint(branchName)
            checkoutCommand.setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
        }
    }
}