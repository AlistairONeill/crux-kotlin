package crux.kotlin.configuration

class NodeConfiguratorKt {
    val modules = HashMap<String, Any>()

    fun with(module: String, f: ModuleConfiguratorKt.() -> Unit) = modules.put(module, ModuleConfiguratorKt().also(f).opts)
    fun with(module: String) = with(module) {}
}