/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <https://unlicense.org>
 */

package com.bubelov.coins.db

import android.support.test.runner.AndroidJUnit4
import org.junit.runner.RunWith
import android.arch.persistence.room.testing.MigrationTestHelper
import org.junit.Rule
import android.arch.persistence.db.framework.FrameworkSQLiteOpenHelperFactory
import android.support.test.InstrumentationRegistry
import org.junit.Test

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @JvmField @Rule val migrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        "schemas/${Database::class.java.canonicalName}",
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate1To2() {
        migrationTestHelper.apply {
            createDatabase(TEST_DB, 1).apply {
                execSQL("INSERT INTO places VALUES (1, 'Italian Bakery', 0, 0, 'Cafe', '', '', 0, 0, '', '', '', 1, 0)")
                close()
            }

            runMigrationsAndValidate(TEST_DB, 2, true, Database.MIGRATION_1_2)
        }
    }

    companion object {
        const val TEST_DB = "migration-test"
    }
}