package server

import client.ByteCopyProgressListener
import client.ourReadBytes
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import isReceiving
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import receiving

import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap
import kotlin.math.roundToInt


private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? =
    null


@Serializable
data class ApiResponse(val message: String, val data: List<String>)


@Serializable
data class AfterDiscoverDetails(val deviceName: String, val deviceIp: String)

@Serializable
data class FileSizeRegister(val size: Long)

data class FileMetadata(val fileName: String, val fileSize: Long)

object Pingers
{

    val fileSizeToReceive = HashMap<String, Long>()
    val _progressChannel = MutableSharedFlow<ProgressUpdate>() // Use MutableSharedFlow (private backing property)
    val progressChannel = _progressChannel.asSharedFlow() // Expose as SharedFlow (read-only)
    val fileUploadProgress = ConcurrentHashMap<String, UploadProgress>()

}

fun Application.module()
{


    install(ContentNegotiation) {
        json()
    }


    routing {
        get("/hi") {
            val responseData = ApiResponse(
                message = "Hi",
                data = listOf("Onkar", "Usman", "Sanyog", "Sanket")
            )
            call.respond(responseData) // Ktor automatically serializes to JSON
        }

        install(WebSockets) {

        }
        get("/discover_bare_bones") {
            call.respond(HttpStatusCode.OK, "")
        }

        get("/discovery") {
            call.respond(
                AfterDiscoverDetails(
                    getDeviceNameWithUserNameIfPossible(),
                    ""
                )
            )
        }


        // Modified server code
//        post("/upload") {
//            val multipart = call.receiveMultipart()
//            val connections = mutableListOf<DefaultWebSocketSession>()
//            var fileCount = 0
//
//            multipart.forEachPart { part ->
//                if (part is PartData.FileItem)
//                {
//                    val fileName = part.originalFileName ?: "uploaded_file_$fileCount"
//                    val file = File("uploads/$fileName")
//                    file.parentFile.mkdirs()
//
//                    var bytesCopied = 0L
//                    val totalBytes = part.headers[HttpHeaders.ContentLength]?.toLong() ?: -1
//
//                    part.streamProvider().use { input ->
//                        file.outputStream().buffered().use { output ->
//                            val buffer = ByteArray(4096)
//                            var bytesRead: Int
//                            while (input.read(buffer).also { bytesRead = it } != -1)
//                            {
//                                output.write(buffer, 0, bytesRead)
//                                bytesCopied += bytesRead
//                                val progress = if (totalBytes > 0) (bytesCopied * 100) / totalBytes else -1
//
//                                // Send progress to all connected clients
//                                connections.forEach { session ->
//                                    session.send("Uploading $fileName: $progress%")
//                                }
//                            }
//                        }
//                    }
//                    fileCount++
//                }
//                part.dispose()
//            }
//
//            call.respondText("All files uploaded successfully.", status = HttpStatusCode.OK)
//        }


        post("/sendFileSize") {
            val req = call.receive(FileSizeRegister::class)
            val ip = call.request.host()
            Pingers.fileSizeToReceive[ip] = req.size
            req.size
        }

        post("/upload") {
            val multipart = call.receiveMultipart(formFieldLimit = Long.MAX_VALUE)

            multipart.forEachPart { part ->
                if (part is PartData.FileItem)
                {
                    receiving.value.fileName += "\n ${part.originalFileName}"
                }
            }

            multipart.forEachPart { part ->
                if (part is PartData.FileItem)
                {
                    val uploadId = UUID.randomUUID().toString()

                    val fileName = part.originalFileName ?: "unknown"

                    val fileSize = Pingers.fileSizeToReceive[call.request.host()]

                    val byteCopyProgressListener = object : ByteCopyProgressListener
                    {
                        override fun progressOnByteRead(bytesRead: Long)
                        {
//                            println("Bytes Read $bytesRead")
                            val percent = (bytesRead * 100) / fileSize!!
                            println("File percent $percent")
                            isReceiving.value = true
                            receiving.value.percent = percent.toInt()

                        }

                    }
                    val fileBytes = part.provider().toInputStream()
                        .ourReadBytes(byteCopyProgressListener)
                    val file = File("uploads/$fileName")
                    file.parentFile?.mkdirs()
                    file.writeBytes(fileBytes)

                    val progressTracker = UploadProgress(uploadId, fileName, fileBytes.size.toLong())
                    Pingers.fileUploadProgress[uploadId] = progressTracker


//                    var bytesReceived = 0L
//                    val mb = 1024 * 1024

//                    withContext(Dispatchers.IO) {
//                        part.provider().toInputStream().use { input ->
//                            FileOutputStream(file).use { output ->
//                                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
//                                var bytes = input.read(buffer)
//                                while (bytes >= 0)
//                                {
//                                    output.write(buffer, 0, bytes)
//                                    bytesReceived += bytes
//                                    progressTracker.bytesUploaded = bytesReceived
//
//                                    if (bytesReceived % mb == 0L)
//                                    {
//                                        val progressPercent =
//                                            (bytesReceived.toDouble() / progressTracker.totalBytes.toDouble() * 100).roundToInt()
//                                        println("[SERVER] Received part for fileId: $uploadId, ${bytesReceived / mb}MB (${bytesReceived} bytes total)")
//
//                                        launch {
//                                            println("Emit")
//                                            Pingers._progressChannel.emit( // Use _progressChannel to emit
//                                                ProgressUpdate(
//                                                    uploadId,
//                                                    fileName,
//                                                    progressPercent,
//                                                    bytesReceived,
//                                                    progressTracker.totalBytes
//                                                )
//                                            )
//                                        }
//                                    }
//                                    bytes = input.read(buffer)
//                                }
//                            }
//                        }
//                    }
                    part.dispose()
                    println("[SERVER] File uploaded to '${file.path}' for fileId: $uploadId")
                }
            }
            call.respondText("Files uploaded!", status = HttpStatusCode.OK)
        }


    }

    // ... other routes ...

}


fun getDeviceNameWithUserNameIfPossible(): String
{
    val userName: String? = "Onkar"
    val model = (if (userName != null)
    {
        userName + "'s"
    }
    else
    {
        ""
    }) + "getDeviceName()"
    return model
}

fun startKtorServer()
{
    server = embeddedServer(
        Netty, port = 8888, host = "0.0.0.0"
    ) { // "0.0.0.0" for all interfaces
        module() // Your Ktor application module
    }.start(wait = false) // Don't wait for server to stop in the main thread
}

// Add memory logging to track improvements
fun logMemory()
{
    val runtime = Runtime.getRuntime()
    val used = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
    val max = runtime.maxMemory() / 1024 / 1024
    println("Memory usage: ${used}MB / ${max}MB")
}


data class UploadProgress(
    val fileId: String,
    val fileName: String,
    val totalBytes: Long,
    var bytesUploaded: Long = 0
)

@Serializable
data class ProgressUpdate(
    val fileId: String,
    val fileName: String,
    val percentage: Int,
    val bytesUploaded: Long,
    val totalBytes: Long
)

fun toJson(progressUpdate: ProgressUpdate): String
{
    progressUpdate.toString()
    return Json.encodeToString(progressUpdate)
}