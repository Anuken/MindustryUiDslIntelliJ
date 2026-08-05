package mindustry.uidsl

import com.intellij.lang.*

/**
 * The `.msui` language: Mindustry's server-side UI builder DSL, parsed on the
 * Java side by `mindustry.ui.builder.UiDslParser`.
 */
object MsuiLanguage : Language("Msui") {
    private fun readResolve(): Any = MsuiLanguage
}
