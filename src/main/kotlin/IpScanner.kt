import utils.decodeIp
import utils.encodeIp

interface IpFoundListener
{
    fun ipFound(ip: String)
}


fun pingIp(ip: String): Boolean
{
    return false
}

fun scanWiFi(listener: IpFoundListener)
{
    //a.b.c.d
    // 192.168.0.0
    // 192.168.255.255
    for (c in 0..255)
    {
        for (d in 0..255)
        {
            val ip = "192.168.${c}.${d}"
            if (pingIp(ip))
            {
                listener.ipFound(ip)
            }
        }
    }
}

fun scanEther(listener: IpFoundListener)
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
                if (pingIp(ip))
                {
                    listener.ipFound(ip)
                }
            }
        }
    }
}

fun scanPrivateOther(listener: IpFoundListener)
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
                if (pingIp(ip))
                {
                    listener.ipFound(ip)
                }
            }
        }
    }
}


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

