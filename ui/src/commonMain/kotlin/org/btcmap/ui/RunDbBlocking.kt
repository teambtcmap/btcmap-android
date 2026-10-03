package org.btcmap.ui

/**
 * Runs a suspending database call and returns its result, blocking the caller.
 *
 * The shared database API is suspending so it can run on the web target, where
 * the driver is asynchronous. The Compose screens still read it synchronously;
 * this bridge keeps them unchanged until they move onto coroutines.
 */
internal expect fun <T> runDbBlocking(block: suspend () -> T): T
