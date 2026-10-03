package org.btcmap.db

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.runBlocking

/** A connected in-memory database, for tests. */
internal fun testDatabase(): Database = runBlocking {
    Database(BundledSQLiteDriver(), ":memory:").apply { connect() }
}
