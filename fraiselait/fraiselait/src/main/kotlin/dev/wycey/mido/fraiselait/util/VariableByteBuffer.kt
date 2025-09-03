package dev.wycey.mido.fraiselait.util

import java.nio.ByteBuffer
import java.nio.ByteOrder

public class VariableByteBuffer
  @JvmOverloads
  constructor(
    order: ByteOrder = ByteOrder.LITTLE_ENDIAN
  ) {
    private companion object {
      private const val INITIAL_CAPACITY = 256
      private const val GROWTH_FACTOR = 2
    }

    private var buffer = ByteBuffer.allocate(INITIAL_CAPACITY).order(order)

    public var order: ByteOrder
      get() =
        buffer.order()
      set(value) {
        buffer.order(value)
      }

    public var size: Int = 0
      private set

    public val capacity: Int
      get() = buffer.capacity()

    public val array: ByteArray
      get() = buffer.array().copyOfRange(0, size)

    public val arrayOffset: Int
      get() = buffer.arrayOffset()

    public fun duplicate(): VariableByteBuffer {
      val newVbb = VariableByteBuffer(order)

      newVbb.ensureCapacity(size)
      newVbb.size = size

      buffer.position(0)
      newVbb.buffer.put(buffer.duplicate().limit(size))

      return newVbb
    }

    public fun get(index: Int): Byte {
      if (index !in 0..<size) {
        throw IndexOutOfBoundsException("Index: $index, Size: $size")
      }

      return buffer.get(index)
    }

    public fun get(): Byte {
      if (buffer.position() + 1 > size) {
        throw IndexOutOfBoundsException("Size: $size")
      }

      return buffer.get()
    }

    @JvmOverloads
    public fun get(
      dst: ByteArray,
      offset: Int = 0,
      length: Int = dst.size
    ) {
      if (length < 0 || offset < 0 || offset + length > dst.size) {
        throw IndexOutOfBoundsException("Offset: $offset, Length: $length, Dst size: ${dst.size}")
      }

      if (buffer.position() + length > size) {
        throw IndexOutOfBoundsException("Length: $length, Size: $size")
      }

      buffer.get(dst, offset, length)
    }

    public fun getShort(index: Int): Short {
      if (index !in 0..<(size - 1)) {
        throw IndexOutOfBoundsException("Index: $index, Size: $size")
      }

      return buffer.getShort(index)
    }

    public fun getShort(): Short {
      if (buffer.position() + 2 > size) {
        throw IndexOutOfBoundsException("Size: $size")
      }

      return buffer.getShort()
    }

    public fun getInt(index: Int): Int {
      if (index !in 0..<(size - 3)) {
        throw IndexOutOfBoundsException("Index: $index, Size: $size")
      }

      return buffer.getInt(index)
    }

    public fun getInt(): Int {
      if (buffer.position() + 4 > size) {
        throw IndexOutOfBoundsException("Size: $size")
      }

      return buffer.getInt()
    }

    public fun getLong(index: Int): Long {
      if (index !in 0..<(size - 7)) {
        throw IndexOutOfBoundsException("Index: $index, Size: $size")
      }

      return buffer.getLong(index)
    }

    public fun getLong(): Long {
      if (buffer.position() + 8 > size) {
        throw IndexOutOfBoundsException("Size: $size")
      }

      return buffer.getLong()
    }

    public fun getFloat(index: Int): Float {
      if (index !in 0..<(size - 3)) {
        throw IndexOutOfBoundsException("Index: $index, Size: $size")
      }

      return buffer.getFloat(index)
    }

    public fun getFloat(): Float {
      if (buffer.position() + 4 > size) {
        throw IndexOutOfBoundsException("Size: $size")
      }

      return buffer.getFloat()
    }

    public fun getDouble(index: Int): Double {
      if (index !in 0..<(size - 7)) {
        throw IndexOutOfBoundsException("Index: $index, Size: $size")
      }

      return buffer.getDouble(index)
    }

    public fun getDouble(): Double {
      if (buffer.position() + 8 > size) {
        throw IndexOutOfBoundsException("Size: $size")
      }

      return buffer.getDouble()
    }

    public fun put(value: Byte) {
      ensureCapacity(size + 1)

      buffer.put(value)

      if (size < buffer.position()) {
        size = buffer.position()
      }
    }

    @JvmOverloads
    public fun put(
      src: ByteArray,
      offset: Int = 0,
      length: Int = src.size
    ) {
      if (length < 0 || offset < 0 || offset + length > src.size) {
        throw IndexOutOfBoundsException("Offset: $offset, Length: $length, Src size: ${src.size}")
      }

      val newSize = maxOf(size + length, buffer.position() + length)

      ensureCapacity(newSize)
      buffer.put(src, offset, length)

      size = newSize
    }

    public fun putShort(
      index: Int,
      value: Short
    ) {
      if (index !in 0..<(size - 1)) {
        throw IndexOutOfBoundsException("Index: $index, Size: $size")
      }

      buffer.putShort(index, value)
    }

    public fun putShort(value: Short) {
      val newSize = maxOf(size + 2, buffer.position() + 2)

      ensureCapacity(newSize)

      buffer.putShort(value)
      size = newSize
    }

    public fun putInt(
      index: Int,
      value: Int
    ) {
      if (index !in 0..<(size - 3)) {
        throw IndexOutOfBoundsException("Index: $index, Size: $size")
      }

      buffer.putInt(index, value)
    }

    public fun putInt(value: Int) {
      val newSize = maxOf(size + 4, buffer.position() + 4)

      ensureCapacity(newSize)

      buffer.putInt(value)
      size = newSize
    }

    public fun putLong(
      index: Int,
      value: Long
    ) {
      if (index !in 0..<(size - 7)) {
        throw IndexOutOfBoundsException("Index: $index, Size: $size")
      }

      buffer.putLong(index, value)
    }

    public fun putLong(value: Long) {
      val newSize = maxOf(size + 8, buffer.position() + 8)

      ensureCapacity(newSize)

      buffer.putLong(size, value)
      size = newSize
    }

    public fun putFloat(
      index: Int,
      value: Float
    ) {
      if (index !in 0..<(size - 3)) {
        throw IndexOutOfBoundsException("Index: $index, Size: $size")
      }

      buffer.putFloat(index, value)
    }

    public fun putFloat(value: Float) {
      val newSize = maxOf(size + 4, buffer.position() + 4)

      ensureCapacity(newSize)

      buffer.putFloat(size, value)
      size = newSize
    }

    public fun putDouble(
      index: Int,
      value: Double
    ) {
      if (index !in 0..<(size - 7)) {
        throw IndexOutOfBoundsException("Index: $index, Size: $size")
      }

      buffer.putDouble(index, value)
    }

    public fun putDouble(value: Double) {
      val newSize = maxOf(size + 8, buffer.position() + 8)

      ensureCapacity(newSize)

      buffer.putDouble(size, value)
      size = newSize
    }

    public fun slice(): ByteBuffer = buffer.slice().order(order)

    public fun clear() {
      buffer.clear()

      size = 0
    }

    public fun reserve(newCapacity: Int) {
      if (newCapacity > capacity) {
        extend(newCapacity)
      }
    }

    public fun flip() {
      buffer.flip()
    }

    public fun rewind() {
      buffer.rewind()
    }

    private fun ensureCapacity(minCapacity: Int) {
      if (minCapacity > capacity) {
        extend(minCapacity)
      }
    }

    private fun extend(minCapacity: Int) {
      var newCapacity = capacity

      while (newCapacity < minCapacity) {
        newCapacity *= GROWTH_FACTOR
      }

      val newBuffer = ByteBuffer.allocate(newCapacity).order(order)

      buffer.position(0)
      newBuffer.put(buffer.duplicate().limit(size))

      buffer = newBuffer
    }
  }
