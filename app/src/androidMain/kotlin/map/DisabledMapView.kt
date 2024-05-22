package map

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import org.osmdroid.views.MapView

class DisabledMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : MapView(context, attrs) {

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        return false
    }
}