package client


import Constants
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import isReceiving
import isSending
import kotlinx.serialization.Serializable
import utils.calculateNumberOfSlices
import utils.getDeviceName
import utils.getOS
import java.io.File


@Serializable
data class FileMetaData(
    val fileName: String,
    val fileSize: Long,
    val bytesSlicesRequired: Long
)

@Serializable
data class FilesMetaData(
    val metaData: List<FileMetaData>
)

@Serializable
data class BytePacket(
    val byteArray: ByteArray,
    val dataRead: Int
)
{
    override fun equals(other: Any?): Boolean
    {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BytePacket

        if (dataRead != other.dataRead) return false
        if (!byteArray.contentEquals(other.byteArray)) return false

        return true
    }

    override fun hashCode(): Int
    {
        var result = dataRead.hashCode()
        result = 31 * result + byteArray.contentHashCode()
        return result
    }
}

@Serializable
data class DeviceInfo(
    val deviceName: String,
    val os: String
)

suspend fun uploadFilesWebsockets(
    files: List<File>,
    client: HttpClient,
    ip: String,
    fileSentProgressListener: FileSentProgressListener
)
{
    val totalSize = files.sumOf { it.length() }
    var totalBytesSent = 0L
    var flag = false
    try
    {

        client.webSocket("ws://$ip:8888/uploadFilesWs")
        {
            sendSerialized(DeviceInfo(getDeviceName(), getOS()))
            val filesMetadata = FilesMetaData(files.map {
                FileMetaData(it.name, it.length(), calculateNumberOfSlices(it.length()))
            })
            sendSerialized(filesMetadata)
            flush()
            var buffer: ByteArray = ByteArray(Constants.Buffer.fileBuffer.toInt())
            files.forEachIndexed { index: Int, file: File ->
                val inputStream = file.inputStream()
                for (i in 0 until filesMetadata.metaData[index].bytesSlicesRequired)
                {
                    if (i.toInt() % 10 == 0)
                    {
                        println("Calling gc")
                        System.gc()
                    }
                    val bytesRead = inputStream.read(buffer)

                    val data = BytePacket(buffer, bytesRead)
                    if (!flag)
                    {
//                    println(Json.encodeToString(data))
                        flag = true
                    }
                    sendSerialized(data)
                    flush()

                    //
                    totalBytesSent += bytesRead
                    fileSentProgressListener.report(((totalBytesSent * 100) / totalSize).toInt())
                }
                System.gc()
            }
            System.gc()
            flush()
            isReceiving.value = false
            isSending.value = false
        }
    }
    catch (e: Exception)
    {
        println(e.stackTrace)

        throw RuntimeException("Failed to connect")
    }
}

suspend fun getDeviceDetails(
    client: HttpClient,
    ip: String,
): DeviceInfo
{
    val response = try
    {
        client.get("http://$ip:8888/getDeviceDetails")
    }
    catch (e: Exception)
    {
        println("Failed to connect")
        throw e
    }
    return response.body<DeviceInfo>()
}
