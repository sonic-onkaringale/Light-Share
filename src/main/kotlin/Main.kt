import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import client.FileSentProgressListener
import client.client
import client.getDeviceDetails
import client.uploadFilesWebsockets
import com.dokar.sonner.ToastType
import com.dokar.sonner.Toaster
import com.dokar.sonner.rememberToasterState
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import server.startKtorServer
import utils.decodeIp
import utils.encodeIp
import utils.generateQRCode
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

data class ReceivingUpdate(
    var fileName: ArrayList<String> = ArrayList(), var percent: MutableState<Int> = mutableStateOf(0)
)

var scope = CoroutineScope(Dispatchers.IO)
var monitorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
var isChangeServer = mutableStateOf(true)
var changedServerDueTo = mutableStateOf("")

var currentIp = mutableStateOf("0.0.0.0")
var currentNetworkName = mutableStateOf("All Interface")

var isReceiving = mutableStateOf(false)
var receivingFrom = mutableStateOf("")
var sendingTo = mutableStateOf("")
val receivingProgress = mutableStateOf(ReceivingUpdate())
var isSending = mutableStateOf(false)
var isSent = mutableStateOf(false)
var isReceived = mutableStateOf(false)


var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
fun main() = application {

//    Test.testAllEncoding()
    val windowState = rememberWindowState()
    windowState.isMinimized = false
    Window(
        onCloseRequest = {
            exitApplication()
        }, title = "Light Share - File Sharing App",
        state = windowState
    ) {
        App(
        )
    }


}

private fun startServer(ip: String = "0.0.0.0")
{
    stopServer()
    server = startKtorServer(ip)
    runBlocking {
        server?.engine?.resolvedConnectors()?.forEach {
            val hostIp = it.host
            currentIp.value = hostIp

            val encodedIp = encodeIp(hostIp)
            println("Encoded $hostIp to $encodedIp")
            println("Decoded $encodedIp to ${decodeIp(encodedIp)}")

        }
    }
    monitorNetwork()

}

private fun stopServer()
{
    isReceiving.value = false
    isSending.value = false

    server?.stop(0, 0)
    server = null
}

@Composable
@Preview
fun App(

)
{
    val toaster = rememberToasterState()
    var code by remember { mutableStateOf("") }
    val encodedIp = encodeIp(currentIp.value)
    var selectedFiles by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    var fileMutableList: MutableList<File> = remember { mutableListOf() }

    var sendingProgress = remember { mutableStateOf(0F) }
    var noOfFiles by remember { mutableStateOf(0) }
    val clipboardManager = LocalClipboardManager.current

    var isEnabledSendFile by remember { mutableStateOf(true) }






    MaterialTheme {
        if (isChangeServer.value)
        {
            changeServerUi()
        }
        Toaster(
            state = toaster,
            alignment = Alignment.TopEnd, richColors = true
        )
        LazyColumn {
            item {

                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text("Light Share", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(currentIp.value)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(generateQRCode(encodedIp).toComposeImageBitmap(), "Qr Code for receiving file")
                        Spacer(Modifier.width(16.dp))
                        val first = encodedIp.substring(0, 4)
                        val last = encodedIp.substring(4, 8)
                        Column(
                            Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                Modifier.padding(4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SelectionContainer {
                                    Text(
                                        "Your Connection Code : $first-$last",
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                }
                                IconButton(
                                    {
                                        toaster.show("Code Copied to Clipboard")
                                        val annotatedString = AnnotatedString(encodedIp)
                                        clipboardManager.setText(annotatedString)


                                    },
                                    modifier = Modifier.padding(4.dp)

                                ) {
                                    Icon(Icons.Default.ContentCopy, "Copy Code")
                                }
                            }

                            Text("You can skip dash if you wish", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.fillMaxWidth().height(16.dp))
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {

                        Button({
                            val files = browseFile()

                            if (files != null)
                            {
                                for (file in files)
                                {
                                    if (!file.isFile)
                                    {
                                        toaster.show("Invalid File Selected", type = ToastType.Error)
                                        return@Button
                                    }
                                }
                                fileMutableList.clear()
                                fileMutableList.addAll(files.toMutableList())
                                var fileNames = ""
                                for (i in 0 until files.size)
                                {
                                    fileNames += "\n ${i + 1}. ${files[i].name}"
                                }
                                selectedFiles = fileNames
                            }
                            else selectedFiles = ""
                        }) {
                            if (selectedFiles.isNullOrBlank()) Text("Select Files to Send Someone")
                            else Text("Cancel Current Selection and Select Another Files to Send")
                        }
                        Spacer(Modifier.fillMaxWidth().height(16.dp))
                        if (selectedFiles.isNotBlank())
                        {
                            Row(
                                Modifier.padding(24.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(modifier = Modifier.width(350.dp), value = code, onValueChange = {
                                    code = it.uppercase()
                                }, label = {
                                    Text("Enter code of device to send files")
                                }, isError = errorText.isNotBlank(), supportingText = {
                                    if (errorText.isNotBlank()) Text(errorText)
                                })
                                Spacer(Modifier.width(16.dp))
                                Button(
                                    {
                                        isEnabledSendFile = false
                                        CoroutineScope(Dispatchers.IO).launch {
                                            val deviceDetails =
                                                try
                                                {

                                                    getDeviceDetails(client, decodeIp(code.replace("-", "")))

                                                }
                                                catch (e: Exception)
                                                {
                                                    isReceiving.value = false
                                                    isSending.value = false
                                                    isEnabledSendFile = true
                                                    toaster.show("Failed to connect.", type = ToastType.Error)
                                                    return@launch
                                                }
                                            sendingTo.value = "${deviceDetails.deviceName} (${deviceDetails.os})"
                                            isSending.value = true
                                            noOfFiles = fileMutableList.size
                                            var allFileSize = 0L
                                            fileMutableList.forEach { allFileSize += it.length() }


                                            try
                                            {

                                                uploadFilesWebsockets(
                                                    fileMutableList,
                                                    client,
                                                    decodeIp(code.replace("-", "")),
                                                    object : FileSentProgressListener
                                                    {
                                                        override fun report(percent: Int)
                                                        {
                                                            println("Sent $percent")
                                                            sendingProgress.value = percent.toFloat()
                                                        }

                                                    })
                                                isEnabledSendFile = true

                                            }
                                            catch (e: Exception)
                                            {
                                                isReceiving.value = false
                                                isSending.value = false
                                                isEnabledSendFile = true
                                                toaster.show(e.message ?: "Error Occurred", type = ToastType.Error)
                                            }
                                            isSent.value = true

                                        }
                                    }, enabled = try
                                    {
                                        decodeIp(code.replace("-", ""))
                                        errorText = ""
                                        isEnabledSendFile
                                    }
                                    catch (e: Exception)
                                    {
                                        errorText = "Enter Valid Connection-Code"
                                        false
                                    }
                                )

                                {
                                    Text("Send Files")
                                }
                            }
                        }
                        Text(selectedFiles)
                        if (isSending.value)
                        {
                            sendUi(sendingProgress, noOfFiles)
                        }
                        if (isReceiving.value)
                        {
                            receiveUi()
                        }
                        if (isSent.value)
                        {
                            Dialog(
                                onDismissRequest = {
                                    isSent.value = false

                                },
                            ) {

                            }
                            AlertDialog(
                                onDismissRequest = { isSent.value = false },
                                title = {
                                    Text("Files sent to ${sendingTo.value}")
                                },
                                text = {
                                    Column(
                                        Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            "Done",
                                            tint = Color(82, 146, 69),
                                            modifier = Modifier.size(56.dp)
                                        )

                                        Text("Files sent successfully.")
                                    }
                                },
                                confirmButton = {
                                    Button(onClick = { isSent.value = false }) {
                                        Text("OK")
                                        code = ""
                                        selectedFiles = ""
                                        fileMutableList.clear()

                                    }
                                })
                        }
                        if (isReceived.value)
                        {
                            Dialog(
                                onDismissRequest = {
                                    isReceived.value = false
                                },
                            ) {

                            }
                            AlertDialog(
                                onDismissRequest = { isReceived.value = false },
                                title = {
                                    Text("Files received from ${receivingFrom.value}")
                                },
                                text = {
                                    Column(
                                        Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            "Done",
                                            tint = Color(82, 146, 69),
                                            modifier = Modifier.size(56.dp)
                                        )
                                        Text("Files received successfully.")
                                    }

                                },
                                confirmButton = {
                                    Button(onClick = { isReceived.value = false }) {
                                        Text("OK")
                                    }
                                })
                        }

                    }


                }
            }
        }

    }
}

@Composable
fun changeServerUi()
{
    val allIpList = IpUtils.getNetworks()
    AlertDialog(onDismissRequest = {},
        confirmButton = {},
        title = {
            if (changedServerDueTo.value.isBlank())
            {
                Text("Pick network to bind on.")
            }
            else
                Text(changedServerDueTo.value)

        },
        text = {
            LazyColumn {

                allIpList.forEachIndexed { index, it ->
                    item {
                        Card(modifier = Modifier.padding(4.dp), onClick = {
                            currentNetworkName.value = it.name ?: "Unknown Network"
                            changedServerDueTo.value = ""
                            scope.launch {
                                startServer(it.ip)
                            }
                            isChangeServer.value = false
                        }) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                Row(Modifier.fillMaxWidth()) {
                                    Text(it.name ?: "Unknown Network")

                                }
                                Row(Modifier.fillMaxWidth()) {
                                    Text(it.ip)
                                    Spacer(Modifier.weight(1f))
                                    if (it.isRecommended != null)
                                    {
                                        if (it.isRecommended!!)
                                            Text("Recommended")
                                        else
                                            Text("Not Recommended")
                                    }
                                }
                            }

                        }
                    }
                }
            }

        }
    )

}

fun monitorNetwork()
{

    println("Monitoring Started")
    monitorScope.launch {
        while (true)
        {
            val ips = IpUtils.getNetworks()
            var got = false
            ips.forEach {
                if (it.name == currentNetworkName.value && it.ip == currentIp.value)
                {
//                    println("Monitoring Matched")
                    got = true
                }
            }
            if (!got)
            {
                println("Server Changed")
                stopServer()
                changedServerDueTo.value = "Network Disconnected, Reselect the Network."
                isChangeServer.value = true
                break
            }
            delay(1000)
        }
    }
}


@Composable
fun receiveUi()
{
    Column(Modifier.fillMaxWidth()) {
        Text("Receiving ${if (receivingProgress.value.fileName.size == 1) "file" else "files"} from ${receivingFrom.value}")
        LinearProgressIndicator({
            normalizeTheIndicator(receivingProgress.value.percent.value.toFloat())
        })
        var fileNames = ""
        val fileNameList = fileNames.toList()
        for (i in fileNameList.indices)
        {
            fileNames += "\n ${i + 1}. ${fileNameList[i]}"
        }
        Text(fileNames, style = MaterialTheme.typography.bodyMedium)
    }

}

fun normalizeTheIndicator(percent: Float): Float
{
    return percent / 100
}


@Composable
fun sendUi(progress: MutableState<Float>, noOfFiles: Int)
{
    Column(Modifier.fillMaxWidth()) {
        Text("Sending ${if (noOfFiles == 1) "file" else "files"} to ${sendingTo.value}")
        LinearProgressIndicator({
            normalizeTheIndicator(
                progress.value
            )
        })
//        var fileNames = ""
//        val fileNameList = fileNames.toList()
//        for (i in fileNameList.indices)
//        {
//            fileNames += "\n ${i + 1}. ${fileNameList[i]}"
//        }
//        Text(fileNames, style = MaterialTheme.typography.bodyMedium)
    }

}


fun browseFile(): Array<out File>?
{
    val fileDialog = FileDialog(null as Frame?, "Choose a file to share", FileDialog.LOAD)
    fileDialog.isMultipleMode = true
    fileDialog.isVisible = true

    val files = fileDialog.files

    if (files.isNotEmpty()) return files


    return null
}