package com.jetpackduba.gitnuro.domain.exceptions

class CommandExecutionFailed(msg: String, cause: Exception) : GitnuroException(msg, cause) {
}