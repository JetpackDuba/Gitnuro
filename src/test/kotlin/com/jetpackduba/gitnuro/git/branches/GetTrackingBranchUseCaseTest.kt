package com.jetpackduba.gitnuro.git.branches

import io.mockk.every
import io.mockk.mockk
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.StoredConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetTrackingBranchUseCaseTest {
    private val gitMock: Git = mockk(relaxed = true)
    private val repositoryMock: Repository = mockk(relaxed = true)
    private val configMock: StoredConfig = mockk(relaxed = true)
    private val refMock: Ref = mockk(relaxed = true)

    private val localBranchName = "feature-branch"
    private val remoteName = "origin"
    private val remoteBranchFullName = "refs/heads/main"
    private val remoteBranchShortName = "main"
    private val objectId = ObjectId.zeroId()

    @BeforeEach
    fun setUp() {
        every { gitMock.repository } returns repositoryMock
        every { refMock.name } returns "refs/heads/$localBranchName"
        every { refMock.objectId } returns objectId
        every { repositoryMock.config } returns configMock
    }

    @Test
    fun `invoke returns null when remote not found`() {
        every { configMock.getString("branch", localBranchName, "remote") } returns null
        every { configMock.getString("branch", localBranchName, "merge") } returns null

        val result = GetTrackingBranchUseCase()(gitMock, refMock)

        assertNull(result)
    }

    @Test
    fun `invoke returns tracking branch when local and remote branches exist`() {
        every { configMock.getString("branch", localBranchName, "remote") } returns remoteName
        every { configMock.getString("branch", localBranchName, "merge") } returns remoteBranchFullName

        val result = GetTrackingBranchUseCase()(gitMock, refMock)

        assertEquals(TrackingBranch(remoteName, remoteBranchShortName), result)
    }
}