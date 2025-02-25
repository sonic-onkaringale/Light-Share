package server

import client.BytePacket
import client.DeviceInfo
import client.FileMetaData
import client.FilesMetaData
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import isReceived
import isReceiving
import isSending
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import receivingFrom
import receivingProgress
import utils.getDeviceName
import utils.getOS
import java.io.File
import kotlin.time.Duration.Companion.seconds


private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null



@Serializable
data class FileSizeRegister(val size: Long)


fun Application.module()
{
    install(WebSockets) {
        pingPeriod = 120.seconds
        timeout = (60 * 10).seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }

    install(ContentNegotiation) {
        json()
    }


    routing {


        get("/getDeviceDetails"){
            call.respond(DeviceInfo(getDeviceName(), getOS()))
        }



        webSocket("/uploadFilesWs") {
            val uploadDir = File("uploads").apply { mkdirs() }
            val deviceDetails = receiveDeserialized<DeviceInfo>()
            receivingFrom.value="${deviceDetails.deviceName} (${deviceDetails.os})"
//            sendSerialized(DeviceInfo(getDeviceName(), getOS()))
            flush()

            val allFilesMetadata = receiveDeserialized<FilesMetaData>()

            var totalBytesToReceive = 0L
            allFilesMetadata.metaData.forEach {
                totalBytesToReceive += it.fileSize
                println("File :${it.fileName} size : ${it.fileSize}")
            }
            println("totalBytesToReceive : $totalBytesToReceive")
            var totalBytesReceived = 0L
            isReceiving.value = true

            val allFileNames = allFilesMetadata.metaData.map {
                it.fileName
            }.toMutableList()
            receivingProgress.value.fileName = ArrayList(allFileNames)
            allFilesMetadata.metaData.forEach { fileMetaData: FileMetaData ->
                val newFile = File(uploadDir, fileMetaData.fileName)
                val fileOutputStream = newFile.outputStream()

                var bytesWrote = 0L

                for (i in 0 until fileMetaData.bytesSlicesRequired)
                {
                    val bytePacket = receiveDeserialized<BytePacket>()
                    fileOutputStream.write(bytePacket.byteArray, 0, bytePacket.dataRead)
                    //Calculations
                    bytesWrote += bytePacket.dataRead
                    totalBytesReceived += bytePacket.dataRead
                    println("File percent ${(totalBytesReceived * 100) / totalBytesToReceive}")

                    receivingProgress.value.percent = ((totalBytesReceived * 100) / totalBytesToReceive).toInt()

                }

                if (bytesWrote == fileMetaData.fileSize)
                    println("File Size Matched : ${fileMetaData.fileName}")
                else
                    println("File Not Size Matched : ${fileMetaData.fileName} - Expected : ${fileMetaData.fileSize} but got : $bytesWrote")
                println()
                fileOutputStream.close()
                isReceiving.value = false
                isSending.value = false
                isReceived.value=true
            }

            println("BytesToReceive :  $totalBytesToReceive, ByteReceived : $totalBytesReceived")
            close(CloseReason(CloseReason.Codes.NORMAL, "File Received"))
        }
    }


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




