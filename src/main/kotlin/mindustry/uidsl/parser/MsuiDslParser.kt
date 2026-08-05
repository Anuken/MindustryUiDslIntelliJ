package mindustry.uidsl.parser

import mindustry.uidsl.color.*
import mindustry.uidsl.parser.MsuiDslParser.Diagnostic
import mindustry.uidsl.schema.*

/**
 * A structural/diagnostic parser for the `.msui` DSL, ported from the VSCode extension's
 * `src/parser.js`. It mirrors the grammar rules of `mindustry.ui.builder.UiDslParser` closely
 * enough to:
 *  - detect syntax errors (with source offsets, for [MsuiAnnotator]),
 *  - know, at any offset, which node type currently encloses the cursor (for completion), and
 *  - know which key/value is being written (for completion).
 *
 * Unlike the real Java parser, this one never throws - it collects [Diagnostic]s and keeps
 * parsing so the IDE can report every problem in one pass, and recovers from malformed input
 * enough to keep offering useful completions while the user is mid-edit.
 */
object MsuiDslParser {

    enum class TokType { WORD, STRING, COLON, LBRACE, RBRACE, EOF }

    data class Tok(
        val type: TokType,
        val start: Int,
        val end: Int,
        val value: String,
        val terminated: Boolean = true
    )

    enum class EntryKind { ROW, PROP, CHILD }

    class Entry(
        val key: String,
        val keyToken: Tok,
        val kind: EntryKind,
        val child: Node? = null,
        val valueToken: Tok? = null,
        val colonToken: Tok? = null
    )

    class Node(val type: String, val keyToken: Tok) {
        var start: Int = keyToken.start
        var end: Int = keyToken.end
        val entries: MutableList<Entry> = mutableListOf()
        var parent: Node? = null
        var isRoot: Boolean = false
        var shorthandValue: Tok? = null
    }

    enum class Severity { ERROR, WARNING }

    data class Diagnostic(val start: Int, val end: Int, val message: String, val severity: Severity)

    class ParseResult(val root: Node, val diagnostics: List<Diagnostic>, val tokens: List<Tok>)

    // ---------------------------------------------------------------------
    // Tokenizer
    // ---------------------------------------------------------------------

    fun tokenize(text: String): List<Tok> {
        val tokens = ArrayList<Tok>()
        var i = 0
        val n = text.length

        fun isWs(c: Char) = c == ' ' || c == '\t' || c == '\r' || c == '\n'

        while(i < n) {
            val c = text[i]

            if(isWs(c)) {
                i++; continue
            }

            if(c == '/' && i + 1 < n && text[i + 1] == '/') {
                while(i < n && text[i] != '\n') i++
                continue
            }

            when(c) {
                '{' -> {
                    tokens.add(Tok(TokType.LBRACE, i, i + 1, "{")); i++; continue
                }

                '}' -> {
                    tokens.add(Tok(TokType.RBRACE, i, i + 1, "}")); i++; continue
                }

                ':' -> {
                    tokens.add(Tok(TokType.COLON, i, i + 1, ":")); i++; continue
                }
            }

            if(c == '"') {
                val start = i
                i++
                val sb = StringBuilder()
                var terminated = false
                while(i < n) {
                    if(text[i] == '"') {
                        terminated = true; i++; break
                    }
                    if(text[i] == '\\' && i + 1 < n) {
                        sb.append(text[i + 1]); i += 2; continue
                    }
                    sb.append(text[i]); i++
                }
                tokens.add(Tok(TokType.STRING, start, i, sb.toString(), terminated))
                continue
            }

            // bare word/number token
            val start = i
            while(i < n) {
                val ch = text[i]
                if(isWs(ch) || ch == '{' || ch == '}' || ch == ':' || ch == '"') break
                i++
            }
            if(i == start) {
                i++; continue
            } // stray char safety net
            tokens.add(Tok(TokType.WORD, start, i, text.substring(start, i)))
        }

        tokens.add(Tok(TokType.EOF, n, n, ""))
        return tokens
    }

    // ---------------------------------------------------------------------
    // Parser
    // ---------------------------------------------------------------------

    fun parse(text: String, schema: MsuiSchema): ParseResult {
        val tokens = tokenize(text)
        var pos = 0
        val diagnostics = ArrayList<Diagnostic>()

        val nodeTypeNames = schema.nodeTypes.keys
        val allPropertyNames = schema.properties.keys

        fun peek() = tokens[pos]
        fun advance() = tokens[pos++]
        fun atEnd() = peek().type == TokType.EOF

        fun err(tok: Tok, message: String) {
            diagnostics.add(Diagnostic(tok.start, maxOf(tok.end, tok.start + 1), message, Severity.ERROR))
        }

        fun warn(tok: Tok, message: String) {
            diagnostics.add(Diagnostic(tok.start, maxOf(tok.end, tok.start + 1), message, Severity.WARNING))
        }

        fun validatePropUsage(containerNode: Node, key: String, keyTok: Tok, valueTok: Tok) {
            val allowed = allowedPropertiesFor(containerNode.type, schema)
            if(key !in allowed) {
                warn(keyTok, "Property '$key' is not valid on '${containerNode.type}' (or its cell). It will be ignored.")
            }

            val propDef = schema.properties[key] ?: return

            when(propDef.type) {
                "boolean" -> {
                    if(valueTok.type == TokType.WORD && valueTok.value != "true" && valueTok.value != "false") {
                        warn(valueTok, "Expected 'true' or 'false' for '$key', found '${valueTok.value}'.")
                    }
                }

                "number" -> {
                    if(valueTok.type == TokType.STRING || valueTok.value.toFloatOrNull() == null) {
                        warn(valueTok, "Expected a number for '$key', found '${valueTok.value}'.")
                    }
                }

                "enum" -> {
                    if(propDef.values != null && valueTok.value !in propDef.values) {
                        warn(valueTok, "Unknown value '${valueTok.value}' for '$key'. Expected one of: ${propDef.values.joinToString(", ")}.")
                    }
                }

                "style" -> {
                    val nodeDef = schema.nodeTypes[containerNode.type]
                    val styleTypes = nodeDef?.styleTypeNames().orEmpty()
                    if(styleTypes.isNotEmpty()) {
                        val validNames = styleNamesFor(styleTypes, schema)
                        if(validNames.isNotEmpty() && valueTok.value !in validNames) {
                            warn(valueTok, "Unknown style '${valueTok.value}' for '${containerNode.type}'. Expected one of: ${validNames.joinToString(", ")}.")
                        }
                    }
                }

                "condition" -> {
                    val cond = valueTok.value.trim()
                    val isKeyword = cond == "portrait" || cond == "landscape"
                    val isComparison = Regex("""^(width|height)\s*(>=|<=|>|<)\s*-?\d+(\.\d+)?$""").matches(cond)
                    if(!isKeyword && !isComparison && valueTok.type == TokType.WORD) {
                        warn(valueTok, "Condition '$cond' does not look like 'portrait', 'landscape', or 'width|height >=|>|<|<= number'.")
                    }
                }

                "color" -> {
                    val resolved = parseMsuiColor(valueTok.value) != null || valueTok.value.lowercase() in schema.namedColors
                    if(!resolved) {
                        warn(valueTok, "Unknown color '${valueTok.value}'. Expected a hex value (e.g. \"ffffffff\") or a built-in color name.")
                    }
                }
            }
        }

        fun makeNode(type: String, keyToken: Tok): Node = Node(type, keyToken)

        // parseStatementsInto and parseStatement are mutually recursive. Kotlin local `fun`s
        // can't forward-reference each other, so they're declared as lateinit lambdas instead:
        // both are assigned before either is ever invoked (parse() only calls parseStatementsInto
        // at the very end, after this whole block has run), and each just closes over the other's
        // var, resolved at call time.
        lateinit var parseStatementsInto: (Node) -> Unit
        lateinit var parseStatement: (Node) -> Unit

        parseStatementsInto = { node ->
            while(!atEnd() && peek().type != TokType.RBRACE) {
                val before = pos
                parseStatement(node)
                if(pos == before) advance() // safety net against infinite loops on malformed input
            }
        }

        // Labeled because it's a plain (non-inline) lambda: bare `return` inside it would try
        // to do a non-local return from the enclosing `parse()`, which isn't allowed here.
        parseStatement = ps@{ node ->
            val identTok = peek()

            if(identTok.type == TokType.STRING) {
                err(identTok, "Expected an identifier (node type or property name), found a string.")
                advance()
                return@ps
            }
            if(identTok.type != TokType.WORD) {
                if(identTok.type == TokType.RBRACE || identTok.type == TokType.EOF) return@ps
                err(identTok, "Unexpected token '${identTok.value}'.")
                advance()
                return@ps
            }
            advance()
            val ident = identTok.value

            if(ident == "row") {
                node.entries.add(Entry("row", identTok, EntryKind.ROW))
                return@ps
            }

            val isNodeType = ident in nodeTypeNames
            val isProperty = ident in allPropertyNames

            if(!isNodeType && !isProperty) {
                err(identTok, "Unknown identifier '$ident'. Not a recognized node type or property.")
                // still try to continue parsing whatever follows to keep diagnostics useful
            }

            val next = peek()

            if(next.type == TokType.COLON) {
                val colonTok = advance()
                val valueTok = peek()

                if(valueTok.type != TokType.STRING && valueTok.type != TokType.WORD) {
                    err(valueTok, "Expected a value after ':'.")
                    return@ps
                }
                advance()
                if(valueTok.type == TokType.STRING && !valueTok.terminated) {
                    err(valueTok, "Unterminated string literal.")
                }

                if(isNodeType) {
                    val child = makeNode(ident, identTok)
                    child.shorthandValue = valueTok
                    child.parent = node
                    // optional block after shorthand value
                    if(peek().type == TokType.LBRACE) {
                        advance()
                        parseStatementsInto(child)
                        if(peek().type == TokType.RBRACE) {
                            child.end = advance().end
                        } else {
                            err(peek(), "Expected '}' to close block.")
                        }
                    } else {
                        child.end = valueTok.end
                    }
                    node.entries.add(Entry(ident, identTok, EntryKind.CHILD, child = child, colonToken = colonTok))
                } else {
                    node.entries.add(Entry(ident, identTok, EntryKind.PROP, valueToken = valueTok, colonToken = colonTok))
                    if(isProperty) validatePropUsage(node, ident, identTok, valueTok)
                }
                return@ps
            }

            if(next.type == TokType.LBRACE) {
                advance()
                if(!isNodeType) {
                    err(identTok, "'$ident' is not a node type and cannot start a '{' block.")
                    // still parse & discard the block so we recover
                    val dummy = makeNode(ident, identTok)
                    parseStatementsInto(dummy)
                    if(peek().type == TokType.RBRACE) advance()
                    return@ps
                }
                val child = makeNode(ident, identTok)
                child.parent = node
                parseStatementsInto(child)
                if(peek().type == TokType.RBRACE) {
                    child.end = advance().end
                } else {
                    err(peek(), "Expected '}' to close block.")
                }
                node.entries.add(Entry(ident, identTok, EntryKind.CHILD, child = child))
                return@ps
            }

            // bare identifier, no ':' or '{' following
            if(!isNodeType) {
                if(isProperty) {
                    err(identTok, "Property '$ident' requires a value, e.g. '$ident: ...'.")
                }
                return@ps
            }
            val child = makeNode(ident, identTok)
            child.end = identTok.end
            child.parent = node
            node.entries.add(Entry(ident, identTok, EntryKind.CHILD, child = child))
        }

        val rootKeyTok = Tok(TokType.WORD, 0, 0, "table")
        val root = makeNode("table", rootKeyTok)
        root.isRoot = true
        parseStatementsInto(root)
        root.end = text.length

        if(!atEnd()) {
            err(peek(), "Unexpected '${peek().value}'.")
        }

        return ParseResult(root, diagnostics, tokens)
    }

    // ---------------------------------------------------------------------
    // Schema-driven helpers, shared with completion/documentation
    // ---------------------------------------------------------------------

    fun styleNamesFor(styleTypes: List<String>, schema: MsuiSchema): List<String> =
        styleTypes.flatMap { t -> schema.styles[t].orEmpty().map { it.name } }

    fun allowedPropertiesFor(nodeType: String?, schema: MsuiSchema): Set<String> {
        val set = LinkedHashSet<String>()
        schema.cellProperties.forEach { set.add(it) }
        val def = schema.nodeTypes[nodeType] ?: return set
        if(def.cellPropsOnly) return set
        schema.commonProperties.forEach { set.add(it) }
        def.properties.forEach { set.add(it) }
        return set
    }

    /** Finds the innermost node whose block contains the given offset (used for completion context). */
    fun findEnclosingNode(root: Node, offset: Int): Node {
        var current = root
        var found = true
        while(found) {
            found = false
            for(entry in current.entries) {
                val child = entry.child
                if(entry.kind == EntryKind.CHILD && child != null && child.start <= offset && offset <= child.end) {
                    current = child
                    found = true
                    break
                }
            }
        }
        return current
    }
}
