package org.btcmap.io

import okio.FileSystem

/** The browser has no file system; the database store is the driver's. */
actual val platformFileSystem: FileSystem? = null
