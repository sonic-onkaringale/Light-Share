package client

import androidx.compose.ui.geometry.Size
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*

import kotlinx.serialization.json.Json

import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.nio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import server.FileSizeRegister
import java.io.File
import java.io.FileInputStream
import java.util.*

// Initialize Ktor HttpClient with WebSocket support
val client = HttpClient(CIO) {

    install(ContentNegotiation) {
        json()
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 5 * 60 * 1000 // 5 minutes
    }
    engine {
        maxConnectionsCount = 100
    }

}

suspend fun sendFileSize(size: Long)
{
    client.post("http://localhost:8888/sendFileSize") {
        contentType(ContentType.Application.Json)
        setBody(FileSizeRegister(size))
    }
}

@OptIn(InternalAPI::class)
suspend fun uploadFiles2(
    files: List<File>,
    onUploadProgress: (percentage: Int) -> Unit
): String = withContext(Dispatchers.IO) {
    val uploadId = UUID.randomUUID().toString() // Generate uploadId client-side
    val file = files.firstOrNull() ?: return@withContext "No files selected" // Assuming single file for simplicity


    var totalSize = 0L
    try
    {
        val parts = files.map { currentFile ->
            println("[CLIENT] File Size: ${currentFile.name} - ${currentFile.length()} bytes")
            totalSize += currentFile.length()

            PartData.FileItem(
                provider = { FileInputStream(currentFile).channel.toByteReadChannel() },
                dispose = { },
                partHeaders = Headers.build {
                    append(
                        HttpHeaders.ContentDisposition,
                        "form-data; name=\"files\"; filename=\"${currentFile.name}\";fileSize:${totalSize}\";"
                    )
                    append(HttpHeaders.ContentType, "application/octet-stream")
                    append("fileSize", "$totalSize")
                }
            )
        }

        val response = HttpClient(CIO) {

            install(HttpTimeout) {
                requestTimeoutMillis = 5 * 60 * 1000 // 5 minutes
            }
            install(WebSockets) // Install WebSocket plugin
            engine {
                maxConnectionsCount = 100
            }

        }.submitFormWithBinaryData(

            url = "http://localhost:8888/upload", // Use localhost for IntelliJ testing
            formData = parts
        ) {
            headers {
                append("fileSize", totalSize.toString())
            }
            timeout {
                requestTimeoutMillis = 5 * 60 * 1000 // 5 minutes
            }

            onUpload { bytesSentTotal, contentLengthFromCallback ->
                val fileLength = file.length() // Use file.length() for client-side progress
                val progressPercentage = if (fileLength > 0)
                {
                    (bytesSentTotal * 100 / fileLength).toInt()
                }
                else 0
                onUploadProgress(progressPercentage) // Call the upload progress callback
//                println("[CLIENT ONUPLOAD] bytesSentTotal: $bytesSentTotal, File Length: $fileLength, Progress: $progressPercentage%")
            }
        }

        val responseBody: String = response.body<String>() // Use response.body<String>() to get text response
        return@withContext responseBody // Return the response body

    }
    catch (e: Exception)
    {

        return@withContext "Error: ${e.message}"
    } finally
    {

    }
}


