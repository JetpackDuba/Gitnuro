package com.jetpackduba.gitnuro.domain.models

import com.jetpackduba.gitnuro.domain.exceptions.GitnuroException

data class Error(
    val taskType: TaskType,
    val date: Long,
    val exception: Exception,
    val isUnhandled: Boolean,
)


fun newErrorNow(
    taskType: TaskType,
    exception: Exception,
): Error {
    return Error(
        taskType = taskType,
        date = System.currentTimeMillis(),
        exception = exception,
        isUnhandled = exception !is GitnuroException
    )
}
