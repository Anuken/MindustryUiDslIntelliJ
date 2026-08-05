package mindustry.uidsl.settings

import com.intellij.openapi.components.*

/**
 * Equivalent of the VSCode extension's `msui.schemaPath` setting: an optional absolute path
 * to a custom `schema.json` overriding the bundled one (node types / properties / styles).
 */
@State(name = "MsuiSettings", storages = [Storage("msui.xml")])
@Service(Service.Level.APP)
class MsuiSettings : PersistentStateComponent<MsuiSettings.State> {

    data class State(var schemaPath: String = "")

    private var myState = State()

    override fun getState(): State = myState
    override fun loadState(state: State) {
        myState = state
    }

    var schemaPath: String
        get() = myState.schemaPath
        set(value) {
            myState.schemaPath = value
        }

    companion object {
        val instance: MsuiSettings
            get() = service()
    }
}
