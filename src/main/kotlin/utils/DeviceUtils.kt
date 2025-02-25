package utils

import java.net.InetAddress
import java.net.UnknownHostException


fun getDeviceName(): String {
    return try {
        InetAddress.getLocalHost().hostName
    } catch (e: UnknownHostException) {
        "Unknown"
    }
}



fun getOS(): String {
    return System.getProperty("os.name")
}
