package org.btcmap.ui

import kotlinx.coroutines.runBlocking

internal actual fun <T> runDbBlocking(block: suspend () -> T): T = runBlocking { block() }
