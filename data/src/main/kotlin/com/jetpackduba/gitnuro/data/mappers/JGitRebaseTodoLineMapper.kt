package com.jetpackduba.gitnuro.data.mappers

import com.jetpackduba.gitnuro.domain.models.RebaseAction
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
        )
    }

    private fun toDomainRebaseAction(value: RebaseTodoLine.Action): RebaseAction {
        return when (value) {
            RebaseTodoLine.Action.PICK -> RebaseAction.PICK
            RebaseTodoLine.Action.REWORD -> RebaseAction.REWORD
            RebaseTodoLine.Action.EDIT -> RebaseAction.EDIT
            RebaseTodoLine.Action.SQUASH -> RebaseAction.SQUASH
            RebaseTodoLine.Action.FIXUP -> RebaseAction.FIXUP
            RebaseTodoLine.Action.COMMENT -> RebaseAction.COMMENT
        }
    }
}