package org.btcmap.webspike

import androidx.sqlite.driver.web.WebWorkerSQLiteDriver

/**
 * Proves the SQLite driver the web target needs resolves on wasmJs. The type is
 * only referenced, not opened: opening it needs a Web Worker and the wasm
 * binary served by the host, which is a separate setup step.
 */
val webSqliteDriverType = WebWorkerSQLiteDriver::class
