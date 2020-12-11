package crux.kotlin.configuration

class ModuleConfiguratorKt {
    val opts = HashMap<String, Any>()

    fun module(module: String) = opts.put("crux/module", module)

    operator fun set(key: String, value: Any) = opts.put(key, value)
    fun set(key: String, f: () -> Any) = opts.put(key, f())
    fun set(options: Map<String, Any>) = opts.putAll(options)
    fun with(module: String, reference: String) = opts.put(module, reference)
    fun with(module: String, f: ModuleConfiguratorKt.() -> Unit) = opts.put(module, ModuleConfiguratorKt().also(f).opts)
    fun with(module: String) = with(module) {}
}