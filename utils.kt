val Any.name: String
    get() = with(this) { javaClass.simpleName }

fun log(current: Any, content: Any) {
    println("${current.name} - $content")
}

suspend fun onMain(block: () -> Unit) {
    withContext(Dispatchers.Main) {
        block()
    }
}