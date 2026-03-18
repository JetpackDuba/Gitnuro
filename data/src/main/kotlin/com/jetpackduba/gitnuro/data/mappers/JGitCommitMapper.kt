package com.jetpackduba.gitnuro.data.mappers

import com.jetpackduba.gitnuro.domain.models.Commit
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class JGitCommitMapper @Inject constructor(
    private val identityMapper: JGitIdentityMapper,
): DataMapper<Commit, RevCommit> {
    override fun toData(value: Commit): Nothing {
        TODO("Not yet implemented")
    }

    override fun toDomain(value: RevCommit): Commit {
        with (value) {
            return Commit(
                hash = name,
                message = fullMessage,
                committer = identityMapper.toDomain(committerIdent),
                author = identityMapper.toDomain(authorIdent),
                date = committerIdent.whenAsInstant.epochSecond
            )
        }
    }

}