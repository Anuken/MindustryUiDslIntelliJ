package mindustry.uidsl.schema

import com.google.gson.*

/**
 * Kotlin mirror of `data/schema.json`. Node types / properties / styles are all data-driven
 * so the plugin behaves identically for a user-supplied custom schema (see [MsuiSettings]).
 */
data class MsuiSchema(
    val nodeTypes: Map<String, NodeTypeDef> = emptyMap(),
    val commonProperties: List<String> = emptyList(),
    val cellProperties: List<String> = emptyList(),
    val properties: Map<String, PropertyDef> = emptyMap(),
    val styles: Map<String, List<StyleDef>> = emptyMap(),
    // name -> 8-digit "rrggbbaa" hex, mirroring arc.graphics.Color's built-in named constants.
    val namedColors: Map<String, String> = emptyMap()
) {
    companion object {
        val EMPTY = MsuiSchema()
    }
}

data class NodeTypeDef(
    val container: Boolean = false,
    // styleType may be absent, null, a single string, or an array of strings in the JSON.
    val styleType: JsonElement? = null,
    val properties: List<String> = emptyList(),
    val cellPropsOnly: Boolean = false
) {
    /** Normalizes [styleType] to a list, regardless of whether the JSON had a string or an array. */
    fun styleTypeNames(): List<String> {
        val el = styleType ?: return emptyList()
        return when {
            el.isJsonNull -> emptyList()
            el.isJsonArray -> el.asJsonArray.mapNotNull { if(it.isJsonPrimitive) it.asString else null }
            el.isJsonPrimitive -> listOf(el.asString)
            else -> emptyList()
        }
    }
}

data class PropertyDef(
    val type: String,
    val values: List<String>? = null,
    val description: String? = null
)

data class StyleDef(
    val name: String,
    val description: String? = null
)