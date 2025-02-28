package utils

import org.jetbrains.skia.Image
import qrcode.QRCode


fun generateQRCode(content: String): Image
{
    val qrCode = QRCode.ofSquares()
//        .withColor(Colors.DEEP_SKY_BLUE) // Default is Colors.BLACK
        .withSize(10) // Default is 25
        .build(content)


    val pngBytes = Image.makeFromEncoded(qrCode.render().getBytes())
    return pngBytes

}

fun encodeIp(ip: String): String
{
    val octets = ip.split('.').map { it.toInt() }
    require(octets.size == 4) { "Invalid IP format" }
    octets.forEach { require(it in 0..255) { "Invalid octet value: $it" } }

    return octets.joinToString("") { String.format("%02X", it) }
}

fun decodeIp(code: String): String
{
    require(code.length == 8) { "Invalid code length for code $code and code length ${code.length}" }

    return code.chunked(2).map { pair ->
        pair.toInt(16)
    }.joinToString(".")
}
