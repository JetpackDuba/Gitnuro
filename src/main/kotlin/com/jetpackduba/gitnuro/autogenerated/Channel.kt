
//package 

class Channel private constructor(val pointer: Long) : AutoCloseable {
    private val resource: NativeResource = thread.addObject(this, pointer, "Channel") { ChannelObj.destroy(it) }


    fun openSession(): String =
        ChannelObj.openSession(this.pointer)
    

    fun isOpen(): Boolean =
        ChannelObj.isOpen(this.pointer)
    

    fun closeChannel(): String =
        ChannelObj.closeChannel(this.pointer)
    

    fun requestExec(
        command: String,
    ): String =
        ChannelObj.requestExec(this.pointer, command)
    

    fun pollHasBytes(
        is_stderr: Boolean,
    ): Boolean =
        ChannelObj.pollHasBytes(this.pointer, is_stderr)
    

    fun read(
        is_stderr: Boolean,
        len: Long,
    ): ReadResult? =
        ChannelObj.read(this.pointer, is_stderr, len)
    

    fun writeByte(
        byte: Int,
    ): String =
        ChannelObj.writeByte(this.pointer, byte)
    

    fun writeBytes(
        data: ByteArray,
    ): String =
        ChannelObj.writeBytes(this.pointer, data)
    

    override fun close() {
        if (thread.contains(resource)) {
            resource.close()
            thread.remove(resource)
        }  else {
            println("Channel was already closed")
        }
    }

    companion object {
    
    fun new(
        session: Session,
    ): Channel? =
        ChannelObj.new(session)
    
    }
}

private object ChannelObj {
        external fun new(
        session: Session,
    ): Channel?

    external fun openSession(
        pointer: Long,
    ): String

    external fun isOpen(
        pointer: Long,
    ): Boolean

    external fun closeChannel(
        pointer: Long,
    ): String

    external fun requestExec(
        pointer: Long,
        command: String,
    ): String

    external fun pollHasBytes(
        pointer: Long,
        is_stderr: Boolean,
    ): Boolean

    external fun read(
        pointer: Long,
        is_stderr: Boolean,
        len: Long,
    ): ReadResult?

    external fun writeByte(
        pointer: Long,
        byte: Int,
    ): String

    external fun writeBytes(
        pointer: Long,
        data: ByteArray,
    ): String

    external fun destroy(pointer: Long)
}
