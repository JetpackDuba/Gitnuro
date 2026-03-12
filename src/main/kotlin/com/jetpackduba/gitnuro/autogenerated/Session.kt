
//package 

class Session private constructor(val pointer: Long) : AutoCloseable {
    private val resource: NativeResource = thread.addObject(this, pointer, "Session") { SessionObj.destroy(it) }


    fun setup(
        host: String,
        user: String,
        port: Int?,
    ): String =
        SessionObj.setup(this.pointer, host, user, port)
    

    fun publicKeyAuth(
        password: String,
    ): Int =
        SessionObj.publicKeyAuth(this.pointer, password)
    

    fun passwordAuth(
        password: String,
    ): Int =
        SessionObj.passwordAuth(this.pointer, password)
    

    fun disconnect() =
        SessionObj.disconnect(this.pointer)
    

    override fun close() {
        if (thread.contains(resource)) {
            resource.close()
            thread.remove(resource)
        }  else {
            println("Session was already closed")
        }
    }

    companion object {
    
    fun new(): Session? =
        SessionObj.new()
    
    }
}

private object SessionObj {
        external fun new(): Session?

    external fun setup(
        pointer: Long,
        host: String,
        user: String,
        port: Int?,
    ): String

    external fun publicKeyAuth(
        pointer: Long,
        password: String,
    ): Int

    external fun passwordAuth(
        pointer: Long,
        password: String,
    ): Int

    external fun disconnect(
        pointer: Long,
    )

    external fun destroy(pointer: Long)
}
