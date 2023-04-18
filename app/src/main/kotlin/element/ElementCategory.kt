package element

import android.content.res.Resources
import org.btcmap.R

data class ElementCategory(
    val id: String,
    val elements: Long,
)

fun ElementCategory.pluralDisplayString(res: Resources): String {
    return when (id) {
        "atm" -> res.getString(R.string.category_atm_plural)
        "bar" -> res.getString(R.string.category_bar_plural)
        "cafe" -> res.getString(R.string.category_cafe_plural)
        "hotel" -> res.getString(R.string.category_hotel_plural)
        "other" -> res.getString(R.string.category_other_plural)
        "pub" -> res.getString(R.string.category_pub_plural)
        "restaurant" -> res.getString(R.string.category_restaurant_plural)
        else -> id
    }
}