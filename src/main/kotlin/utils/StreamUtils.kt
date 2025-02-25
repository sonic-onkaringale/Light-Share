import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.math.min


fun readExactBytes(inputStream: InputStream, exactBytes: Int)
{
    inputStream.readNBytesOur(exactBytes)

}

const val MAX_BUFFER_SIZE: Int = Int.MAX_VALUE - 8
const val DEFAULT_BUFFER_SIZE: Int = 8192

@Throws(IOException::class)
fun InputStream.readNBytesOur(len: Int): ByteArray
{
    require(len >= 0) { "len < 0" }

    var buffs: MutableList<ByteArray>? = null
    var result: ByteArray? = null
    var total = 0
    var remaining = len
    var n: Int
    do
    {
        var buf = ByteArray(
            min(remaining.toDouble(), DEFAULT_BUFFER_SIZE.toDouble())
                .toInt()
        )
        var nread = 0

        // read to EOF which may read more or less than buffer size
        while ((read(
                buf, nread,
                min((buf.size - nread), remaining)
            ).also { n = it }) > 0
        )
        {
            nread += n
            remaining -= n
        }

        if (nread > 0)
        {
            if (MAX_BUFFER_SIZE - total < nread)
            {
                throw OutOfMemoryError("Required array size too large")
            }
            if (nread < buf.size)
            {
                buf = Arrays.copyOfRange(buf, 0, nread)
            }
            total += nread
            if (result == null)
            {
                result = buf
            }
            else
            {
                if (buffs == null)
                {
                    buffs = ArrayList()
                    buffs.add(result)
                }
                buffs.add(buf)
            }
        }
        // if the last call to read returned -1 or the number of bytes
        // requested have been read then break
    } while (n >= 0 && remaining > 0)

    if (buffs == null)
    {
        if (result == null)
        {
            return ByteArray(0)
        }
        return if (result.size == total) result else result.copyOf(total)
    }

    result = ByteArray(total)
    var offset = 0
    remaining = total
    for (b in buffs)
    {
        val count = min(b.size.toDouble(), remaining.toDouble()).toInt()
        System.arraycopy(b, 0, result, offset, count)
        offset += count
        remaining -= count
    }

    return result
}


fun transferNBytes(input: InputStream, output: OutputStream, len: Int)
{
    require(len >= 0) { "len must be non-negative" }
    var remaining = len
    val buffer =
        ByteArray(DEFAULT_BUFFER_SIZE) // Continue until you've reached len bytes or hit EOF.
    while (remaining > 0)
    {
// Read at most the remaining bytes or the buffer size.
        val bytesToRead = min(buffer.size, remaining)
        val n = input.read(buffer, 0, bytesToRead)
        if (n == -1)
        { // End of stream reached
            break
        }
        output.write(buffer, 0, n)
        remaining -= n
    } // Optionally, flush the output stream.
    output.flush()
}

@Throws(IOException::class)
fun transferNBytesSafe(
    input: InputStream,
    output: OutputStream,
    len: Int,
    flushOutput: Boolean = true,
    callBackBytesWritten: ((bytesWrote: Long) -> Unit)? = null
)
{
    require(len >= 0) { "len must be non-negative" }
    var remaining = len
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

    // Optional: count consecutive zero reads
    var zeroReadCount = 0
    val maxZeroReads = 12  // adjust as needed

    var bytesWrote = 0L
    while (remaining > 0)
    {
        // Read at most the remaining bytes or the full buffer.
        val bytesToRead = min(buffer.size, remaining)
        val n = input.read(buffer, 0, bytesToRead)
        if (n == -1)
        { // End of stream reached
            break
        }
        if (n == 0)
        {
            zeroReadCount++
            if (zeroReadCount >= maxZeroReads)
            {
                // If we get too many zero-length reads, break out
                break
            }
            continue  // try to read again
        }
        else
        {
            zeroReadCount = 0  // reset on a successful read
        }

        if (callBackBytesWritten != null)
        {
            bytesWrote += n
            callBackBytesWritten.invoke(bytesWrote)
        }
        output.write(buffer, 0, n)
        remaining -= n
    }



    if (flushOutput)
    {
        output.flush()
    }
}