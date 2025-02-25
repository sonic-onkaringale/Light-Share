package utils

import Constants
import debug
import err
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun convertIntTo4ByteArray(number: Int): ByteArray
{
    val buffer = ByteBuffer.allocate(4)
    buffer.order(ByteOrder.BIG_ENDIAN)
    buffer.putInt(number)
    val byteArray = buffer.array()
    return byteArray
}

fun convert4ByteToInt(byteArray: ByteArray): Int
{
    if (byteArray.size != 4)
    {
        err("Byte array not 4 bytes")
    }
    val buffer = ByteBuffer.wrap(byteArray)
    buffer.order(ByteOrder.BIG_ENDIAN)
    return buffer.getInt()
}


fun calculateNumberOfSlices(
    numOfBytes: Long,
    sizeOfBuffer: Long = Constants.Buffer.fileBuffer
): Long {
    debug("Num of bytes : $numOfBytes and Size of Buffer : $sizeOfBuffer")
    if (numOfBytes == 0L) {
        err("Zero bytes in slices")
    }
    val slicesDouble = numOfBytes.toDouble() / sizeOfBuffer.toDouble()
    return Math.ceil(slicesDouble).toLong()
}

