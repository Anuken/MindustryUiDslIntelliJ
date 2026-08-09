package mindustry.uidsl

import com.intellij.lang.*

object MsuiLanguage : Language("Msui") {
    private fun readResolve(): Any = MsuiLanguage
}
