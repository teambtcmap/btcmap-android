package element

import java.util.*

data class ElementCategory(
    val singular: String,
    val plural: String,
    val elements: Long,
)

fun ElementCategory.pluralDisplayString(): String {
    return when (plural) {
        "atms" -> "ATMs"
        else -> plural.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }
}