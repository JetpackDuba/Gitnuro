
//package 

class Signing private constructor(val pointer: Long) : AutoCloseable {
    private val resource: NativeResource = thread.addObject(this, pointer, "Signing") { SigningObj.destroy(it) }



    override fun close() {
        if (thread.contains(resource)) {
            resource.close()
            thread.remove(resource)
        }  else {
            println("Signing was already closed")
        }
    }

    companion object {
    
    fun signData(
        data: ByteArray,
        key: String,
        password: String,
    ): String =
        SigningObj.signData(data, key, password)
    
    }
}

private object SigningObj {
        external fun signData(
        data: ByteArray,
        key: String,
        password: String,
    ): String

    external fun destroy(pointer: Long)
}
