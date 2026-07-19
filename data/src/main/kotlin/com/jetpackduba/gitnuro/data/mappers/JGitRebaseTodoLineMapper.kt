package com.jetpackduba.gitnuro.data.mappers

import com.jetpackduba.gitnuro.domain.models.RebaseLine
import org.eclipse.jgit.lib.RebaseTodoLine
import javax.inject.Inject

class JGitRebaseTodoLineMapper @Inject constructor() : DataMapper<RebaseLine, RebaseTodoLine> {
    override fun toData(value: RebaseLine): Nothing {
        throw NotImplementedError("Mapping of RebaseLine domain to data not implemented")
    }

    override fun toDomain(value: RebaseTodoLine): RebaseLine {
        return RebaseLine(
            action = toDomainRebaseAction(value.action),
            commit = value.commit.name(),
            shortMessage = value.shortMessage,
            fullMessage = "",
        )
    }

    private fun toDomainRebaseAction(value: RebaseTodoLine.Action): RebaseLine.Action {
        return when (value) {
            RebaseTodoLine.Action.PICK -> RebaseLine.Action.PICK
            RebaseTodoLine.Action.REWORD -> RebaseLine.Action.REWORD
            RebaseTodoLine.Action.EDIT -> RebaseLine.Action.EDIT
            RebaseTodoLine.Action.SQUASH -> RebaseLine.Action.SQUASH
            RebaseTodoLine.Action.FIXUP -> RebaseLine.Action.FIXUP
            RebaseTodoLine.Action.COMMENT -> RebaseLine.Action.COMMENT
        }
    }
}