package com.jetpackduba.gitnuro.ssh.libssh

enum class sshAuthE(val value: Int) {
    SSH_AUTH_SUCCESS(0),
    SSH_AUTH_DENIED(1),
    SSH_AUTH_PARTIAL(2),
    SSH_AUTH_INFO(3),
    SSH_AUTH_AGAIN(4),
    SSH_AUTH_ERROR(-1)
}