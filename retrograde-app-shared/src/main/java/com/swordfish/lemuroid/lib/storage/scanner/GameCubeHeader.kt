package com.swordfish.lemuroid.lib.storage.scanner

object GameCubeHeader {
    fun matches(header: ByteArray): Boolean = header.size >= 32 &&
        header[28] == 0xC2.toByte() && header[29] == 0x33.toByte() &&
        header[30] == 0x9F.toByte() && header[31] == 0x3D.toByte()
}
