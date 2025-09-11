package dev.wycey.mido.fraiselait.packet

import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

internal data class Frame(
  val type: UShort,
  val frameId: UInt,
  val totalChunks: UShort,
  val received: AtomicInteger = AtomicInteger(0),
  var lastUpdate: Instant = Instant.now(),
  val chunks: MutableList<ByteArray> = MutableList(totalChunks.toInt()) { byteArrayOf() }
)
