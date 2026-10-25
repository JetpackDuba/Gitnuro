package com.jetpackduba.gitnuro.data.mappers

import com.jetpackduba.gitnuro.domain.extensions.lineAt
import com.jetpackduba.gitnuro.domain.models.BlameLine
import com.jetpackduba.gitnuro.domain.models.BlameResult
import javax.inject.Inject
import org.eclipse.jgit.blame.BlameResult as JGitBlameResult

class JGitBlameMapper @Inject constructor(
    private val jGitCommitMapper: JGitCommitMapper,
    private val jGitIdentityMapper: JGitIdentityMapper,
) : DataMapper<BlameResult, JGitBlameResult> {
    override fun toData(value: BlameResult): Nothing {
        throw IllegalAccessException("Not implemented yet")
    }

    override fun toDomain(value: JGitBlameResult): BlameResult {
        val contents = value.resultContents
        val contentsSize = contents.size()

        val lines = mutableListOf<BlameLine>()

        repeat(contentsSize) { index ->
            val line = contents.lineAt(index)
            val author = value.getSourceAuthor(index)
            val commit = value.getSourceCommit(index)

            val blameLine = BlameLine(
                identity = jGitIdentityMapper.toDomainOrNull(author),
                commit = jGitCommitMapper.toDomainOrNull(commit),
                content = line,
            )

            lines.add(blameLine)
        }

        return BlameResult(
            path = value.resultPath.orEmpty(),
            lines = lines,
        )
    }
}