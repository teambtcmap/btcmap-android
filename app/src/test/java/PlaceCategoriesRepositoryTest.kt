import com.bubelov.coins.model.PlaceCategory

import org.junit.Assert
import org.junit.Test

import java.io.IOException
import java.util.Random

/**
 * @author Igor Bubelov
 */

class PlaceCategoriesRepositoryTest : BaseRobolectricTest() {
    private val repository = lazy { dependencies.value.placeCategoriesRepository() }
    private val networkSource = lazy { dependencies.value.placeCategoriesDataSourceNetwork() }
    private val dbSource = lazy { dependencies.value.placeCategoriesDataSourceDb() }
    private val memorySource = lazy { dependencies.value.placeCategoriesDataSourceMemory() }

    @Test
    fun memorySource_addsCategory() {
        val category = generateRandomCategory()
        Assert.assertNull(memorySource.value.getPlaceCategory(category.id()))
        memorySource.value.addPlaceCategory(category)
        Assert.assertEquals(memorySource.value.getPlaceCategory(category.id()), category)
    }

    @Test
    fun dbSource_addsCategory() {
        val category = generateRandomCategory()
        Assert.assertNull(dbSource.value.getPlaceCategory(category.id()))
        dbSource.value.addPlaceCategory(category)
        Assert.assertEquals(dbSource.value.getPlaceCategory(category.id()), category)
    }

    @Test
    @Throws(IOException::class)
    fun networkSource_loadsCategories() {
        repository.value.reloadFromApi()
        Assert.assertNotNull(repository.value.getPlaceCategory(TEST_CATEGORY_ID))
    }

    @Test
    @Throws(IOException::class)
    fun repository_cachesCategory() {
        repository.value.reloadFromApi()
        Assert.assertNotNull(repository.value.getPlaceCategory(TEST_CATEGORY_ID))
        Assert.assertNotNull(dbSource.value.getPlaceCategory(TEST_CATEGORY_ID))
        Assert.assertNotNull(memorySource.value.getPlaceCategory(TEST_CATEGORY_ID))
    }

    private fun generateRandomCategory(): PlaceCategory {
        val random = Random(System.currentTimeMillis())

        return PlaceCategory.builder()
                .id(random.nextLong())
                .name(random.nextLong().toString())
                .build()
    }

    companion object {
        private val TEST_CATEGORY_ID: Long = 1
    }
}