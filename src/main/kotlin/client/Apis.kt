package client

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.streams.*
import io.ktor.websocket.*
import java.io.File
import java.io.FileInputStream

// Modified client
suspend fun uploadFiles(files: List<File>, onProgress: (Float) -> Unit)
{
    val totalSize = files.sumOf { it.length() }
    val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 300000 // 5 minutes timeout
        }
    }

    try
    {
        val response = client.post("http://localhost:8888/upload") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("totalSize", totalSize.toString())
                        files.forEach { file ->
                            appendInput(
                                key = "files",
                                headers = Headers.build {
                                    append(HttpHeaders.ContentDisposition, "filename=${file.name}")
                                }
                            ) {
                                file.inputStream().buffered().asInput()
                            }
                        }
                    }
                )
            )
            headers.append("fileSize", totalSize.toString())
            headers.remove(HttpHeaders.ContentLength)

            onUpload { bytesSentTotal, _ ->
                val progress = bytesSentTotal.toDouble() / totalSize
                onProgress(progress.toFloat().coerceIn(0f, 1f))
            }
        }

        println("Server response: ${response.status}")
        println(response.bodyAsText())
    }
    catch (e: Exception)
    {
        println("Upload failed: ${e.message}")
        throw e
    } finally
    {
        client.close()
    }
}

suspend fun uploadFileWs(files: List<File>, onProgress: (Float) -> Unit)
{
    val client = HttpClient(CIO) {
        install(WebSockets)
        install(HttpTimeout) {
            requestTimeoutMillis = 300000 // 5 minutes timeout
        }
    }

    try
    {
        client.webSocket(method = HttpMethod.Get, host = "localhost", port = 8888, path = "/file-share") {
            println("✅ Connected to WebSocket Server")

            // Send client identifier
            send(Frame.Text("Client_${System.currentTimeMillis()}"))

            for (fileToSend in files)
            {
                if (!fileToSend.exists())
                {
                    println("❌ File does not exist: ${fileToSend.absolutePath}")
                    continue
                }

                val fileSize = fileToSend.length()
                val fileName = fileToSend.name

                println("📤 Starting upload for: $fileName ($fileSize bytes)")

                // Send file metadata
                send(Frame.Text("META:$fileName:$fileSize"))

                // Send file in chunks
                FileInputStream(fileToSend).use { inputStream ->
                    val buffer = ByteArray(2048)
                    var bytesRead: Int
                    var sentBytes = 0L

                    try
                    {
                        while (inputStream.read(buffer).also { bytesRead = it } != -1)
                        {
                            send(Frame.Binary(true, buffer.copyOf(bytesRead)))
                            sentBytes += bytesRead

                            // Progress update
                            val progress = (sentBytes.toFloat() / fileSize) * 100
                            onProgress(progress)
                            println("📊 Upload Progress for $fileName: ${"%.2f".format(progress)}%")
                        }

                        // Notify server about completion
                        send(Frame.Text("COMPLETE:$fileName"))

                        // Wait for ACK from server
                        for (serverFrame in incoming)
                        {
                            if (serverFrame is Frame.Text)
                            {
                                val serverMsg = serverFrame.readText()
                                if (serverMsg == "ACK:$fileName")
                                {
                                    println("✅ Server acknowledged receipt of $fileName")
                                    break
                                }
                            }
                        }
                    }
                    catch (e: Exception)
                    {
                        println("❌ Error during file upload for $fileName: ${e.localizedMessage}")
                    }
                }
            }

            // Close WebSocket after uploads
            close(CloseReason(CloseReason.Codes.NORMAL, "Upload completed"))
            println("🔒 WebSocket connection closed.")
        }
    }
    catch (e: Exception)
    {
        println("❌ WebSocket Error: ${e.localizedMessage}")
    } finally
    {
        client.close()
        println("🔌 HTTP Client closed.")
    }
}



