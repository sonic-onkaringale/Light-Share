//import androidx.compose.material3.icons.filled.FolderOpen
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import client.sendFileSize
import client.uploadFiles2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import server.startKtorServer
import utils.decodeIp
import utils.encodeIp
import utils.generateQRCode
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.NetworkInterface
import java.net.SocketException

data class ReceivingUpdate(
    val fileName: ArrayList<String> = ArrayList(), var percent: Int = 0
)

var isReceiving = mutableStateOf(false)
val receiving = mutableStateOf(ReceivingUpdate())


fun main() = application {

    startKtorServer()

    val encodedIp = encodeIp(getLocalIpAddress()!!)

    Window(onCloseRequest = {

        exitApplication()
    }, title = "File Sharing App") {
        App(

        )
    }

    println("Encoded ${getLocalIpAddress()} to $encodedIp")
    println("Decoded $encodedIp to ${decodeIp(encodedIp)}")
//    Test.testAllEncoding()
}

@Composable
@Preview
fun App(

)
{
    var code by remember { mutableStateOf("") }
    val encodedIp = encodeIp(getLocalIpAddress()!!)
    var selectedFiles by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    var fileMutableList: MutableList<File> = remember { mutableListOf() }
    var isSending by remember { mutableStateOf(false) }
    var sendingProgress = remember { mutableStateOf(0F) }
    var noOfFiles by remember { mutableStateOf(0) }


    println("Encoded ${getLocalIpAddress()} to $encodedIp")
    println("Decoded $encodedIp to ${decodeIp(encodedIp)}")
    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Light Share", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(getLocalIpAddress() ?: "")
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
                    Text(
                        "You can even join by using this code : $first-$last",
                        style = MaterialTheme.typography.headlineSmall
                    )
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
                            code = it
                        }, label = {
                            Text("Enter code of device to send files")
                        }, isError = errorText.isNotBlank(), supportingText = {
                            if (errorText.isNotBlank()) Text(errorText)
                        })
                        Spacer(Modifier.width(16.dp))
                        Button(
                            {

                                isSending = true
                                CoroutineScope(Dispatchers.IO).launch {
                                    noOfFiles = fileMutableList.size
//                                    uploadFiles(fileMutableList){
//                                        sendingProgress.value=it
//                                    }
                                    var allFileSize = 0L
                                    fileMutableList.forEach { allFileSize += it.length() }
                                    sendFileSize(allFileSize)

                                    uploadFiles2(fileMutableList) {
                                        sendingProgress.value = it.toFloat()
                                    }
                                    isSending = false
                                }
                            }, enabled = try
                            {
                                decodeIp(code.replace("-", ""))
                                errorText = ""
                                true
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
                if (isSending)
                {
                    sendUiWithoutFileNames(sendingProgress, "Legion", noOfFiles)
                }
                if (isReceiving.value)
                {
                    receiveUi("")
                }
            }


        }
    }
}

@Composable
fun receiveUi(receiversName: String)
{
    Column(Modifier.fillMaxWidth()) {
        Text("Receiving ${if (receiving.value.fileName.size == 1) "file" else "files"} from $receiversName")
        LinearProgressIndicator({
            receiving.value.percent.toFloat()
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

@Composable
fun sendUi(progress: MutableState<Float>, filesName: MutableList<String>, receiversName: String)
{
    Column(Modifier.fillMaxWidth()) {
        Text("Sending ${if (filesName.size == 1) "file" else "files"} to $receiversName")
        LinearProgressIndicator({
            progress.value
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

@Composable
fun sendUiWithoutFileNames(progress: MutableState<Float>, receiversName: String, noOfFiles: Int)
{
    Column(Modifier.fillMaxWidth()) {
        Text("Sending ${if (noOfFiles == 1) "file" else "files"} to $receiversName")
        LinearProgressIndicator({
            progress.value
        })

    }

}

fun getLocalIpAddress(): String?
{
    try
    {
        val en = NetworkInterface.getNetworkInterfaces()
        while (en.hasMoreElements())
        {
            val intf = en.nextElement()
            val enumIpAddr = intf.inetAddresses
            while (enumIpAddr.hasMoreElements())
            {
                val inetAddress = enumIpAddr.nextElement()
                if (!inetAddress.isLoopbackAddress && inetAddress is java.net.Inet4Address)
                { // Only IPv4, non-loopback
                    return inetAddress.hostAddress
                }
            }
        }
    }
    catch (ex: SocketException)
    {
        ex.printStackTrace()
    }
    return null
}

fun browseFile(): Array<out File>?
{
    val fileDialog = FileDialog(null as Frame?, "Choose a file to share", FileDialog.LOAD)
    fileDialog.isVisible = true
    fileDialog.isMultipleMode = true

    val files = fileDialog.files

    if (files.isNotEmpty()) return files

//    if (directory != null && filename != null)
//    {
//        val file = File(directory, filename)
//        return file
//    }
    return null
}