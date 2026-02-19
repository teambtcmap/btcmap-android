package icon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.graphics.createBitmap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import typeface.iconTypeface

private val KNOWN_ICONS = """
        account_balance      
        adult_content        
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
        design_services      
        diamond              
        directions_boat      
        directions_car       
        directions_walk      
        dns                  
        edit                 
        electric_bolt        
        electrical_services  
        emoji_food_beverage  
        engineering          
        event                
        factory              
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
        info_outline         
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
        water_pump           
        wc                   
        window               
        wine_bar
    """.trimIndent().lines().map { it.trim() }

fun init(context: Context, style: Style) {
    KNOWN_ICONS.forEach { icon ->
        val bitmap = generateIconBitmap(context, icon)
        style.addImage("marker-icon-$icon", bitmap)
    }
}

fun matcher(): List<Expression> {
    return buildList {
        KNOWN_ICONS.forEach { icon ->
            add(Expression.literal(icon))
            add(Expression.literal("marker-icon-$icon"))
        }
        add(Expression.literal("marker-icon-storefront"))
    }
}

private fun generateIconBitmap(
    context: Context,
    character: String,
    textSize: Float = context.dpToPx(24).toFloat(),
    textColor: Int = Color.WHITE
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