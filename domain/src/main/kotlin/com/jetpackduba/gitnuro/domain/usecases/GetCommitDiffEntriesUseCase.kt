package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.models.Commit
import org.eclipse.jgit.diff.DiffEntry
import javax.inject.Inject

class GetCommitDiffEntriesUseCase @Inject constructor() {
    suspend operator fun invoke(commit: Commit): List<DiffEntry> {
        return emptyList()
    }
}