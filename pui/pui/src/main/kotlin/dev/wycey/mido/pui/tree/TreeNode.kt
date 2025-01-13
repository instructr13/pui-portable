package dev.wycey.mido.pui.tree

import java.util.*

data class TreeNode<T>
  @JvmOverloads
  constructor(
    internal val id: Int = -1,
    internal val key: Int = -1,
    internal val parentKey: Int = -1,
    val depth: Int = -1,
    val data: T? = null,
    val children: TreeMap<Int, TreeNode<T>> = TreeMap()
  )
