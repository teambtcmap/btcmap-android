package map

import areas.Area
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import org.osmdroid.util.BoundingBox

fun Area.name(): String {
    return tags["name"]!!.jsonPrimitive.content
}

fun Area.toBoundingBox(): BoundingBox {
    return BoundingBox(
        tags["box:north"]!!.jsonPrimitive.double,
        tags["box:east"]!!.jsonPrimitive.double,
        tags["box:south"]!!.jsonPrimitive.double,
        tags["box:west"]!!.jsonPrimitive.double,
    )
}