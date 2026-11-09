package com.gauto.app.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.gauto.app.R
import java.io.BufferedReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

/**
 * LAN-синхронизация Gauto с ПК.
 *
 * HTTP на 8765: `GET /api/ping`, `GET /api/state`, `POST /api/state`.
 * UDP на 8766: ПК шлёт `GAUTO_DISCOVER_v1`, телефон отвечает адресом порта и именем.
 * Всё только внутри локальной сети, без облака, аккаунта и токена.
 */
class GautoSyncService : Service() {

    @Volatile private var running = false
    private var serverSocket: ServerSocket? = null
    private var datagramSocket: DatagramSocket? = null
    private var httpThread: Thread? = null
    private var udpThread: Thread? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(3, notification())
        if (!running) start()
        return START_STICKY
    }

    private fun start() {
        running = true
        GautoStore(applicationContext).ensurePin()
        httpThread = Thread { httpLoop() }.apply { name = "gauto-sync-http"; start() }
        udpThread = Thread { discoveryLoop() }.apply { name = "gauto-sync-udp"; start() }
    }

    private fun httpLoop() {
        val socket = runCatching {
            ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress(SyncProtocol.PORT)) }
        }.getOrNull()
        serverSocket = socket ?: return
        while (running) {
            val client = runCatching { socket.accept() }.getOrNull() ?: break
            Thread { handle(client) }.apply { name = "gauto-sync-client" }.start()
        }
    }

    private fun handle(client: Socket) {
        runCatching {
            client.use { socket ->
                socket.soTimeout = 8_000
                val input = socket.getInputStream().bufferedReader()
                val requestLine = input.readLine() ?: return
                val headers = mutableMapOf<String, String>()
                var headerLine = input.readLine()
                while (!headerLine.isNullOrEmpty()) {
                    val colon = headerLine.indexOf(':')
                    if (colon > 0) {
                        headers[headerLine.substring(0, colon).trim().lowercase()] =
                            headerLine.substring(colon + 1).trim()
                    }
                    headerLine = input.readLine()
                }
                val length = headers["content-length"]?.toIntOrNull() ?: 0
                val body = readBody(input, length)
                val parts = requestLine.split(' ')
                val method = parts.getOrNull(0).orEmpty()
                val path = parts.getOrNull(1).orEmpty()
                val store = GautoStore(applicationContext)
                val pin = store.settings().syncPin
                val providedPin = headers[SyncProtocol.PIN_HEADER.lowercase()]
                val (code, payload) = when {
                    path == "/api/ping" && method == "GET" -> 200 to SyncProtocol.ping()
                    !SyncProtocol.pinValid(providedPin, pin) -> 401 to "{\"error\":\"pin\"}"
                    path == "/api/state" && method == "GET" ->
                        200 to SyncProtocol.encode(store.snapshot())
                    path == "/api/state" && method == "POST" -> {
                        val accepted = store.applyRemote(body)
                        if (accepted == null) 400 to "{\"error\":\"state\"}"
                        else 200 to SyncProtocol.encode(accepted)
                    }
                    else -> 404 to "{\"error\":\"not found\"}"
                }
                val bytes = payload.toByteArray(Charsets.UTF_8)
                val out = socket.getOutputStream()
                val head = "HTTP/1.1 $code ${reason(code)}\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Content-Length: ${bytes.size}\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Access-Control-Allow-Headers: ${SyncProtocol.PIN_HEADER}\r\n" +
                    "Connection: close\r\n\r\n"
                out.write(head.toByteArray(Charsets.UTF_8))
                out.write(bytes)
                out.flush()
            }
        }
    }

    private fun readBody(reader: BufferedReader, length: Int): String {
        if (length <= 0) return ""
        val buffer = CharArray(length.coerceAtMost(2 * 1024 * 1024))
        var read = 0
        while (read < buffer.size) {
            val n = reader.read(buffer, read, buffer.size - read)
            if (n < 0) break
            read += n
        }
        return String(buffer, 0, read)
    }

    private fun discoveryLoop() {
        val socket = runCatching {
            DatagramSocket(null).apply { reuseAddress = true; bind(InetSocketAddress(SyncProtocol.DISCOVERY_PORT)) }
        }.getOrNull() ?: return
        datagramSocket = socket
        val buffer = ByteArray(256)
        while (running) {
            val packet = DatagramPacket(buffer, buffer.size)
            if (runCatching { socket.receive(packet) }.isFailure) break
            val message = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()
            if (message == SyncProtocol.DISCOVER_MAGIC) {
                val device = runCatching {
                    GautoStore(applicationContext).settings().syncDeviceName
                }.getOrDefault("Gauto phone")
                val reply = SyncProtocol.here(device = device).toByteArray(Charsets.UTF_8)
                runCatching {
                    socket.send(DatagramPacket(reply, reply.size, packet.address, packet.port))
                }
            }
        }
    }

    override fun onDestroy() {
        running = false
        runCatching { serverSocket?.close() }
        runCatching { datagramSocket?.close() }
        httpThread?.interrupt()
        udpThread?.interrupt()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(): Notification = Notification.Builder(this, CHANNEL_SYNC)
        .setSmallIcon(R.drawable.ic_gauto_foreground)
        .setContentTitle("Gauto · синхронизация с ПК")
        .setContentText("${localIpAddress()}:${SyncProtocol.PORT} · PIN в настройках")
        .setOngoing(true)
        .build()

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SYNC, "Синхронизация с ПК", NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
        )
    }

    companion object {
        const val CHANNEL_SYNC = "gauto_sync"

        fun start(context: Context) {
            runCatching {
                context.startForegroundService(Intent(context, GautoSyncService::class.java))
            }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, GautoSyncService::class.java)) }
        }

        /** Первый не-loopback IPv4 адрес устройства в локальной сети. */
        fun localIpAddress(): String = runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress }
                ?.hostAddress
                .orEmpty()
        }.getOrDefault("")

        private fun reason(code: Int): String = when (code) {
            200 -> "OK"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            404 -> "Not Found"
            else -> "Error"
        }
    }
}
