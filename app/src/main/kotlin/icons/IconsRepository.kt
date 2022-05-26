package icons

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import db.Element
import org.btcmap.R
import org.koin.core.annotation.Single

@Single
class IconsRepository(
    private val context: Context,
) {

    private val cache = mutableMapOf<Int, Bitmap>()

    fun getIcon(element: Element): Bitmap {
        val iconResId = element.iconResId() ?: R.drawable.ic_place
        var icon = cache[iconResId]

        if (icon == null) {
            icon = ContextCompat.getDrawable(context, iconResId)!!.toBitmap()
            cache[iconResId] = icon
        }

        return icon
    }
}