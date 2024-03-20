package dev.wycey.mido.pui.util

import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

val KClass<*>.isInterface: Boolean
  get() = java.isInterface

val superclassCache = mutableMapOf<KClass<*>, KClass<*>>()

fun KClass<*>.hasSuperclassUntil(until: KClass<*>): Boolean {
  if (superclassCache[this] == until) {
    return true
  }

  var current = this

  while (current != until) {
    if (current == Any::class) {
      return false
    }

    current = current.superclasses.filterNot { it.isInterface || it == current }.single()
  }

  superclassCache[this] = current

  return true
}