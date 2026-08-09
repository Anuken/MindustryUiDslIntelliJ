package mindustry.uidsl.schema

import com.google.gson.*
import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.editor.*
import mindustry.uidsl.settings.*
import java.io.*

/** Loads `data/schema.json` (bundled with the plugin) or, if configured in Settings | Tools | Mindustry UI DSL. */
@Service(Service.Level.APP)
class MsuiSchemaService {

    private val log = Logger.getInstance(MsuiSchemaService::class.java)

    @Volatile
    var schema: MsuiSchema = MsuiSchema.EMPTY
        private set

    @Volatile
    var lastLoadError: String? = null
        private set

    init {
        reload()
    }

    /** Path currently backing [schema], for display/diagnostics/file-watching purposes. */
    fun currentSchemaPath(): String? {
        val configured = MsuiSettings.instance.schemaPath.trim()
        return if(configured.isNotEmpty()) configured else null
    }

    fun reload() {
        val configured = MsuiSettings.instance.schemaPath.trim()
        schema = try {
            val text = if(configured.isNotEmpty()) {
                File(configured).readText()
            } else {
                loadBundled()
            }
            lastLoadError = null
            Gson().fromJson(text, MsuiSchema::class.java) ?: MsuiSchema.EMPTY
        } catch(e: Exception) {
            log.warn("Mindustry UI DSL: failed to load schema" + (if(configured.isNotEmpty()) " at $configured" else ""), e)
            lastLoadError = e.message ?: e.toString()
            MsuiSchema.EMPTY
        }
    }

    /** Reloads the schema and asks the editor to re-highlight open `.msui` files. */
    fun reloadAndRestartAnalysis() {
        reload()
        for(editor in EditorFactory.getInstance().allEditors) {
            editor.project?.let { project ->
                com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.getInstance(project).restart()
            }
        }
    }

    private fun loadBundled(): String {
        val stream = javaClass.getResourceAsStream("/data/schema.json")
            ?: error("bundled data/schema.json missing from plugin resources")
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    companion object {
        fun getInstance(): MsuiSchemaService = service()
    }
}
