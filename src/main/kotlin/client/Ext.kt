package client

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream


interface ByteCopyProgressListener
{
    fun progressOnByteRead(bytesRead: Long)
}

public fun InputStream.ourReadBytes(byteCopyProgressListener: ByteCopyProgressListener): ByteArray
{
    val buffer = ByteArrayOutputStream(maxOf(DEFAULT_BUFFER_SIZE, this.available()))
    ourCopyTo(buffer, byteCopyProgressListener)
    return buffer.toByteArray()
}

public fun InputStream.ourCopyTo(
    out: OutputStream,
    progressListener: ByteCopyProgressListener,
    bufferSize: Int = DEFAULT_BUFFER_SIZE
): Long
{
    var bytesCopied: Long = 0
    val buffer = ByteArray(bufferSize)
    var bytes = read(buffer)
    while (bytes >= 0)
    {
        out.write(buffer, 0, bytes)
        bytesCopied += bytes
        progressListener.progressOnByteRead(bytesCopied)
        bytes = read(buffer)
    }
    return bytesCopied
}