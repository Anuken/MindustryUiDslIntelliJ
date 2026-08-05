package mindustry.uidsl.schema

import com.intellij.openapi.vfs.*
import com.intellij.openapi.vfs.newvfs.*
import com.intellij.openapi.vfs.newvfs.events.*
import java.io.*

/**
 * Equivalent of the VSCode extension's `fs.watch(schemaPath, ...)`: when the user has
 * configured a custom schema path and that file changes on disk, reload it and re-run
 * diagnostics on open `.msui` files. Registered as an application-level [BulkFileListener]
 * via `plugin.xml` so it's active for the lifetime of the IDE.
 */
class MsuiSchemaFileListener : BulkFileListener {
    override fun after(events: MutableList<out VFileEvent>) {
        val service = MsuiSchemaService.getInstance()
        val watchedPath = service.currentSchemaPath() ?: return
        val watchedCanonical = runCatching { File(watchedPath).canonicalPath }.getOrNull() ?: return

        val touched = events.any { event ->
            val path = event.path
            val canonical = runCatching { File(path).canonicalPath }.getOrNull() ?: path
            canonical == watchedCanonical
        }

        if(touched) {
            service.reloadAndRestartAnalysis()
        }
    }
}

/** Not currently used directly, kept as a documented extension point for a future
 *  "refresh now" action - VirtualFileManager refreshes are otherwise driven by the platform. */
internal fun refreshSchemaFileVfs(path: String) {
    VirtualFileManager.getInstance().refreshAndFindFileByUrl("file://$path")
}
