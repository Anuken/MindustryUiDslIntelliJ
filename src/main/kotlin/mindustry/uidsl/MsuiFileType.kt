package mindustry.uidsl

import com.intellij.openapi.fileTypes.*
import javax.swing.*

object MsuiFileType : LanguageFileType(MsuiLanguage) {
    override fun getName(): String = "Mindustry UI DSL"
    override fun getDescription(): String = "Mindustry server-side UI builder DSL"
    override fun getDefaultExtension(): String = "msui"
    override fun getIcon(): Icon = MsuiIcons.FILE
}
