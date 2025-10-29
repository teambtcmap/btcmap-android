package icons

import android.graphics.Typeface
import app.App

lateinit var iconTypeface: Typeface

fun init(app: App) {
    iconTypeface = Typeface.Builder(app.assets, "material-symbols-outlined-2022-12-06.ttf")
        .setFontVariationSettings("'FILL' 1, 'wght' 400, 'GRAD' 0, 'opsz' 24")
        .build()
}