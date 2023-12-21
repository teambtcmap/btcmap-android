//package element
//
//import db.inMemoryDatabase
//import kotlinx.coroutines.runBlocking
//import kotlinx.serialization.decodeFromString
//import kotlinx.serialization.json.Json
//import kotlinx.serialization.json.JsonObject
//import kotlinx.serialization.json.JsonPrimitive
//import org.junit.Assert.assertEquals
//import org.junit.Before
//import org.junit.Test
//import org.osmdroid.util.BoundingBox
//import org.osmdroid.util.GeoPoint
//import java.lang.Double.max
//import java.lang.Double.min
//import java.time.ZoneOffset
//import java.time.ZonedDateTime
//import kotlin.random.Random
//
//class ElementQueriesTest {
//
//    private lateinit var queries: ElementQueries
//
//    @Before
//    fun beforeEach() {
//        queries = ElementQueries(inMemoryDatabase())
//    }
//
//    @Test
//    fun insertOrReplace() = runBlocking {
//        val row = testElement()
//        queries.insertOrReplace(listOf(row))
//        assertEquals(row, queries.selectById(row.id))
//    }
//
//    @Test
//    fun selectById() = runBlocking {
//        val rows = (0..Random.nextInt(6)).map { testElement() }
//        queries.insertOrReplace(rows)
//        val randomRow = rows.random()
//        assertEquals(randomRow, queries.selectById(randomRow.id))
//    }
//
//    @Test
//    fun selectBySearchString() = runBlocking {
//        val row1 = testElement().copy(
//            osmJson = Json.decodeFromString("""{ "tags": { "amenity": "cafe" } }"""),
//        )
//        val row2 = testElement().copy(
//            osmJson = Json.decodeFromString("""{ "tags": { "amenity": "bar" } }"""),
//        )
//        queries.insertOrReplace(listOf(row1, row2))
//
//        val result = queries.selectBySearchString("cafe")
//        assertEquals(row1, result.singleOrNull())
//    }
//
//    @Test
//    fun selectByBoundingBox() = runBlocking {
//        val rows = buildList { repeat(100) { add(testElement()) } }
//        queries.insertOrReplace(rows)
//        val london = GeoPoint(51.509865, -0.118092)
//        val phuket = GeoPoint(7.878978, 98.398392)
//        val boundingBox = BoundingBox.fromGeoPoints(listOf(london, phuket))
//
//        val resultRows = queries.selectByBoundingBox(
//            minLat = min(boundingBox.latNorth, boundingBox.latSouth),
//            maxLat = max(boundingBox.latNorth, boundingBox.latSouth),
//            minLon = min(boundingBox.lonEast, boundingBox.lonWest),
//            maxLon = max(boundingBox.lonEast, boundingBox.lonWest),
//        )
//
//        rows.forEach { row ->
//            assert(
//                !boundingBox.contains(
//                    row.lat,
//                    row.lon,
//                ) || resultRows.any { it.id == row.id })
//        }
//    }
//
//    @Test
//    fun selectCategories() = runBlocking {
//        val elements = listOf(
//            testElement().copy(tags = JsonObject(mapOf("category" to JsonPrimitive("a")))),
//            testElement().copy(tags = JsonObject(mapOf("category" to JsonPrimitive("b")))),
//        )
//        queries.insertOrReplace(elements)
//        assertEquals(
//            listOf(
//                ElementCategory("a", 1),
//                ElementCategory("b", 1),
//            ), queries.selectCategories()
//        )
//
//        var element = testElement().copy(tags = JsonObject(mapOf("category" to JsonPrimitive("a"))))
//        queries.insertOrReplace(listOf(element))
//        assertEquals(listOf(
//            ElementCategory("a", 2),
//            ElementCategory("b", 1),
//        ), queries.selectCategories())
//
//        element = testElement().copy(tags = JsonObject(mapOf("category" to JsonPrimitive("c"))))
//        queries.insertOrReplace(listOf(element))
//        assertEquals(listOf(
//            ElementCategory("a", 2),
//            ElementCategory("b", 1),
//            ElementCategory("c", 1),
//        ), queries.selectCategories())
//    }
//}
//
//fun testElement(): Element {
//    return Element(
//        id = "${arrayOf("node", "way", "relation").random()}:${Random.nextLong()}",
//        lat = Random.nextDouble(-90.0, 90.0),
//        lon = Random.nextDouble(-180.0, 180.0),
//        osmJson = JsonObject(mapOf("tags" to JsonObject(emptyMap()))),
//        tags = JsonObject(mapOf("icon:android" to JsonPrimitive(""))),
//        createdAt = ZonedDateTime.now(ZoneOffset.UTC)
//            .minusMinutes(Random.nextLong(60 * 24 * 30)),
//        updatedAt = ZonedDateTime.now(ZoneOffset.UTC)
//            .minusMinutes(Random.nextLong(60 * 24 * 30)),
//        deletedAt = null,
//    )
//}