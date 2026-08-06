# Mindustry UI DSL — IntelliJ Plugin

Language support for Mindustry's server-side UI builder DSL (parsed by `UiDslParser` / built by `UiTreeBuilder`), for files with the `.msui` extension.

[Read the docs on the server UI builder system](https://mindustrygame.github.io/wiki/serverUiBuilder/) for details.

**This extension is AI-generated.**[^1]

# Installing

Download the latest JAR file from the [releases](https://github.com/Anuken/MindustryUiDslIntelliJ/releases/latest) tab. In IntelliJ, open Settings -> Plugins, click hear icon -> "Install From Disk..." -> select the downloaded JAR file.

# Using

Open a `.msui` file in IntelliJ. The, launch Mindustry, and in the Mindustry console (enabled in Developer options, opened with F8), type:

`UiHotReload.show()`

This will open a file chooser window. Select your `.msui` file. The dialog will now automatically display the layout you're working on, and live-reload it when the file changes.

## Features

- **Syntax highlighting** — node types, `row`/`portrait`/`landscape` keywords, property
  keys, strings, numbers, booleans, comments (`//`), braces, colon. Because a bare word
  token is ambiguous out of context (a node type, a boolean, a style name, or a plain
  identifier all lex the same way), coloring for those is applied semantically by
  [`MsuiAnnotator`](src/main/kotlin/com/anuke/mindustry/uidsl/annotator/MsuiAnnotator.kt)
  after a real parse, the same way the original TextMate grammar used regex lookarounds.
- **Error/warning diagnostics** — unknown node types/properties, values of the wrong type,
  unknown enum/style values, malformed conditions, unterminated strings, unbalanced braces.
- **Autocomplete** — node types and `row`, and properties valid for the node type enclosing
  the caret; for a property, completes its value (booleans, enum values, style names for
  that node's style type, or condition snippets).
- **Hover docs** (`Ctrl+Q` / Quick Documentation) for node types and properties.
- **Schema-driven** — `data/schema.json` (bundled, same file as the VSCode extension) defines
  node types, properties, and styles. A custom override path can be set under
  **Settings | Tools | Mindustry UI DSL**; the file is watched and hot-reloaded, and open
  `.msui` files are immediately re-validated.
- Brace matching, line-comment toggling (`Cmd/Ctrl+/`), and auto-closing `"`/`{`.
- A **Color Settings Page** (Settings | Editor | Color Scheme | Mindustry UI DSL) so users
  can customize the token colors.

## Building & running

Requires JDK 17+.

```bash
gradle runIde       # launches a sandbox IDE with the plugin installed
gradle buildPlugin  # produces build/distributions/mindustry-ui-dsl-idea-*.zip
```

Open [`samples/example.msui`](samples/example.msui) to try it out.

## Customizing the schema

Edit `src/main/resources/data/schema.json` and rebuild, or point
**Settings | Tools | Mindustry UI DSL | Custom schema.json path** at your own copy to
override node types/properties/styles without rebuilding the plugin.

---

[^1]: If you have concerns about ethics, keep in mind: This was generated on the free tier; I didn't pay for it, nor am I supporting the AI industry in any way. If you have concerns about quality... it's a very simple plugin with useful diagnostics, and the alternative is not having a plugin at all. If you still find this objectionable, don't use it.
