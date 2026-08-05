package mindustry.uidsl.settings

import com.intellij.openapi.fileChooser.*
import com.intellij.openapi.options.*
import com.intellij.openapi.ui.*
import com.intellij.ui.components.*
import com.intellij.util.ui.*
import mindustry.uidsl.schema.*
import javax.swing.*

class MsuiConfigurable : Configurable {

    private val schemaPathField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(
            "Custom Mindustry UI DSL Schema",
            "Optional absolute path to a schema.json overriding the bundled one (node types / properties / styles).",
            null,
            FileChooserDescriptorFactory.createSingleFileDescriptor("json"),
            TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        )
    }

    private var panel: JPanel? = null

    override fun getDisplayName(): String = "Mindustry UI DSL"

    override fun createComponent(): JComponent {
        val built = FormBuilder.createFormBuilder()
            .addLabeledComponent("Custom schema.json path:", schemaPathField)
            .addComponentToRightColumn(
                JBLabel("<html>Leave empty to use the bundled schema. Overrides node types, properties and styles.</html>")
            )
            .addComponentFillVertically(JPanel(), 0)
            .panel
        panel = built
        return built
    }

    override fun isModified(): Boolean =
        schemaPathField.text.trim() != MsuiSettings.instance.schemaPath.trim()

    override fun apply() {
        MsuiSettings.instance.schemaPath = schemaPathField.text.trim()
        MsuiSchemaService.getInstance().reloadAndRestartAnalysis()
    }

    override fun reset() {
        schemaPathField.text = MsuiSettings.instance.schemaPath
    }

    override fun disposeUIResources() {
        panel = null
    }
}
