package dev.wycey.mido.pui.tree

import java.util.*

/*
 * Depth first tree implementation in Kotlin
 *
 * (c) 2024 dekavit
 *
 * Modified by Mido
 */

class DepthFirstTree<T> : Iterable<T> {
  private val root = TreeNode<T>(0, 0, -1, 0)
  private val nodes = mutableListOf(TreeMap<Int, TreeNode<T>>())
  private var depth = 1
  private var id = 1

  init {
    for (i in 0..depth) {
      nodes[i] = TreeMap()
    }

    nodes[0][0] = root
  }

  val isNotEmpty get() = nodes[0].isNotEmpty()

  fun add(
    depth: Int,
    parentKey: Int,
    key: Int,
    data: T
  ) {
    if (depth >= this.depth) return

    nodes[depth][parentKey]?.children?.set(key, TreeNode(id++, key, parentKey, depth + 1, data))

    val newNode = nodes[depth][parentKey]?.children?.get(key) ?: return

    nodes[depth + 1][key] = newNode
  }

  fun clear() {
    nodes.clear()
    nodes.add(TreeMap())
    depth = 1
    id = 1
  }

  fun clone(): DepthFirstTree<T> {
    val newTree = DepthFirstTree<T>()

    for (i in 0..depth) {
      for (node in nodes[i].values) {
        newTree.add(node.depth, node.parentKey, node.key, node.data!!)
      }
    }

    return newTree
  }

  override fun iterator(): Iterator<T> =
    object : Iterator<T> {
      private var currentDepth = 0
      private var currentIndex = 0

      override fun hasNext(): Boolean {
        if (currentDepth >= depth) return false

        if (currentIndex >= nodes[currentDepth].size) {
          currentDepth++

          currentIndex = 0
        }

        return currentDepth < depth
      }

      override fun next(): T {
        val node = nodes[currentDepth].values.elementAt(currentIndex)

        currentIndex++

        return node.data!!
      }
    }
}
