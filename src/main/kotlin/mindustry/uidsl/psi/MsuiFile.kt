package mindustry.uidsl.psi

import com.intellij.extapi.psi.*
import com.intellij.openapi.fileTypes.*
import com.intellij.psi.*
import mindustry.uidsl.*

class MsuiFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, MsuiLanguage) {
    override fun getFileType(): FileType = MsuiFileType
    override fun toString(): String = "Mindustry UI DSL File"
}
