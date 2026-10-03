package org.btcmap.io

import okio.FileSystem

/**
 * The platform file system, or null where there is none (the browser).
 *
 * The shared database and legacy-cleanup code use it only to inspect and remove
 * files the app may have created. In the browser the store is managed by the
 * SQLite web driver, so there is nothing to inspect and the actual is null.
 */
expect val platformFileSystem: FileSystem?
