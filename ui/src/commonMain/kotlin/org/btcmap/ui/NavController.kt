package org.btcmap.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember

/**
 * A minimal back stack of route keys, shared by the Android and desktop hosts so
 * that navigating between screens does not have to go through Android's
 * `FragmentManager`.
 *
 * The stack always holds at least one entry, the root. [push] opens a screen,
 * [pop] returns to the previous one, and [reset] throws the stack away when a
 * screen sends the user back to the root (a deep link, or a feed row that
 * returns to the map). A host wires the platform back gesture to [pop]: Android
 * uses `BackHandler`, the desktop has no system back.
 *
 * Routes carry no arguments of their own — the host keeps the selected place,
 * area or event in its own state beside the controller, as the desktop did with
 * its `Route` enum — so [T] is only the screen's identity.
 */
class NavController<T : Any>(root: T) {

    private val stack = mutableStateListOf(root)

    /** The screen currently on top. Reading it in a composable subscribes to changes. */
    val current: T get() = stack.last()

    /** Whether [pop] would return to another screen. */
    val canGoBack: Boolean get() = stack.size > 1

    /** The routes on the stack, root first. */
    val routes: List<T> get() = stack

    /** Opens [route] on top of the current screen. */
    fun push(route: T) {
        stack.add(route)
    }

    /**
     * Opens [route] unless it is already on top. A host that can deliver the same
     * navigation twice (a deep link re-delivered, a button double-tap) can call
     * this so the second delivery is a no-op instead of a duplicate entry.
     */
    fun pushUnique(route: T) {
        if (current != route) stack.add(route)
    }

    /**
     * Returns to the previous screen, or does nothing when the root is current.
     * Returns whether the stack changed, so a host can fall through to its own
     * back behaviour (for example closing the window on the desktop).
     */
    fun pop(): Boolean {
        if (!canGoBack) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    /**
     * Pops back to [route] when it is on the stack, dropping everything above it.
     * Does nothing when the route is not on the stack.
     */
    fun popTo(route: T) {
        val index = stack.indexOfLast { it == route }
        if (index < 0) return
        while (stack.size > index + 1) stack.removeAt(stack.lastIndex)
    }

    /**
     * Replaces the whole stack with [route] as the single root. Used when a
     * screen deep-links back to the root and no history should remain above it.
     */
    fun reset(route: T) {
        stack.clear()
        stack.add(route)
    }
}

/** Remembers a [NavController] whose root is [route] across recompositions. */
@Composable
fun <T : Any> rememberNavController(route: T): NavController<T> = remember { NavController(route) }
