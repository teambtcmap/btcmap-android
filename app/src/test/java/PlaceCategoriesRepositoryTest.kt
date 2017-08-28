import com.bubelov.coins.model.PlaceCategory
import com.bubelov.coins.repository.placecategory.PlaceCategoriesDataSourceDb
import com.bubelov.coins.repository.placecategory.PlaceCategoriesDataSourceMemory
import com.bubelov.coins.repository.placecategory.PlaceCategoriesRepository

import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.io.IOException
import java.util.Random
import javax.inject.Inject

/**
 * @author Igor Bubelov
 */

class PlaceCategoriesRepositoryTest : BaseRobolectricTest() {
    @Inject lateinit var repository: PlaceCategoriesRepository
    @Inject lateinit var dbSource: PlaceCategoriesDataSourceDb
    @Inject lateinit var memorySource: PlaceCategoriesDataSourceMemory

    @Before
    fun init() {
        TestInjector.testComponent.inject(this)
    }

    @Test
    fun memorySource_addsCategory() {
        val category = generateRandomCategory()
        Assert.assertNull(memorySource.getPlaceCategory(category.id))
        memorySource.addPlaceCategory(category)
        Assert.assertEquals(memorySource.getPlaceCategory(category.id), category)
    }

    @Test
    fun dbSource_addsCategory() {
        val category = generateRandomCategory()
        Assert.assertNull(dbSource.getPlaceCategory(category.id))
        dbSource.addPlaceCategory(category)
        Assert.assertEquals(dbSource.getPlaceCategory(category.id), category)
    }

    @Test
    @Throws(IOException::class)
    fun networkSource_loadsCategories() {
        repository.reloadFromApi()
        Assert.assertNotNull(repository.getPlaceCategory(TEST_CATEGORY_ID))
    }

    @Test
    @Throws(IOException::class)
    fun cachesCategory() {
        repository.reloadFromApi()
        Assert.assertNotNull(repository.getPlaceCategory(TEST_CATEGORY_ID))
        Assert.assertNotNull(dbSource.getPlaceCategory(TEST_CATEGORY_ID))
        Assert.assertNotNull(memorySource.getPlaceCategory(TEST_CATEGORY_ID))
    }

    private fun generateRandomCategory(): PlaceCategory {
        val random = Random(System.currentTimeMillis())

        return PlaceCategory(
                id = random.nextLong(),
                name = random.nextLong().toString()
        )
    }

    companion object {
        private val TEST_CATEGORY_ID: Long = 1
    }
}