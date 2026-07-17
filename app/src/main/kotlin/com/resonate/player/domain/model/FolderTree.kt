package com.resonate.player.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * §2.2 Folders is a real filesystem tree. Nodes are derived from the flat
 * scanned folder list; ancestor directories without direct songs still appear.
 */
@Immutable
data class FolderNode(
    val path: String,
    val name: String,
    /** Songs directly in this folder (0 for pure intermediate directories). */
    val directCount: Int,
    /** Songs in this folder and all descendants. */
    val totalCount: Int,
    /** FolderEntity id when the folder has direct songs, else null. */
    val folderId: Long?,
)

class FolderTree private constructor(
    private val childrenByParent: Map<String, List<FolderNode>>,
    private val nodesByPath: Map<String, FolderNode>,
) {
    fun roots(): ImmutableList<FolderNode> =
        (childrenByParent[ROOT] ?: emptyList()).toImmutableList()

    fun childrenOf(path: String): ImmutableList<FolderNode> =
        (childrenByParent[path] ?: emptyList()).toImmutableList()

    fun node(path: String): FolderNode? = nodesByPath[path]

    companion object {
        private const val ROOT = ""
        private const val INTERNAL_STORAGE = "/storage/emulated/0"

        fun build(folders: List<Folder>): FolderTree {
            data class Builder(
                val path: String,
                val name: String,
                var directCount: Int = 0,
                var totalCount: Int = 0,
                var folderId: Long? = null,
            )

            val builders = HashMap<String, Builder>()
            val parents = HashMap<String, String>()

            fun ensureChain(path: String): Builder {
                builders[path]?.let { return it }
                val name = path.substringAfterLast('/').ifEmpty { path }
                val builder = Builder(path, name)
                builders[path] = builder
                val parentPath = when {
                    path == INTERNAL_STORAGE -> ROOT
                    path.startsWith("/storage/") && path.count { it == '/' } == 2 -> ROOT
                    else -> path.substringBeforeLast('/', missingDelimiterValue = ROOT)
                }
                parents[path] = parentPath
                if (parentPath != ROOT && parentPath.isNotEmpty()) ensureChain(parentPath)
                return builder
            }

            for (folder in folders) {
                if (folder.path.isEmpty()) continue
                val builder = ensureChain(folder.path)
                builder.directCount += folder.songCount
                builder.folderId = folder.id
                // Propagate counts up the chain.
                var path = folder.path
                while (true) {
                    builders[path]?.let { it.totalCount += folder.songCount }
                    path = parents[path] ?: break
                    if (path == ROOT || path.isEmpty()) break
                }
            }

            val childrenByParent = HashMap<String, MutableList<FolderNode>>()
            val nodesByPath = HashMap<String, FolderNode>()
            for (builder in builders.values) {
                val node = FolderNode(
                    path = builder.path,
                    name = if (builder.path == INTERNAL_STORAGE) "Internal storage" else builder.name,
                    directCount = builder.directCount,
                    totalCount = builder.totalCount,
                    folderId = builder.folderId,
                )
                nodesByPath[builder.path] = node
                val parent = parents[builder.path] ?: ROOT
                childrenByParent.getOrPut(if (parent.isEmpty()) ROOT else parent) { mutableListOf() } += node
            }
            childrenByParent.values.forEach { list -> list.sortBy { it.name.lowercase() } }
            // Collapse chains of single-child intermediates above internal storage
            // (e.g. "/storage") so roots start at meaningful volumes.
            return FolderTree(childrenByParent, nodesByPath)
        }
    }
}
