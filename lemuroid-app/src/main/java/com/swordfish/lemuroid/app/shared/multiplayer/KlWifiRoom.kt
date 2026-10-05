package com.swordfish.lemuroid.app.shared.multiplayer

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * LAN room connection only. No emulated link traffic is attached yet.
 * The host counts as one of the three participants.
 */
class KlWifiRoom(private val port: Int = 55342) {
    enum class Phase { IDLE, CONNECTING, HOSTING, JOINED, ERROR }
    data class Member(val id: Int, val name: String)
    data class State(
        val phase: Phase = Phase.IDLE,
        val addresses: List<String> = emptyList(),
        val members: List<Member> = emptyList(),
        val message: String = "",
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val changes = MutableStateFlow(State())
    val state = changes.asStateFlow()
    @Volatile private var session: Session? = null

    private class Peer(val socket: Socket, val name: String) {
        val input = DataInputStream(socket.getInputStream())
        private val output = DataOutputStream(socket.getOutputStream())
        fun write(block: DataOutputStream.() -> Unit) {
            synchronized(output) {
                output.block()
                output.flush()
            }
        }
    }

    private class Session(val hostName: String) {
        @Volatile var closed = false
        @Volatile var server: ServerSocket? = null
        val sockets = ConcurrentHashMap.newKeySet<Socket>()
        val peers = ConcurrentHashMap<Int, Peer>()
        val lock = Any()
        val pending = AtomicInteger(0)
        fun close() {
            closed = true
            runCatching { server?.close() }
            sockets.forEach { runCatching { it.close() } }
            sockets.clear()
            peers.clear()
        }
    }

    private fun active(s: Session) = session === s && !s.closed
    private fun begin(name: String): Session {
        session?.close()
        val s = Session(name.trim().take(32).ifBlank { "Jogador" })
        session = s
        changes.value = State(phase = Phase.CONNECTING, message = "Conectando…")
        return s
    }

    fun host(name: String) {
        val s = begin(name)
        scope.launch {
            try {
                val server = ServerSocket()
                s.server = server
                server.reuseAddress = true
                server.bind(InetSocketAddress(port))
                if (!active(s)) { server.close(); return@launch }
                publishRoster(s)
                while (active(s)) {
                    val socket = server.accept()
                    if (!active(s)) { socket.close(); break }
                    s.sockets.add(socket)
                    if (s.pending.incrementAndGet() > 8) {
                        s.pending.decrementAndGet()
                        socket.close()
                        s.sockets.remove(socket)
                        continue
                    }
                    scope.launch { acceptVisitor(s, socket) }
                }
            } catch (error: Exception) {
                fail(s, "Não foi possível criar a sala. Saia da outra sala ou tente novamente.")
            }
        }
    }

    private fun publishRoster(s: Session) {
        if (!active(s)) return
        val members = listOf(Member(0, s.hostName)) +
            s.peers.entries.sortedBy { it.key }.map { Member(it.key, it.value.name) }
        changes.value = State(
            phase = Phase.HOSTING,
            addresses = localAddresses(),
            members = members,
            message = "Sala aberta — ${members.size}/3 jogadores",
        )
        s.peers.values.forEach { peer ->
            runCatching {
                peer.write {
                    writeInt(ROSTER)
                    writeInt(members.size)
                    members.forEach { writeInt(it.id); writeUTF(it.name) }
                }
            }.onFailure { runCatching { peer.socket.close() } }
        }
    }

    private suspend fun acceptVisitor(s: Session, socket: Socket) {
        var id = -1
        var pending = true
        try {
            socket.tcpNoDelay = true
            socket.soTimeout = 5000
            val peer = Peer(socket, "")
            require(peer.input.readInt() == MAGIC && peer.input.readInt() == VERSION)
            val name = peer.input.readUTF()
            require(name.isNotBlank() && name.length <= 32)
            val visitor = Peer(socket, name)
            synchronized(s.lock) {
                if (!active(s)) return
                id = (1..2).firstOrNull { !s.peers.containsKey(it) } ?: -1
                if (id < 0) {
                    visitor.write { writeInt(REJECTED); writeUTF("Sala cheia: o limite é de 3 jogadores.") }
                    return
                }
                visitor.write { writeInt(WELCOME); writeInt(id) }
                s.peers[id] = visitor
                socket.soTimeout = 30000
                publishRoster(s)
            }
            pending = false
            s.pending.decrementAndGet()
            while (active(s)) {
                require(visitor.input.readInt() == PING)
                visitor.write { writeInt(PONG) }
            }
        } catch (_: Exception) {
            // A broken socket removes only that visitor; the host room remains open.
        } finally {
            if (pending) s.pending.decrementAndGet()
            runCatching { socket.close() }
            s.sockets.remove(socket)
            synchronized(s.lock) {
                if (id >= 0) s.peers.remove(id)
                if (active(s)) publishRoster(s)
            }
        }
    }

    fun join(address: String, name: String) {
        val cleanAddress = address.trim()
        if (!validAddress(cleanAddress)) {
            leave()
            changes.value = State(phase = Phase.ERROR, message = "Digite o IP mostrado no celular anfitrião, por exemplo 192.168.1.10.")
            return
        }
        val s = begin(name)
        scope.launch {
            try {
                val socket = Socket()
                s.sockets.add(socket)
                socket.tcpNoDelay = true
                socket.connect(InetSocketAddress(cleanAddress, port), 5000)
                socket.soTimeout = 30000
                if (!active(s)) { socket.close(); return@launch }
                val peer = Peer(socket, s.hostName)
                peer.write { writeInt(MAGIC); writeInt(VERSION); writeUTF(s.hostName) }
                when (peer.input.readInt()) {
                    REJECTED -> {
                        fail(s, peer.input.readUTF())
                        return@launch
                    }
                    WELCOME -> require(peer.input.readInt() in 1..2)
                    else -> error("Unexpected handshake")
                }
                val heartbeat = scope.launch {
                    try {
                        while (active(s) && isActive) {
                            peer.write { writeInt(PING) }
                            delay(4000)
                        }
                    } catch (_: Exception) {
                        runCatching { socket.close() }
                    }
                }
                try {
                    while (active(s)) {
                        when (peer.input.readInt()) {
                            PONG -> Unit
                            ROSTER -> {
                                val count = peer.input.readInt()
                                require(count in 1..3)
                                val members = (0 until count).map {
                                    val id = peer.input.readInt()
                                    val memberName = peer.input.readUTF()
                                    require(id in 0..2 && memberName.length in 1..32)
                                    Member(id, memberName)
                                }
                                require(members.map { it.id }.distinct().size == count && members.first().id == 0)
                                if (active(s)) changes.value = State(
                                    phase = Phase.JOINED,
                                    addresses = listOf(cleanAddress),
                                    members = members,
                                    message = "Você entrou — $count/3 jogadores",
                                )
                            }
                            else -> error("Unexpected message")
                        }
                    }
                } finally {
                    heartbeat.cancel()
                }
            } catch (_: Exception) {
                fail(s, "A conexão terminou ou o anfitrião não respondeu. Confira o IP e se os celulares estão no mesmo Wi-Fi.")
            }
        }
    }

    private fun fail(s: Session, message: String) {
        if (!active(s)) return
        s.close()
        changes.value = State(phase = Phase.ERROR, message = message)
    }

    fun leave() {
        val old = session
        session = null
        old?.close()
        changes.value = State()
    }

    fun close() {
        leave()
        scope.cancel()
    }

    private fun localAddresses(): List<String> =
        runCatching {
            Collections.list(NetworkInterface.getNetworkInterfaces())
                .filter { it.isUp && !it.isLoopback }
                .sortedBy { if (it.name.startsWith("wlan") || it.name.startsWith("ap")) 0 else 1 }
                .flatMap { Collections.list(it.inetAddresses) }
                .filterIsInstance<Inet4Address>()
                .filter { !it.isLoopbackAddress && !it.isLinkLocalAddress }
                .mapNotNull { it.hostAddress }
                .distinct()
        }.getOrDefault(emptyList())

    companion object {
        fun validAddress(address: String): Boolean {
            val octets = address.trim().split('.')
            return octets.size == 4 && octets.all { it.matches(Regex("[0-9]{1,3}")) && it.toInt() in 0..255 }
        }

        const val MAGIC = 0x4B4C5746
        const val VERSION = 1
        const val WELCOME = 1
        const val REJECTED = 2
        const val ROSTER = 3
        const val PING = 4
        const val PONG = 5
    }
}
