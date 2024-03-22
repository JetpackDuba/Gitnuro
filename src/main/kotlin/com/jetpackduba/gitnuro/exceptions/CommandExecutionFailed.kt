package com.jetpackduba.gitnuro.exceptions

class CommandExecutionFailed(msg: String, cause: Exception) : GitnuroException(msg, cause) {
}