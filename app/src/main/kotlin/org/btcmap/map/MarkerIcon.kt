package org.btcmap.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.DrawableCompat
import org.btcmap.R
import org.btcmap.db.table.place.Marker
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.btcmap.util.iconTypeface

private val OUTDATED_ICON_COLOR = 0xFFBDBDBD.toInt()

private val KNOWN_ICONS = """
        18_up_rating         
        account_balance
        agriculture          
        airport_shuttle      
        architecture         
        attach_money         
        attractions          
        bakery_dining        
        balance              
        beach_access         
        bedroom_baby         
        build                
        business             
        cake                 
        camping              
        car_rental           
        car_repair           
        card_giftcard        
        carpenter            
        casino               
        castle               
        celebration          
        cell_tower           
        chair                
        chalet               
        checkroom            
        child_care           
        church               
        cleaning_services    
        coffee               
        colorize             
        commute              
        computer             
        construction         
        content_cut          
        cooking              
        cottage              
        cruelty_free         
        currency_exchange    
        delete               
        dentistry            
        destruction          
        design_services      
        diamond              
        directions_boat      
        directions_car       
        directions_walk      
        dns                  
        dresser              
        edit                 
        electric_bolt        
        electrical_services  
        emoji_food_beverage  
        engineering          
        event                
        factory              
        favorite             
        fitness_center       
        flight_takeoff       
        footprint            
        games                
        gate                 
        golf_course          
        grass                
        grid_view            
        group                
        groups               
        hardware             
        hive                 
        home                 
        hotel                
        hvac                 
        icecream             
        imagesearch_roller   
        info                 
        kayaking             
        kitesurfing          
        lan                  
        liquor               
        local_atm            
        local_bar            
        local_cafe           
        local_car_wash       
        local_florist        
        local_gas_station    
        local_grocery_store  
        local_hospital       
        local_laundry_service
        local_mall           
        local_movies         
        local_parking        
        local_pharmacy       
        local_pizza          
        local_police         
        local_post_office    
        local_printshop      
        local_taxi           
        lock                 
        luggage              
        lunch_dining         
        mail                 
        medical_services     
        menu_book            
        mic                  
        minor_crash          
        museum               
        music_note           
        nature_people        
        newspaper            
        nightlife            
        outdoor_grill        
        palette              
        panorama             
        paragliding          
        park                 
        pedal_bike           
        pets                 
        photo_camera         
        piano                
        plumbing             
        pool                 
        potted_plant         
        public               
        question_mark        
        radar                
        raven                
        restaurant           
        roofing              
        sailing              
        sauna                
        school               
        science              
        scuba_diving         
        shopping_cart        
        smartphone           
        smoking_rooms        
        spa                  
        sports               
        sports_bar           
        sports_handball      
        sports_hockey        
        sports_martial_arts  
        sports_score         
        sports_soccer        
        stadium              
        storefront           
        surfing              
        surgical             
        tapas                
        tour                 
        toys                 
        translate            
        trip_origin          
        two_wheeler          
        vaping_rooms         
        videocam             
        videogame_asset      
        visibility           
        volunteer_activism   
        warehouse            
        watch                
        water_drop           
        water_pump           
        wc                   
        window               
        wine_bar
    """.trimIndent().lines().map { it.trim() }

fun init(context: Context, style: Style) {
    KNOWN_ICONS.forEach { icon ->
        val white = generateIconBitmap(context, icon, textColor = Color.WHITE)
        style.addImage("marker-icon-$icon", white)
        val outdated = generateIconBitmap(context, icon, textColor = OUTDATED_ICON_COLOR)
        style.addImage("marker-icon-$icon-outdated", outdated)
    }
}

fun ensureMerchantMarkerImages(
    context: Context,
    style: Style,
    markers: Set<Marker>,
    markerBackgroundColor: Int,
    boostedMarkerBackgroundColor: Int,
    markerBadgeBackgroundColor: Int,
    markerBadgeTextColor: Int,
) {
    val normalPin = tintedPin(context, markerBackgroundColor)
    val boostedPin = tintedPin(context, boostedMarkerBackgroundColor)

    markers.forEach { marker ->
        val name = marker.markerImageName()
        if (style.getImage(name) != null) return@forEach

        val outdated = marker.isOutdated()
        val boosted = marker.isBoosted()
        val pin = if (boosted && !outdated) boostedPin else normalPin
        val glyph = if (marker.icon in KNOWN_ICONS) marker.icon else "storefront"
        val textColor = if (outdated) OUTDATED_ICON_COLOR else Color.WHITE
        val comments = marker.comments.takeIf { it > 0 }

        val bitmap = compositeMarker(
            context = context,
            pin = pin,
            character = glyph,
            textColor = textColor,
            comments = comments,
            badgeBackgroundColor = markerBadgeBackgroundColor,
            badgeTextColor = markerBadgeTextColor,
        )
        style.addImage(name, bitmap)
        merchantMarkerMasks[name] = AlphaMask.from(bitmap)
    }
}

private val merchantMarkerMasks = mutableMapOf<String, AlphaMask>()

fun clearMerchantMarkerMasks() {
    merchantMarkerMasks.clear()
}

fun merchantMarkerMask(name: String): AlphaMask? = merchantMarkerMasks[name]

class AlphaMask(
    val width: Int,
    val height: Int,
    private val bits: LongArray,
) {
    fun isOpaque(x: Int, y: Int): Boolean {
        if (x < 0 || y < 0 || x >= width || y >= height) return false
        val index = y * width + x
        return bits[index ushr 6] and (1L shl (index and 63)) != 0L
    }

    companion object {
        fun from(bitmap: Bitmap): AlphaMask {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val bits = LongArray((pixels.size + 63) / 64)
            pixels.forEachIndexed { index, pixel ->
                val alpha = pixel ushr 24 and 0xFF
                if (alpha >= ALPHA_THRESHOLD) {
                    bits[index ushr 6] = bits[index ushr 6] or (1L shl (index and 63))
                }
            }
            return AlphaMask(width, height, bits)
        }
    }
}

private const val ALPHA_THRESHOLD = 40

fun matcher(suffix: String = ""): List<Expression> {
    return buildList {
        KNOWN_ICONS.forEach { icon ->
            add(Expression.literal(icon))
            add(Expression.literal("marker-icon-$icon$suffix"))
        }
        add(Expression.literal("marker-icon-storefront$suffix"))
    }
}

private fun tintedPin(context: Context, color: Int): Drawable {
    return AppCompatResources.getDrawable(context, R.drawable.map_marker)!!.mutate().apply {
        DrawableCompat.setTint(this, color)
    }
}

private fun compositeMarker(
    context: Context,
    pin: Drawable,
    character: String,
    textColor: Int,
    comments: Long?,
    badgeBackgroundColor: Int,
    badgeTextColor: Int,
): Bitmap {
    val density = context.resources.displayMetrics.density
    val width = pin.intrinsicWidth
    val pinHeight = pin.intrinsicHeight
    val topPadding = if (comments != null) (BADGE_TOP_PADDING_DP * density).toInt() else 0
    val height = pinHeight + topPadding

    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)

    pin.setBounds(0, topPadding, width, topPadding + pinHeight)
    pin.draw(canvas)

    val glyph = generateIconBitmap(context, character, textColor = textColor)
    canvas.drawBitmap(
        glyph,
        (width - glyph.width) / 2f,
        topPadding + pinHeight * PIN_GLYPH_CENTER_RATIO - glyph.height / 2f,
        null,
    )

    if (comments != null) {
        drawCommentBadge(
            context = context,
            canvas = canvas,
            width = width,
            pinHeight = pinHeight,
            topPadding = topPadding,
            comments = comments,
            backgroundColor = badgeBackgroundColor,
            textColor = badgeTextColor,
        )
    }

    return bitmap
}

private fun drawCommentBadge(
    context: Context,
    canvas: Canvas,
    width: Int,
    pinHeight: Int,
    topPadding: Int,
    comments: Long,
    backgroundColor: Int,
    textColor: Int,
) {
    val density = context.resources.displayMetrics.density
    val cx = width / 2f + BADGE_OFFSET_X_DP * density
    val cy = topPadding + pinHeight - BADGE_OFFSET_Y_DP * density

    canvas.drawCircle(
        cx,
        cy,
        BADGE_RADIUS_DP * density,
        Paint().apply {
            color = backgroundColor
            isAntiAlias = true
        },
    )

    val textPaint = Paint().apply {
        color = textColor
        textSize = BADGE_TEXT_SIZE_DP * density
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    val label = if (comments > MAX_COMMENT_BADGE) "9+" else comments.toString()
    val bounds = Rect()
    textPaint.getTextBounds(label, 0, label.length, bounds)
    canvas.drawText(label, cx, cy - bounds.exactCenterY(), textPaint)
}

private const val PIN_GLYPH_CENTER_RATIO = 0.40f
private const val BADGE_OFFSET_X_DP = 13f
private const val BADGE_OFFSET_Y_DP = 43f
private const val BADGE_RADIUS_DP = 9f
private const val BADGE_TEXT_SIZE_DP = 11f
private const val BADGE_TOP_PADDING_DP = 10f

private fun generateIconBitmap(
    context: Context,
    character: String,
    textSize: Float = context.dpToPx(24).toFloat(),
    textColor: Int = Color.WHITE,
): Bitmap {
    val paint = Paint().apply {
        color = textColor
        this.textSize = textSize
        typeface = iconTypeface
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    val bounds = Rect()
    paint.getTextBounds(character, 0, character.length, bounds)

    val padding = 4
    val width = bounds.width() + padding * 2
    val height = bounds.height() + padding * 2
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)

    val x = width / 2f
    val y = height - padding - bounds.bottom
    canvas.drawText(character, x, y.toFloat(), paint)

    return bitmap
}

private fun Context.dpToPx(dp: Int): Int {
    return (dp * resources.displayMetrics.density).toInt()
}