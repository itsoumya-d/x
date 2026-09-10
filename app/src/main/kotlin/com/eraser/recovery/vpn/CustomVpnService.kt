package com.eraser.recovery.vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

class CustomVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private lateinit var blocklistManager: BlocklistManager

    companion object {
        const val VPN_ADDRESS = "10.0.0.2"
        const val VPN_ROUTE = "0.0.0.0"
        const val PROXY_HOST = "127.0.0.1"
        const val PROXY_PORT = 8080
        const val DNS_SERVER = "8.8.8.8"

        @Volatile
        private var instance: CustomVpnService? = null

        fun isRunning(): Boolean {
            return instance != null
        }

        fun getStatistics(): Map<String, Any>? {
            return instance?.getStatistics()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        blocklistManager = BlocklistManager(this)
        blocklistManager.initialize()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == BlocklistManager.ACTION_UPDATE_BLOCKLIST) {
            val blocked = intent.getStringArrayExtra(BlocklistManager.EXTRA_BLOCKED_DOMAINS)
            val whitelisted = intent.getStringArrayExtra(BlocklistManager.EXTRA_WHITELISTED_DOMAINS)
            blocklistManager.updateBlocklist(blocked?.toList() ?: emptyList(), whitelisted?.toList() ?: emptyList())
        } else {
            startVpn()
        }
        return START_STICKY
    }

    private fun startVpn(host: String = PROXY_HOST, port: Int = PROXY_PORT) {
        val builder = Builder()
        builder.setSession("CustomVpnService")
        builder.addAddress(VPN_ADDRESS, 24)
        builder.addRoute(VPN_ROUTE, 0)
        builder.addDnsServer(DNS_SERVER)
        vpnInterface = builder.establish()

        val fileDescriptor = vpnInterface!!.fileDescriptor

        Thread {
            val input = FileInputStream(fileDescriptor)
            val output = FileOutputStream(fileDescriptor)
            val packet = ByteBuffer.allocate(32767)

            while (true) {
                val length = input.read(packet.array())
                if (length > 0) {
                    val destinationAddress = getDestinationAddress(packet.array())
                    if (destinationAddress != null && destinationAddress.hostAddress == DNS_SERVER) {
                        handleDnsPacket(packet.array(), length, output)
                        continue
                    }

                    val tunnel = DatagramChannel.open()
                    tunnel.connect(InetSocketAddress(host, port))
                    protect(tunnel.socket())
                    packet.limit(length)
                    tunnel.write(packet)
                    packet.clear()

                    val read = tunnel.read(packet)
                    if (read > 0) {
                        packet.limit(read)
                        output.write(packet.array(), 0, read)
                        packet.clear()
                    }
                    tunnel.close()
                }
            }
        }.start()
    }

    private fun handleDnsPacket(packet: ByteArray, length: Int, output: FileOutputStream) {
        try {
            var offset = 12
            var domain = ""
            while (true) {
                val len = packet[offset].toInt()
                if (len == 0) {
                    offset++
                    break
                }
                domain += String(packet, offset + 1, len) + "."
                offset += len + 1
            }

            if (blocklistManager.shouldBlockDomain(domain)) {
                return
            }

            val dnsRequest = ByteBuffer.wrap(packet, 0, length)
            val dnsChannel = DatagramChannel.open()
            dnsChannel.connect(InetSocketAddress(DNS_SERVER, 53))
            protect(dnsChannel.socket())
            dnsChannel.write(dnsRequest)

            val dnsResponse = ByteBuffer.allocate(32767)
            val read = dnsChannel.read(dnsResponse)
            if (read > 0) {
                output.write(dnsResponse.array(), 0, read)
            }
            dnsChannel.close()

        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun getDestinationAddress(packet: ByteArray): InetAddress? {
        return try {
            val ipVersion = packet[0].toInt() shr 4
            if (ipVersion == 4) {
                val destinationIp = ByteArray(4)
                System.arraycopy(packet, 16, destinationIp, 0, 4)
                InetAddress.getByAddress(destinationIp)
            } else if (ipVersion == 6) {
                val destinationIp = ByteArray(16)
                System.arraycopy(packet, 24, destinationIp, 0, 16)
                InetAddress.getByAddress(destinationIp)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getStatistics(): Map<String, Any> {
        return blocklistManager.getStatistics()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        vpnInterface?.close()
        blocklistManager.shutdown()
    }
}