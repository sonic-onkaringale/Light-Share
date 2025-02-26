import utils.decodeIp
import utils.encodeIp
import java.net.NetworkInterface
import java.net.SocketException

//Test Encoding and Decoding fof Ips
object Test
{
    fun scanWiFi()
    {
        //a.b.c.d
        // 192.168.0.0
        // 192.168.255.255
        for (c in 0..255)
        {
            for (d in 0..255)
            {
                val ip = "192.168.${c}.${d}"
                val encodeIp = encodeIp(ip)
                if (ip != decodeIp(encodeIp))
                {
                    println("$ip couldn't be encoded")

                }
            }
        }
    }

    fun scanEther()
    {
        //a.b.c.d
        // 172.16.0.0
        // 172.31.255.255
        for (b in 16..31)
        {
            for (c in 0..255)
            {
                for (d in 0..255)
                {
                    val ip = "172.${b}.${c}.${d}"
                    val encodeIp = encodeIp(ip)
                    if (ip != decodeIp(encodeIp))
                    {
                        println("$ip couldn't be encoded")

                    }
                }
            }
        }
    }

    fun scanPrivateOther()
    {
        //a.b.c.d
        // 10.0.0.0
        // 10.255.255.255
        for (b in 0..255)
        {
            for (c in 0..255)
            {
                for (d in 0..255)
                {
                    val ip = "10.${b}.${c}.${d}"
                    val encodeIp = encodeIp(ip)
                    if (ip != decodeIp(encodeIp))
                    {
                        println("$ip couldn't be encoded")

                    }
                }
            }
        }
    }

    fun testAllEncoding()
    {
        scanWiFi()
        scanEther()
        scanPrivateOther()
        println("All Ip Encoded and decoded correctly")
    }

}

data class IpInfo(
    var ip: String,
    var name: String?,
    var isRecommended: Boolean? = null
)

object IpUtils
{


    private fun sortIpAddresses(ipAddresses: List<IpInfo>): MutableList<IpInfo>
    {
        var wifiIps = mutableListOf<IpInfo>()
        var ethernetClassBIps = mutableListOf<IpInfo>()
        var classAIps = mutableListOf<IpInfo>()
        var otherIps = mutableListOf<IpInfo>()
        for (ip in ipAddresses)
        {
            if (ip.name == null)
                ip.isRecommended = false
            else
            {
                if (
                    ip.name!!.contains("Virtual", true) ||
                    ip.name!!.contains("WiFi Direct", true) ||
                    ip.name!!.contains("Wi-Fi Direct", true) ||
                    ip.name!!.contains("Microsoft", true) ||
                    ip.name!!.contains("Hyper-V", true)
                )
                    ip.isRecommended = false

            }
            when
            {
                ip.ip.startsWith("192.168.") -> wifiIps.add(ip) // WiFi IPs
                ip.ip.startsWith("172.16.") -> ethernetClassBIps.add(ip) // Ethernet IPs
                ip.ip.startsWith("10.") -> classAIps.add(ip) // Class A ips
                else -> otherIps.add(ip) // Other IPs
            }
        }


        wifiIps = wifiIps.sortedBy {
            it.ip
        }.toMutableList()
        ethernetClassBIps = ethernetClassBIps.sortedBy {
            it.ip
        }.toMutableList()
        classAIps = classAIps.sortedBy {
            it.ip
        }.toMutableList()
        otherIps = otherIps.sortedBy {
            it.ip
        }.toMutableList()



        return (wifiIps + ethernetClassBIps + classAIps + otherIps).toMutableList()
    }


    fun getNetworks(): MutableList<IpInfo>
    {
        val ips = mutableListOf<IpInfo>()
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
                    {

                        ips.add(IpInfo(inetAddress.hostAddress, intf.displayName))
//                        println("Intf Name ${intf.displayName} ")
//                        println("Should be ${inetAddress.hostAddress} -")

                    }
                }
            }
        }
        catch (ex: SocketException)
        {
            ex.printStackTrace()
        }
        return sortIpAddresses(ips)
    }

}

